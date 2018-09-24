package com.example.disc_dialer_lib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Collection;

public final class DiscDialer extends View implements Rotor.PulseInputReceiver {

  public interface InputListener {
    void onDigitInput(DiscDialer dialer, int digit);
  }

  public interface Renderer {
    void drawBackground(Canvas c, RectF bounds);

    void drawDisc(Canvas c, RectF bounds);

    void drawForeground(Canvas c, RectF bounds);

    void setClipRect(RectF clipRect);
  }


  public DiscDialer(Context context) {
    super(context);
    init(context, null, 0,0);
  }

  public DiscDialer(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs, 0,0);
  }

  public DiscDialer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs, defStyleAttr, 0);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public DiscDialer(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
      int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context, attrs, defStyleAttr, defStyleRes);
  }


  private final Collection<InputListener> _listeners = new ArrayList<>();

  public void addListener(InputListener listener) {
    _listeners.add(listener);
  }

  public void removeListener(InputListener listener) {
    _listeners.remove(listener);
  }

  public void receivePulseInput(int digit) {
    for (InputListener l: _listeners) {
      l.onDigitInput(this, digit);
    }
  }


  private Renderer _renderer;

  public void setRenderer(@NonNull Renderer renderer) {
    _renderer = renderer;
    invalidate();
  }

  private final Rotor _rotor = new Rotor(this, this);

  private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
      int defStyleRes) {
    _renderer = new DrawableRenderer(
        ContextCompat.getDrawable(context, R.drawable.disc_dialer_bg),
        ContextCompat.getDrawable(context, R.drawable.disc_dialer_disc),
        ContextCompat.getDrawable(context, R.drawable.disc_dialer_fg));
  }

  private final RectF _clipRect = new RectF();

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    _rotor.doAnimationTick();

    canvas.save();
    {
      canvas.clipRect(_clipRect);
      _renderer.drawBackground(canvas, _clipRect);

      canvas.save();
      {
        canvas.rotate(_rotor.angle, _clipRect.centerX(), _clipRect.centerY());
        _renderer.drawDisc(canvas, _clipRect);
      }
      canvas.restore();

      _renderer.drawForeground(canvas, _clipRect);
      canvas.translate(_clipRect.left, _clipRect.top);
    }
    canvas.restore();
  }

  @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    int w = right-left, h = bottom-top;
    float cx = .5f*w, cy = .5f*h, r = Math.min(cx, cy);

    _clipRect.set(cx - r, cy - r, cx + r, cy + r);
    _renderer.setClipRect(_clipRect);
    _rotor.setPivot(cx, cy, r);
  }

  @Override @SuppressLint("ClickableViewAccessibility")
  public boolean onTouchEvent(MotionEvent event) {
    return _rotor.onTouchEvent(event);
  }
}
