package com.leyoswimming.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class HtmlUtils {

  private static final Safelist DESCRIPTION_SAFE_LIST =
      Safelist.basic()
          .addTags("h1", "h2", "h3", "h4", "h5", "h6", "hr")
          .addProtocols("a", "href", "http", "https");

  private HtmlUtils() {}

  public static String sanitizeDescription(String html) {
    if (html == null) {
      return null;
    }
    return Jsoup.clean(html, DESCRIPTION_SAFE_LIST);
  }
}
