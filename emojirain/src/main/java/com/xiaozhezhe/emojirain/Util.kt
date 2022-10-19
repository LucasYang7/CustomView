package com.xiaozhezhe.emojirain

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import java.util.*
import kotlin.math.abs

object Util {
    private val random = Random()

    fun setSeed(seed: Long) {
        random.setSeed(seed)
    }

    fun floatStandard() = random.nextFloat()

    fun floatAround(mean: Float, delta: Float) = floatInRange(mean - delta, mean + delta)

    fun intInRange(left: Int, right: Int) = (left..right).random()

    fun positiveGaussian() = abs(random.nextGaussian())

    private fun floatInRange(left: Float, right: Float) = left + (right - left) * random.nextFloat()

    fun dip2px(view: View, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            view.context.resources.displayMetrics
        ).toInt()
    }

    /**
     * 获取屏幕高度
     * */
    fun getWindowHeight(context: Context): Int {
        val windowManager = context.applicationContext
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        return point.y
    }
}