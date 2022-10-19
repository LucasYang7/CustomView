package com.xiaozhezhe.emojirain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.util.Pools
import androidx.core.util.Pools.SynchronizedPool
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

/**
 * 微信表情雨组件
 * */
class EmojiRainLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    companion object {
        const val EMOJI_STANDARD_SIZE_DP = 42F

        const val DEFAULT_PER = 6

        const val DEFAULT_DURATION = 8000

        const val DEFAULT_DROP_DURATION = 2400

        const val DEFAULT_DROP_FREQUENCY = 500

        const val RANDOM_SEED = 7L

        /**
         * 单个动画的横坐标平均值
         * */
        const val RANDOM_POS_X_MEAN = 0F

        /**
         * 单个动画的横坐标偏差
         * */
        const val RANDOM_POS_X_DELTA = 5F

        /**
         * 单个动画的时间长度平均值
         * */
        const val RANDOM_RELATIVE_DROP_DURATION_MEAN = 1F

        /**
         * 单个动画的时间长度偏差
         * */
        const val RANDOM_RELATIVE_DROP_DURATION_DELTA = 0.25F

        const val ELEVATION = 100F
    }

    /**
     * Emoji的尺寸大小
     * */
    private var mEmojiStandardSize: Int = 0

    /**
     * 手机屏幕高度
     * */
    private var mWindowHeight = 0

    /**
     * 每批次动画中的ImageView个数
     * */
    private var mEmojiPer = 0

    /**
     * 总的动画播放时间
     * */
    private var mDuration = 0

    /**
     * 单个图片动画播放时间
     * */
    private var mDropAverageDuration = 0

    /**
     * 刷新的时间间隔
     * */
    private var mDropFrequency = 0

    /**
     * 保存图片的类型
     * */
    private var mEmojiTypes: MutableList<Drawable>? = null

    /**
     * 缓存ImageView的对象池
     * */
    private var mEmojiPool: SynchronizedPool<ImageView>? = null

    private var mCompositeDisposable: CompositeDisposable? = null

    init {
        mCompositeDisposable = CompositeDisposable()
        mEmojiTypes = mutableListOf()

        val ta = context.obtainStyledAttributes(attrs, R.styleable.emoji_rain)
        mEmojiStandardSize = Util.dip2px(
            this,
            ta.getFloat(R.styleable.emoji_rain_img_size, EMOJI_STANDARD_SIZE_DP)
        )
        mEmojiPer = ta.getInteger(R.styleable.emoji_rain_per, DEFAULT_PER)
        mDuration = ta.getInteger(R.styleable.emoji_rain_duration, DEFAULT_DURATION)
        mDropAverageDuration =
            ta.getInteger(R.styleable.emoji_rain_drop_duration, DEFAULT_DROP_DURATION)
        mDropFrequency =
            ta.getInteger(R.styleable.emoji_rain_drop_frequency, DEFAULT_DROP_FREQUENCY)
        ta.recycle()
    }

    fun setPer(per: Int) {
        mEmojiPer = per
    }

    fun setDuration(duration: Int) {
        mDuration = duration
    }

    fun setDropDuration(dropDuration: Int) {
        mDropAverageDuration = dropDuration
    }

    fun setDropFrequency(frequency: Int) {
        mDropFrequency = frequency
    }

    fun addEmoji(emoji: Bitmap) {
        mEmojiTypes?.add(BitmapDrawable(resources, emoji))
    }

    fun addEmoji(emoji: Drawable) {
        mEmojiTypes?.add(emoji)
    }

    /**
     * 添加一种图片类型
     * */
    fun addEmoji(@DrawableRes resId: Int) {
        ContextCompat.getDrawable(context, resId)?.let { mEmojiTypes?.add(it) }
    }

    /**
     * 清空缓存的图片
     * */
    fun clearEmojis() {
        mEmojiTypes?.clear()
    }

    /**
     * 开始播放动画
     * */
    fun startDropping() {
        initEmojiPool()
        Util.setSeed(RANDOM_SEED)
        mWindowHeight = Util.getWindowHeight(context)
        // 每隔mDropFrequency时间屏幕内进入一批新的图片，每批图片个数为mEmojiPer个，
        // 一共进入mDuration / mDropFrequency批图片
        val disposable = Observable.interval(mDropFrequency.toLong(), TimeUnit.MILLISECONDS)
            .take((mDuration / mDropFrequency).toLong())
            .flatMap { flow: Long? ->
                // 一共发送mEmojiPer次事件
                Observable.range(
                    0,
                    mEmojiPer
                )
            }
            .map {
                // 每收到一次事件，就从对象池mEmojiPool中取出一个ImageView
                index: Int? -> mEmojiPool!!.acquire()
            }
            .filter { emoji: ImageView? -> emoji != null }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { emoji: ImageView? ->
                    startDropAnimationForSingleEmoji(
                        emoji!!
                    )
                }
            ) { obj: Throwable -> obj.printStackTrace() }

        mCompositeDisposable?.add(disposable)
    }

    /**
     * 停止播放动画
     * */
    fun stopDropping() {
        mCompositeDisposable?.clear()
    }

    /**
     * 播放单个ImageView的动画
     * */
    private fun startDropAnimationForSingleEmoji(emoji: ImageView) {
        val translateAnimation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0F,
            Animation.RELATIVE_TO_SELF, Util.floatAround(RANDOM_POS_X_MEAN, RANDOM_POS_X_DELTA),
            Animation.RELATIVE_TO_PARENT, 0F,
            Animation.ABSOLUTE, (mWindowHeight + 2 * mEmojiStandardSize).toFloat()
        ).apply {
            duration = (mDropAverageDuration * Util.floatAround(
                RANDOM_RELATIVE_DROP_DURATION_MEAN, RANDOM_RELATIVE_DROP_DURATION_DELTA
            )).toLong()
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {

                }

                override fun onAnimationEnd(animation: Animation?) {
                    // 动画结束后回收进入对象池
                    mEmojiPool?.release(emoji)
                }

                override fun onAnimationRepeat(animation: Animation?) {

                }

            })
        }
        emoji.startAnimation(translateAnimation)
    }

    /**
     * 初始化emoji对象池
     * */
    private fun initEmojiPool() {
        val emojiTypeCount = mEmojiTypes?.size ?: 0
        if (emojiTypeCount <= 0) {
            return
        }

        clearDirtyEmojisInPool()

        // 动画播放过程中，屏幕内部展示最多的ImageView数目
        val expectedMaxEmojiCountInScreen = (((RANDOM_RELATIVE_DROP_DURATION_MEAN
                + RANDOM_RELATIVE_DROP_DURATION_DELTA)
                * mEmojiPer
                * mDropAverageDuration)
                / (mDropFrequency.toFloat())).toInt()

        mEmojiPool = Pools.SynchronizedPool<ImageView>(expectedMaxEmojiCountInScreen)

        for (index in 0 until expectedMaxEmojiCountInScreen) {
            val emojiImageView = mEmojiTypes?.get(index % emojiTypeCount)?.let {
                generateEmojiImage(it)
            }

            emojiImageView?.let {
                val width = mEmojiStandardSize
                val height = mEmojiStandardSize

                addView(it)

                // 配置布局参数
                val constraintSet = ConstraintSet()
                constraintSet.clone(this)
                constraintSet.constrainWidth(it.id, width)
                constraintSet.constrainHeight(it.id, height)
                constraintSet.connect(
                    it.id,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP
                )
                constraintSet.connect(
                    it.id,
                    ConstraintSet.LEFT,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.LEFT,
                    0
                )
                constraintSet.connect(
                    it.id,
                    ConstraintSet.RIGHT,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.RIGHT,
                    0
                )
                // 随机初始的水平坐标位置
                constraintSet.setHorizontalBias(it.id, Util.floatStandard())
                constraintSet.applyTo(this)

                mEmojiPool?.release(it)
            }
        }
    }

    /**
     * 生成表情的ImageView
     * */
    private fun generateEmojiImage(emojiDrawable: Drawable): ImageView {
        val emojiImage = ImageView(context)
        emojiImage.setImageDrawable(emojiDrawable)
        emojiImage.setId(View.generateViewId())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            emojiImage.elevation = ELEVATION
        }
        return emojiImage
    }

    /**
     * 清除屏幕中过期的表情ImageView
     * */
    private fun clearDirtyEmojisInPool() {
        mEmojiPool?.let { pool ->
            var dirtyEmoji: ImageView?
            while ((pool.acquire().also { dirtyEmoji = it }) != null) {
                removeView(dirtyEmoji)
            }
        }
    }
}