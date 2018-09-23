package com.example.disc_dialer_lib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public final class DiscDialer extends View {

  private static final String TAG = DiscDialer.class.getName();

  public interface Renderer {
    void drawBackground(Canvas c, RectF bounds);

    void drawDisc(Canvas c, RectF bounds);

    void drawForeground(Canvas c, RectF bounds);

    void setClipRect(RectF clipRect);
  }

  private static final double RADIAN = Math.PI / 180;
  private static final double RADIAN_INV = 180 / Math.PI;

  private static class DialerConfig {
    private static final double DEFAULT_DISC_ROTARY_VELOCITY = 40. / 1000; // degrees per millisecond
    private static final double DEFAULT_PIN_TILT = RADIAN * 10.5; // radians

    public double discRotaryVelocity = DEFAULT_DISC_ROTARY_VELOCITY;
    public double pinTilt = DEFAULT_PIN_TILT;
    public float outerDeadZoneCoeff = 1.f;
    public float innerDeadZoneCoeff = .58103f;
    public float innerDeadZoneGripMult = .16f;
  }

  private DialerConfig _config;
  private Renderer _renderer;

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

  private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
      int defStyleRes) {
    _config = new DialerConfig();
    _renderer = new DrawableRenderer(
        ContextCompat.getDrawable(context, R.drawable.disc_dialer_bg),
        ContextCompat.getDrawable(context, R.drawable.disc_dialer_disc),
        ContextCompat.getDrawable(context, R.drawable.disc_dialer_fg));
  }

  private static double arc(PointF center, float ax, float ay, float bx, float by) {
    ax -= center.x;
    ay -= center.y;
    bx -= center.x;
    by -= center.y;

    final double s = (ax * by - ay * bx)
        / Math.sqrt((ax * ax + ay * ay) * (bx * bx + by * by));

    return Math.asin(s);
  }

  private static double azimuth(PointF center, float x, float y) {
    double a = Math.atan2(y - center.y, x - center.x);
    return a < 0 ? 2 * Math.PI + a : a;
  }

  private static long getAnimationTime() {
    return SystemClock.elapsedRealtime();
  }

  private float getSquareDistance(PointF p, float x, float y) {
    x -= p.x;
    y -= p.y;

    return x*x+y*y;
  }


  private final RectF _clipRect = new RectF();
  private final Rotor _rotor = new Rotor();

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

  private final class Rotor {
    float angle;

    private boolean _debounce;
    private long _t0;

    void doAnimationTick() {
      if (_debounce) {
        long dt = getAnimationTime() - _t0;
        angle -= dt * _config.discRotaryVelocity;

        if (angle < 0) {
          angle = 0;
          _debounce = false;
        }
        postInvalidate();
      }
    }


    private final PointF _pivot = new PointF();
    private final PointF _touch = new PointF();

    private double _phi0, _phi1;
    private float _radiusOuter, _radiusInner;
    private float _gripMult;

    void setPivot(float cx, float cy, float radius) {
      _pivot.set(cx, cy);
      _radiusOuter = radius*_config.outerDeadZoneCoeff;
      _radiusOuter *= _radiusOuter;
      _radiusInner = radius*_config.innerDeadZoneCoeff;
      _radiusInner *= _radiusInner;
    }

    boolean onTouchEvent(MotionEvent event) {
      final int action = event.getAction();
      switch (action) {
        case MotionEvent.ACTION_DOWN:
          _debounce = false;
          _touch.set(event.getX(), event.getY());
          _phi0 = azimuth(_pivot, _touch.x, _touch.y) - _config.pinTilt;
          _phi1 = _phi0;
          float r = getSquareDistance(_pivot, _touch.x, _touch.y);
          _gripMult = r < _radiusInner ?
              _config.innerDeadZoneGripMult * r / _radiusInner : 1.f;
          return _clipRect.contains(_touch.x, _touch.y)
              && _radiusOuter > r && _gripMult > 0;

        case MotionEvent.ACTION_MOVE:
          double alpha = arc(_pivot, _touch.x, _touch.y, event.getX(), event.getY());
          double phi = _phi0 + alpha * _gripMult;

          _touch.set(event.getX(), event.getY());
          _phi0 = phi;

          if (phi < 0) phi = 0;
          if (phi > 2 * Math.PI) phi = 2 * Math.PI;
          alpha = phi - _phi1;
          _phi1 = phi;

          if (alpha != 0) {
            angle += RADIAN_INV * alpha;
            if (angle < 0) angle = 0;
            if (angle > 360) angle = 360;
            invalidate();
          }
          break;

        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
          _debounce = angle > 0;
          if (_debounce) {
            _t0 = getAnimationTime();
            postInvalidate();
          }
          break;
      }
      return true;
    }
  }
}
