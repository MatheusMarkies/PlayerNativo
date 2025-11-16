package com.brasens.playernativo

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

class H264Decoder(
    private val surface: Surface,
 ) : Thread() {
    private var mediaCodec: MediaCodec? = null
    private val TAG = "H264Decoder"
    private var isRunning = true
    private var isCodecStarted = false

    private val naluQueue = LinkedBlockingQueue<ByteArray>()

    private var presentationTimeUs: Long = 0

    private val NALU_TYPE_SPS = 7
    private val NALU_TYPE_PPS = 8
    private val NALU_TYPE_IDR = 5
    private val NALU_TYPE_P_FRAME = 1

    private val NALU_START_CODE = byteArrayOf(0x00, 0x00, 0x00, 0x01)

    private var naluCount = 1

    fun decode(data: ByteArray) {
        naluQueue.offer(data)
    }

    fun stopDecoder() {
        isRunning = false
        this.interrupt()
    }

    override fun run() {
        try {
            var sps: ByteArray? = null
            var pps: ByteArray? = null

            while (isRunning) {
                val data = naluQueue.take()
                if (data.isEmpty()) continue
                val naluType = data[0].toInt() and 0x1F

                if (naluType == NALU_TYPE_SPS) {
                    sps = data
                    continue
                }
                if (naluType == NALU_TYPE_PPS) {
                    pps = data
                }

                if (sps != null && pps != null && !isCodecStarted) {
                    try {
                        initMediaCodec(sps!!, pps!!)

                        feedDecoderWithConfig(sps!!)

                        feedDecoderWithConfig(pps!!)

                        isCodecStarted = true

                    } catch (e: Exception) {
                        e.printStackTrace()
                        sps = null
                        pps = null
                    }
                    continue
                }

                if (!isCodecStarted) {
                    continue
                }

                if (naluType == NALU_TYPE_IDR || naluType == NALU_TYPE_P_FRAME) {
                    val naluWithStartCode = NALU_START_CODE + data
                    feedDecoder(naluWithStartCode)
                }

                drainOutputBuffers()
            }
        } catch (e: InterruptedException) {
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            releaseCodec()
        }
    }

    private fun initMediaCodec(spsNalu: ByteArray, ppsNalu: ByteArray) {
        val mime = MediaFormat.MIMETYPE_VIDEO_AVC

        val videoWidth = 1920
        val videoHeight = 1080

        val format = MediaFormat.createVideoFormat(mime, videoWidth, videoHeight)

        mediaCodec = MediaCodec.createDecoderByType(mime).apply {
            configure(format, surface, null, 0) // Esta linha não deve mais falhar
            start()
        }
    }

    private fun feedDecoder(data: ByteArray) {
        try {
            val inputBufferIndex = mediaCodec?.dequeueInputBuffer(20000) // 20ms timeout
            if (inputBufferIndex != null && inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec?.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(data)

                mediaCodec?.queueInputBuffer(inputBufferIndex, 0, data.size, presentationTimeUs, 0)
                presentationTimeUs += 11111//33333

            } else {
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drainOutputBuffers() {
        try {
            val bufferInfo = MediaCodec.BufferInfo()
            while (true) {
                val outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) // 10ms timeout

                if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {

                    break
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                } else if (outputBufferIndex != null && outputBufferIndex >= 0) {


                    mediaCodec?.releaseOutputBuffer(outputBufferIndex, true)
                } else {

                }
            }
        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    private fun feedDecoderWithConfig(data: ByteArray) {
        try {
            val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(20000) // 20ms timeout
            if (inputBufferIndex >= 0) {

                val inputBuffer = mediaCodec!!.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(data)
                mediaCodec!!.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    data.size,
                    0,
                    MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                )
            } else {

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseCodec() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
            isCodecStarted = false
        } catch (e: Exception) {

        }
    }

}