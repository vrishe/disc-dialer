package com.example.disc_dialer_lib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public final class DiscDialer extends View {

  private static final String TAG = DiscDialer.class.getName();

  public interface Renderer {
    void drawBackground(Canvas c, RectF bounds);

    void drawDisc(Canvas c, RectF bounds);

    void drawForeground(Canvas c, RectF bounds);
  }

  private static final double RADIAN = Math.PI / 180;
  private static final double RADIAN_INV = 180 / Math.PI;

  private static final double DIALER_ROTARY_VELOCITY = 40. / 1000; // degrees per millisecond
  private static final double DIALER_TILT = RADIAN * 22; // radians

  private final Rotor _rotor = new Rotor();
  private final Renderer _renderer = new Renderer() {
    private final Paint _paint[] = new Paint[] {
        makeTestPaint(Color.LTGRAY), makeTestPaint(Color.BLACK), makeTestPaint(Color.YELLOW)
    };

    @Override public void drawBackground(Canvas c, RectF bounds) {
      c.drawRect(bounds, _paint[0]);
    }

    @Override public void drawDisc(Canvas c, RectF bounds) {
      c.drawRect(bounds, _paint[1]);
    }

    @Override public void drawForeground(Canvas c, RectF bounds) {
      c.save();
      {
        _paint[2].setStrokeWidth(8);
        _paint[2].setStyle(Paint.Style.FILL_AND_STROKE);

        c.rotate((float) (RADIAN_INV * DIALER_TILT), bounds.centerX(), bounds.centerY());
        c.drawLine(bounds.centerX(), bounds.centerY(), bounds.right, bounds.centerY(), _paint[2]);
        c.drawCircle(bounds.right, bounds.centerY(), 16, _paint[2]);
      }
      c.restore();
    }
  };

  public DiscDialer(Context context) {
    super(context);
  }

  public DiscDialer(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public DiscDialer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public DiscDialer(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
      int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  private static double arc(PointF center, float ax, float ay, float bx, float by) {
    ax -= center.x;
    ay -= center.y;
    bx -= center.x;
    by -= center.y;

    final double s = (ax * by - ay * bx) / Math.sqrt((ax * ax + ay * ay) * (bx * bx + by * by));

    return Math.asin(s);
  }

  private static double azimuth(PointF center, float x, float y) {
    double a = Math.atan2(y - center.y, x - center.x);
    return a < 0 ? 2 * Math.PI + a : a;
  }

  private static long getAnimationTime() {
    return SystemClock.elapsedRealtime();
  }

  private static Paint makeTestPaint(int color) {
    Paint result = new Paint(Paint.ANTI_ALIAS_FLAG);
    result.setColor(color);
    result.setStyle(Paint.Style.FILL);
    return result;
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

    float cx = (right - left) * .5f, cy = (bottom - top) * .5f, d = Math.min(cx, cy);

    _clipRect.set(cx - d, cy - d, cx + d, cy + d);
    _rotor.pivot.set(_clipRect.centerX(), _clipRect.centerY());
  }

  @Override @SuppressLint("ClickableViewAccessibility")
  public boolean onTouchEvent(MotionEvent event) {
    return _rotor.onTouchEvent(event);
  }

  private class Rotor {
    final PointF pivot = new PointF();
    float angle;

    private boolean _debounce;
    private long _t0;

    void doAnimationTick() {
      if (_debounce) {
        long dt = getAnimationTime() - _t0;
        angle -= dt * DIALER_ROTARY_VELOCITY;

        if (angle < 0) {
          angle = 0;
          _debounce = false;
        }
        postInvalidate();
      }
    }

    private final PointF _touch = new PointF();
    private double _phi0, _phi1;

    boolean onTouchEvent(MotionEvent event) {
      final int action = event.getAction();
      switch (action) {
        case MotionEvent.ACTION_DOWN:
          _debounce = false;
          _touch.set(event.getX(), event.getY());
          _phi0 = azimuth(pivot, event.getX(), event.getY()) - DIALER_TILT;
          _phi1 = _phi0;
          return _clipRect.contains(event.getX(), event.getY());

        case MotionEvent.ACTION_MOVE:
          double alpha = arc(pivot, _touch.x, _touch.y, event.getX(), event.getY());
          double phi = _phi0 + alpha;

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
