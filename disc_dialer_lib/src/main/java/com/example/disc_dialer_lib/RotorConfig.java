package com.example.disc_dialer_lib;

import android.support.annotation.NonNull;

public final class RotorConfig {

  public static final double ANGULAR_VELOCITY_DEFAULT = 40. / 1000;
  public static final double ANGULAR_VELOCITY_MIN = 1;
  public static final float DIGIT_SEGMENT_MAX = 36;

  public double angularVelocity;
  public double fingerStopAzimuth;
  public float cockAngleThreshold;
  public float digitSegmentArc;
  @XmlName("inner") public float innerDeadZoneCoeff;
  @XmlName("inner_grip") public float innerDeadZoneGripMult;
  @XmlName("outer") public float outerDeadZoneCoeff;

  @NonNull public static RotorConfig makeDefault() {
    RotorConfig result = new RotorConfig();
    result.angularVelocity = ANGULAR_VELOCITY_DEFAULT;
    result.digitSegmentArc = DIGIT_SEGMENT_MAX;
    result.outerDeadZoneCoeff = 1;
    return result;
  }

  private RotorConfig() {
  }

  RotorConfig(@NonNull RotorConfig src) {
    angularVelocity = src.angularVelocity;
    cockAngleThreshold = src.cockAngleThreshold;
    digitSegmentArc = src.digitSegmentArc;
    fingerStopAzimuth = src.fingerStopAzimuth;
    innerDeadZoneCoeff = src.innerDeadZoneCoeff;
    innerDeadZoneGripMult = src.innerDeadZoneGripMult;
    outerDeadZoneCoeff = src.outerDeadZoneCoeff;
  }

  void set(@NonNull RotorConfig src) {
    angularVelocity = Math.max(src.angularVelocity, ANGULAR_VELOCITY_MIN);
    cockAngleThreshold = Math.max(src.cockAngleThreshold, 0);
    digitSegmentArc = Math.max(Math.min(src.digitSegmentArc, DIGIT_SEGMENT_MAX), 0);
    fingerStopAzimuth = src.fingerStopAzimuth;
    innerDeadZoneCoeff = Math.max(Math.min(src.innerDeadZoneCoeff, 1), 0);
    innerDeadZoneGripMult = Math.max(Math.min(src.innerDeadZoneGripMult, 1), 0);
    outerDeadZoneCoeff = Math.max(Math.min(src.outerDeadZoneCoeff, 1), 0);
  }
}
