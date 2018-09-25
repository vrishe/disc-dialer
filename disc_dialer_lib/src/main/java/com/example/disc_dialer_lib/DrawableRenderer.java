package com.example.disc_dialer_lib;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class DrawableRenderer implements DiscDialer.Renderer {

  private final Drawable _backgroundDrawable;
  private final Drawable _discDrawable;
  private final Drawable _foregroundDrawable;

  public DrawableRenderer(@NonNull Drawable discDrawable, @Nullable Drawable backgroundDrawable,
      @Nullable Drawable foregroundDrawable) {
    _backgroundDrawable = backgroundDrawable;
    _discDrawable = discDrawable;
    _foregroundDrawable = foregroundDrawable;
  }

  @Override public void drawBackground(Canvas c, RectF bounds) {
    if (_backgroundDrawable != null)
      _backgroundDrawable.draw(c);
  }

  @Override public void drawDisc(Canvas c, RectF bounds) {
    _discDrawable.draw(c);
  }

  @Override public void drawForeground(Canvas c, RectF bounds) {
    if (_foregroundDrawable != null)
      _foregroundDrawable.draw(c);
  }

  @Override public void setClipRect(RectF clipRect) {
    int l = (int) clipRect.left;
    int t = (int) clipRect.top;
    int r = (int) clipRect.right;
    int b = (int) clipRect.bottom;

    _backgroundDrawable.setBounds(l,t,r,b);
    _discDrawable.setBounds(l,t,r,b);
    _foregroundDrawable.setBounds(l,t,r,b);
  }
}
