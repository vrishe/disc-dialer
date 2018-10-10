package com.example.vladrishe.discDialer

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.example.disc_dialer_lib.DiscDialer

class MainActivity : AppCompatActivity(), DiscDialer.InputListener {

  private var _digitPreview: TextView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    _digitPreview = findViewById(R.id.text_digit_preview)
    val dialer = findViewById<DiscDialer>(R.id.disc_dialer)
    dialer.addListener(this)
  }

  override fun onDigitInput(dialer: DiscDialer, digit: Int) {
    val fadeIn = ObjectAnimator.ofFloat<View>(_digitPreview, TextView.ALPHA, 0.0f, 1.0f)
    fadeIn.duration = 160

    val fadeOut = ObjectAnimator.ofFloat<View>(_digitPreview, TextView.ALPHA, 1.0f, 0.0f)
    fadeOut.duration = 960

    val set = AnimatorSet()
    set.playSequentially(fadeIn, fadeOut)
    set.addListener(object : Animator.AnimatorListener {
      @SuppressLint("SetTextI18n")
      override fun onAnimationStart(animator: Animator) {
        _digitPreview!!.text = Integer.toString(digit)
      }

      override fun onAnimationEnd(animator: Animator) {}

      override fun onAnimationCancel(animator: Animator) {}

      override fun onAnimationRepeat(animator: Animator) {}
    })

    _digitPreview!!.tag = set
    set.start()
  }
}
