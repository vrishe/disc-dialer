package com.example.vladrishe.disc_dialer;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ViewPropertyAnimatorCompatSet;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.TextView;
import android.widget.ViewAnimator;
import com.example.disc_dialer_lib.DiscDialer;

public class MainActivity extends AppCompatActivity implements DiscDialer.InputListener {

  private TextView _digitPreview;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    _digitPreview = findViewById(R.id.text_digit_preview);
    DiscDialer dialer = findViewById(R.id.disc_dialer);
    dialer.addListener(this);
  }

  @Override public void onDigitInput(DiscDialer dialer, final int digit) {
    ObjectAnimator fadeIn = ObjectAnimator.ofFloat(_digitPreview, TextView.ALPHA, 0, 1);
    fadeIn.setDuration(160);

    ObjectAnimator fadeOut = ObjectAnimator.ofFloat(_digitPreview, TextView.ALPHA, 1, 0);
    fadeOut.setDuration(960);

    AnimatorSet set = new AnimatorSet();
    set.playSequentially(fadeIn, fadeOut);
    set.addListener(new Animator.AnimatorListener() {
      @SuppressLint("SetTextI18n") @Override public void onAnimationStart(Animator animator) {
        _digitPreview.setText(Integer.toString(digit));
      }

      @Override public void onAnimationEnd(Animator animator) {
      }

      @Override public void onAnimationCancel(Animator animator) {
      }

      @Override public void onAnimationRepeat(Animator animator) {
      }
    });

    _digitPreview.setTag(set);
    set.start();
  }
}
