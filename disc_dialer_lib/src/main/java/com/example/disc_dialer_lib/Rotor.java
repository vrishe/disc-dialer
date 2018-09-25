package com.example.disc_dialer_lib;

import android.graphics.PointF;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;

final class Rotor {

  private static final int MAX_PULSES = 10;

  interface PulseInputReceiver {
    void receivePulseInput(int pulseCount);
  }

  private final View _view;
  private PulseInputReceiver _dispatcher;

  Rotor(@NonNull View view, @NonNull PulseInputReceiver dispatcher) {
    _view = view;
    _dispatcher = dispatcher;
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

  private static float getSquareDistance(PointF p, float x, float y) {
    x -= p.x;
    y -= p.y;

    return x * x + y * y;
  }


  private static final double RADIAN = Math.PI / 180;
  private static final double RADIAN_INV = 180 / Math.PI;
  private static final double TIME_UNIT = 1000;

  float angle;

  private RotorConfig _config = RotorConfig.makeDefault();

  @NonNull
  public RotorConfig config() {
    return _config;
  }

  private boolean _debounce;
  private long _t0;

  public void doAnimationTick() {
    if (_debounce) {
      long dt = getAnimationTime() - _t0;
      angle -= dt * _config.angularVelocity/TIME_UNIT;

      if (angle < 0) {
        angle = 0;
        _debounce = false;
      }
      discardPulsesCount();
      _view.postInvalidate();
    }
  }

  private final PointF _pivot = new PointF();
  private final PointF _touch = new PointF();

  private double _phi0, _phi1;
  private float _gripMult;
  private float _maxAngle;
  private float _radiusOuter, _radiusInner;
  private int _pulsesCount;

  public boolean onTouchEvent(MotionEvent event) {
    final int action = event.getAction();
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        _debounce = false;
        _touch.set(event.getX(), event.getY());
        _phi0 = azimuth(_pivot, _touch.x, _touch.y) - _config.fingerStopAzimuth*RADIAN;
        _phi1 = _phi0;
        _maxAngle = getMaxAngle(_phi0);
        float r = getSquareDistance(_pivot, _touch.x, _touch.y);
        _gripMult = r < _radiusInner ? _config.innerDeadZoneGripMult * r / _radiusInner : 1.f;
        return _radiusOuter >= r && _gripMult > 0;

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
          float angle_old = angle;

          angle += RADIAN_INV * alpha;
          if (angle < 0) angle = 0;
          if (angle > _maxAngle) angle = _maxAngle;

          if (alpha > 0) _pulsesCount = Math.min(getPulsesCountEstimate(), MAX_PULSES);
          if (angle != angle_old) _view.invalidate();
        }
        discardPulsesCount();
        break;

      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        _debounce = angle > 0;
        if (_debounce) {
          _t0 = getAnimationTime();
          _view.postInvalidate();
        }
        break;
    }
    return true;
  }

  public void setPivot(float cx, float cy, float radius) {
    _pivot.set(cx, cy);
    _radiusOuter = radius * _config.outerDeadZoneCoeff;
    _radiusOuter *= _radiusOuter;
    _radiusInner = radius * _config.innerDeadZoneCoeff;
    _radiusInner *= _radiusInner;
  }

  private void discardPulsesCount() {
    if (_pulsesCount > 0 && angle < _config.digitSegmentArc) {
      _dispatcher.receivePulseInput(_pulsesCount % MAX_PULSES);
      _pulsesCount = 0;
    }
  }

  private int getPulsesCountEstimate() {
    return Math.round((angle - _config.cockAngleThreshold) / _config.digitSegmentArc);
  }

  private float getMaxAngle(double phi0) {
    phi0 = RADIAN_INV * (2 * Math.PI - phi0);
    if (phi0 <= _config.cockAngleThreshold)
      return 360;

    float theta = _config.cockAngleThreshold;
    for (int i = 1; i <= MAX_PULSES; ++i) {
      if (phi0 < theta + _config.digitSegmentArc) return theta;

      theta += _config.digitSegmentArc;
    }
    return theta;
  }
}
