package com.brasens.playernativo

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView

class VideoSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle) {
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
            newHeight = (width / VIDEO_ASPECT_RATIO).toInt()
            newWidth = width
        } else {
            newWidth = (height * VIDEO_ASPECT_RATIO).toInt()
            newHeight = height
        }

        setMeasuredDimension(newWidth, newHeight)
    }
}