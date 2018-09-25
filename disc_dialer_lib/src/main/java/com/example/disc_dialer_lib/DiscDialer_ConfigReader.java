package com.example.disc_dialer_lib;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class DiscDialer_ConfigReader {

  public final RotorConfig config = RotorConfig.makeDefault();

  public DiscDialer_ConfigReader(Context context, int xmlResId) {
    XmlResourceParser parser = context.getResources().getXml(xmlResId);
    {
      try {
        int element = parser.next();
        while (element != XmlPullParser.END_DOCUMENT) {
          if (element == XmlPullParser.START_TAG) {
            switch (parser.getName()) {
              case "renderer":
                parseRenderer(context, parser);
                break;
              case "rotor":
              case "dead_zone":
                parseRotor(parser);
                break;
            }
          }
          element = parser.next();
        }
      } catch (XmlPullParserException | IOException ignored) {
        /* Nothing to do */
      }
    }
    parser.close();
  }

  private DiscDialer.Renderer _renderer;

  @NonNull public DiscDialer.Renderer getRenderer() {
    if (_renderer == null) {
      throw new RuntimeException(
          "Couldn't initialize renderer as it's config declaration is missing.");
    }

    return _renderer;
  }

  @NonNull private static <T> Map<String, Field> toLookUp(Class<T> cls) {
    Map<String, Field> result = new HashMap<>();
    for (Field f : cls.getFields()) {
      if (!Modifier.isStatic(f.getModifiers())) {
        String name = f.getName();
        XmlName xmlName = f.getAnnotation(XmlName.class);
        if (xmlName != null) name = xmlName.value();

        result.put(name, f);
      }
    }
    return result;
  }

  private static Object parse(String value, Class<?> cls) {
    if (boolean.class == cls) {
      return Boolean.parseBoolean(value);
    }
    if (char.class == cls) {
      if (value != null && value.length() == 1) return value.charAt(0);

      throw new IllegalArgumentException(String.format("Can't parse '%s' as %s.", value, cls));
    }
    if (short.class == cls) {
      return Short.parseShort(value, 10);
    }
    if (int.class == cls) {
      return Integer.parseInt(value, 10);
    }
    if (long.class == cls) {
      return Long.parseLong(value, 10);
    }
    if (float.class == cls) {
      return Float.parseFloat(value);
    }
    if (double.class == cls) {
      return Double.parseDouble(value);
    }
    if (String.class == cls) {
      return value;
    }
    throw new RuntimeException(String.format("parse: type %s is not supported.", cls));
  }

  private static Drawable getDrawable(Context context, String drawableId) {
    if (drawableId == null || drawableId.length() <= 1 || !drawableId.startsWith("@")) return null;
    return ContextCompat.getDrawable(context, Integer.parseInt(drawableId.substring(1)));
  }

  private void parseRotor(XmlPullParser p) {
    int count = p.getAttributeCount();
    if (count <= 0) return;

    Map<String, Field> lookup = toLookUp(RotorConfig.class);
    for (int i = 0; i < count; ++i) {
      Field f = lookup.get(p.getAttributeName(i));
      if (f != null) {
        if (!f.isAccessible()) f.setAccessible(true);

        try {
          f.set(config, parse(p.getAttributeValue(i), f.getType()));
        } catch (IllegalAccessException ignored) {
          /* Nothing to do */
        }
      }
    }
  }

  private void parseRenderer(Context context, XmlPullParser p) {
    Map<String, String> attributes = new HashMap<>();
    for (int i = p.getAttributeCount() - 1; i >= 0; --i)
      attributes.put(p.getAttributeName(i), p.getAttributeValue(i));

    String name = attributes.get("name");
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Renderer name is missing.");
    }

    if ("drawable".equalsIgnoreCase(name)
        || "drawablerenderer".equals(name)
        || DrawableRenderer.class.getCanonicalName().equals(name)) {
      Drawable disc = getDrawable(context, attributes.get("disc"));
      if (disc == null) throw new IllegalArgumentException("disc drawable ref's missing.");

      _renderer = new DrawableRenderer(disc, getDrawable(context, attributes.get("background")),
          getDrawable(context, attributes.get("foreground")));
      return;
    }

    throw new RuntimeException(String.format("parseRenderer: '%s' is not supported.", name));

    //try {
    //  Class<?> cls = Class.forName(name);
    //} catch (ClassNotFoundException e) {
    //   throw new RuntimeException(String.format("Can't instantiate %s.", name), e);
    //}
  }
}
