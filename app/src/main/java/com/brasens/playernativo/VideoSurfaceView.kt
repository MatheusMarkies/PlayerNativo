package com.brasens.playernativo

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView

class VideoSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle) {

    // Defina o aspect ratio do seu vídeo (1920x1080 = 16:9)
    private val VIDEO_ASPECT_RATIO = 16.0 / 9.0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        if (width == 0 || height == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val newWidth: Int
        val newHeight: Int

        val viewAspectRatio = width.toDouble() / height

        if (viewAspectRatio > VIDEO_ASPECT_RATIO) {
            // A View é mais LARGA que o vídeo (ex: 1340x600)
            // Corrija a altura para manter o aspect ratio do vídeo
            newHeight = (width / VIDEO_ASPECT_RATIO).toInt()
            newWidth = width
        } else {
            // A View é mais ALTA que o vídeo
            // Corrija a largura para manter o aspect ratio do vídeo
            newWidth = (height * VIDEO_ASPECT_RATIO).toInt()
            newHeight = height
        }

        setMeasuredDimension(newWidth, newHeight)
    }
}