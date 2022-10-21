package com.xiaozhezhe.customview

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
    }

    private fun initViews() {
        tv_goto_emoji_rain_animation.setOnClickListener {
            startActivity(Intent(this, EmojiRainActivity::class.java))
        }
    }
}
