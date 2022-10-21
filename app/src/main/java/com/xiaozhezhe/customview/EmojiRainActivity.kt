package com.xiaozhezhe.customview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.xiaozhezhe.emojirain.Util
import kotlinx.android.synthetic.main.activity_emoji_rain.*

class EmojiRainActivity : AppCompatActivity() {
    companion object {
        val EMOJI_RAIN_ICONS = intArrayOf(
            R.drawable.emoji_heart,
            R.drawable.emoji_cake,
            R.drawable.emoji_apple
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emoji_rain)
        initViews()
    }

    private fun initViews() {
        tv_play_animation.setOnClickListener {
            playEmojiRain()
        }

        tv_stop_animation.setOnClickListener {
            stopEmojiRain()
        }
    }

    private fun playEmojiRain() {
        emoji_rain_layout.clearEmojis()
        emoji_rain_layout.addEmoji(
            EMOJI_RAIN_ICONS[Util.intInRange(0, EMOJI_RAIN_ICONS.size - 1)]
        )
        emoji_rain_layout.startDropping()
    }

    private fun stopEmojiRain() {
        emoji_rain_layout.stopDropping()
    }
}