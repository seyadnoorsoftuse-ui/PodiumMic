package com.seyad.podiummic

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.seyad.podiummic.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private var fontSize = 24f
    private var editMode = false
    private var autoScrolling = false

    private val scrollRunnable = object : Runnable {
        override fun run() {
            if (autoScrolling) {
                b.notesScroll.smoothScrollBy(0, 2)
                b.notesScroll.postDelayed(this, 50)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // ===== 1. KEEP SCREEN ON (திரை அணையாது) =====
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        prefs = getSharedPreferences("podium", Context.MODE_PRIVATE)
        fontSize = prefs.getFloat("fontSize", 24f)
        b.notesView.textSize = fontSize
        b.notesEdit.setText(prefs.getString("notes", getString(R.string.sample_notes)))
        b.notesView.text = b.notesEdit.text

        // Edit / View toggle
        b.btnEdit.setOnClickListener {
            editMode = !editMode
            if (editMode) {
                b.notesEdit.visibility = View.VISIBLE
                b.notesScroll.visibility = View.GONE
                b.btnEdit.text = getString(R.string.done)
            } else {
                prefs.edit().putString("notes", b.notesEdit.text.toString()).apply()
                b.notesView.text = b.notesEdit.text
                b.notesEdit.visibility = View.GONE
                b.notesScroll.visibility = View.VISIBLE
                b.btnEdit.text = getString(R.string.edit)
            }
        }

        // Font size controls
        b.btnFontUp.setOnClickListener { changeFont(+2f) }
        b.btnFontDown.setOnClickListener { changeFont(-2f) }

        // Auto-scroll (teleprompter style)
        b.btnAutoScroll.setOnClickListener {
            autoScrolling = !autoScrolling
            b.btnAutoScroll.text =
                if (autoScrolling) getString(R.string.stop_scroll) else getString(R.string.auto_scroll)
            if (autoScrolling) b.notesScroll.post(scrollRunnable)
        }

        // ===== 2. MIC -> BLUETOOTH AMPLIFIER =====
        b.btnMic.setOnClickListener {
            if (MicStreamService.isRunning) stopMic() else startMicWithPermission()
        }

        b.gainSlider.addOnChangeListener { _, value, _ ->
            MicStreamService.gain = value
            b.gainLabel.text = getString(R.string.gain_fmt, value)
        }
        b.gainSlider.value = 2.0f
    }

    private fun changeFont(delta: Float) {
        fontSize = (fontSize + delta).coerceIn(14f, 72f)
        b.notesView.textSize = fontSize
        b.notesEdit.textSize = fontSize
        prefs.edit().putFloat("fontSize", fontSize).apply()
    }

    private fun startMicWithPermission() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 31) needed += Manifest.permission.BLUETOOTH_CONNECT
        if (Build.VERSION.SDK_INT >= 33) needed += Manifest.permission.POST_NOTIFICATIONS

        val notGranted = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 100)
            return
        }
        startMic()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) startMic()
        else Toast.makeText(this, R.string.perm_needed, Toast.LENGTH_LONG).show()
    }

    private fun startMic() {
        if (!isBluetoothOutputConnected()) {
            Toast.makeText(this, R.string.connect_bt_first, Toast.LENGTH_LONG).show()
        }
        val i = Intent(this, MicStreamService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        b.btnMic.text = getString(R.string.mic_stop)
        b.micStatus.text = getString(R.string.mic_live)
    }

    private fun stopMic() {
        stopService(Intent(this, MicStreamService::class.java))
        b.btnMic.text = getString(R.string.mic_start)
        b.micStatus.text = getString(R.string.mic_off)
    }

    private fun isBluetoothOutputConnected(): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
    }

    override fun onResume() {
        super.onResume()
        if (MicStreamService.isRunning) {
            b.btnMic.text = getString(R.string.mic_stop)
            b.micStatus.text = getString(R.string.mic_live)
        }
    }
}
