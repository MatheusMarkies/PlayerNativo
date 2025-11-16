package com.brasens.playernativo

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.LinearLayout
import android.widget.TextView
import okhttp3.*
import okio.ByteString

class MainActivity : Activity() {

    private lateinit var surfaceView: SurfaceView
    private var decoder: H264Decoder? = null
    private var webSocket: WebSocket? = null

    private val WS_URL = "ws://192.168.0.71:8081"
    private val TAG = "MainActivity"

    private var binaryMessageCount = 0

    private val logHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        surfaceView = VideoSurfaceView(this).apply {
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    startDecoder(holder.surface)
                    connectWebSocket()
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    stopAll()
                }
            })
        }

        layout.addView(surfaceView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
        ))
        setContentView(layout)
    }

    private fun startDecoder(surface: Surface) {
        decoder = H264Decoder(surface)
        decoder?.start()
    }

    private fun connectWebSocket() {
        val client = OkHttpClient.Builder()
            .pingInterval(0, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        val request = Request.Builder().url(WS_URL).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val cmd1 = """{"cmd":"start_stream","param":{"mirror":1}}"""
                webSocket.send(cmd1)

                val cmd2 = """{"cmd":"stop_model","param":["yolo4t","retinaface","mobilenetface","yamnet","object_tracking","palm_detection","hand_landmark"]}"""
                webSocket.send(cmd2)

                val cmd3 = """{"cmd":"start_model","param":[]}"""
                webSocket.send(cmd3)

                val cmd4 = """{"cmd":"get_model_meta","param":["classify"]}"""
                webSocket.send(cmd4)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                binaryMessageCount++
                val data = bytes.toByteArray()

                if (data.size > 2 && data[0] == 0x00.toByte() && data[1] == 0xFF.toByte()) {
                    return
                }

                val videoData = if (data.size > 2 &&
                    data[0] == 0x00.toByte() &&
                    (data[1] == 0xFE.toByte() || data[1] == 0xFD.toByte())) {
                    data.copyOfRange(2, data.size)
                } else {
                    data
                }

                decoder?.decode(videoData)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            }
        })
    }

    private fun stopAll() {
        webSocket?.close(1000, "Activity destruída")
        webSocket = null
        decoder?.stopDecoder()
        decoder = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAll()
        logHandler.removeCallbacksAndMessages(null)
    }
}