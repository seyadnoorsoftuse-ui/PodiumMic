package com.seyad.podiummic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.IBinder
import kotlin.concurrent.thread
import kotlin.math.max

/**
 * Phone mic-ஐ live-ஆக படித்து (AudioRecord), gain ஏற்றி,
 * AudioTrack வழியாக output-க்கு அனுப்புகிறது.
 * Bluetooth speaker/amplifier connect ஆகி இருந்தால்,
 * Android அதை default output-ஆக route செய்யும் (A2DP).
 */
class MicStreamService : Service() {

    companion object {
        @Volatile var isRunning = false
        @Volatile var gain = 2.0f          // 1.0 = normal, 4.0 = max boost
        const val SAMPLE_RATE = 44100
        const val CHANNEL_ID = "mic_stream"
    }

    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) return START_STICKY
        isRunning = true
        startForeground(1, buildNotification())
        startAudioLoop()
        return START_STICKY
    }

    private fun startAudioLoop() {
        worker = thread(name = "MicLoop") {
            val minRec = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val minPlay = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val bufSize = max(minRec, minPlay)

            val recorder: AudioRecord
            try {
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufSize * 2
                )
            } catch (e: SecurityException) {
                stopSelf(); return@thread
            }

            // Echo / noise reduction (device support இருந்தால்)
            if (AcousticEchoCanceler.isAvailable())
                AcousticEchoCanceler.create(recorder.audioSessionId)?.enabled = true
            if (NoiseSuppressor.isAvailable())
                NoiseSuppressor.create(recorder.audioSessionId)?.enabled = true

            val player = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.mode = AudioManager.MODE_NORMAL

            val buffer = ShortArray(bufSize / 2)
            recorder.startRecording()
            player.play()

            try {
                while (isRunning && !Thread.currentThread().isInterrupted) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val g = gain
                        if (g != 1.0f) {
                            for (i in 0 until read) {
                                val v = (buffer[i] * g).toInt()
                                buffer[i] = v.coerceIn(
                                    Short.MIN_VALUE.toInt(),
                                    Short.MAX_VALUE.toInt()
                                ).toShort()
                            }
                        }
                        player.write(buffer, 0, read)
                    }
                }
            } finally {
                try { recorder.stop() } catch (_: Exception) {}
                recorder.release()
                try { player.stop() } catch (_: Exception) {}
                player.release()
            }
        }
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, getString(R.string.mic_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        return builder
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.mic_live))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        isRunning = false
        worker?.interrupt()
        worker = null
        super.onDestroy()
    }
}
