package com.leyoswimming.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HtmlUtilsTest {

  @Test
  @DisplayName("sanitizeDescription: 保留合法富文本标签")
  void sanitizeDescription_validHtml_returnsCleanHtml() {
    assertThat(HtmlUtils.sanitizeDescription("<p>暑期特惠</p>")).isEqualTo("<p>暑期特惠</p>");
    assertThat(HtmlUtils.sanitizeDescription("<h1>标题</h1>")).isEqualTo("<h1>标题</h1>");
    assertThat(HtmlUtils.sanitizeDescription("<strong>加粗</strong>"))
        .isEqualTo("<strong>加粗</strong>");
  }

  @Test
  @DisplayName("sanitizeDescription: 过滤 script 标签")
  void sanitizeDescription_scriptTag_removesScript() {
    assertThat(HtmlUtils.sanitizeDescription("<p>正文</p><script>alert(1)</script>"))
        .isEqualTo("<p>正文</p>");
  }

  @Test
  @DisplayName("sanitizeDescription: 过滤事件处理器属性")
  void sanitizeDescription_eventHandler_removesAttribute() {
    assertThat(HtmlUtils.sanitizeDescription("<img src=x onerror=alert(1)>")).isEqualTo("");
  }

  @Test
  @DisplayName("sanitizeDescription: 过滤 javascript 伪协议链接")
  void sanitizeDescription_javascriptProtocol_removesHref() {
    assertThat(HtmlUtils.sanitizeDescription("<a href=\"javascript:alert(1)\">点击</a>"))
        .isEqualTo("<a rel=\"nofollow\">点击</a>");
  }

  @Test
  @DisplayName("sanitizeDescription: 保留 http/https 链接")
  void sanitizeDescription_httpUrl_keepsHref() {
    assertThat(HtmlUtils.sanitizeDescription("<a href=\"https://example.com\">点击</a>"))
        .isEqualTo("<a href=\"https://example.com\" rel=\"nofollow\">点击</a>");
  }

  @Test
  @DisplayName("sanitizeDescription: 过滤 style 属性")
  void sanitizeDescription_styleAttribute_removesStyle() {
    assertThat(HtmlUtils.sanitizeDescription("<p style=\"color:red\">正文</p>"))
        .isEqualTo("<p>正文</p>");
  }

  @Test
  @DisplayName("sanitizeDescription: 过滤 iframe 等危险标签")
  void sanitizeDescription_dangerousTags_removesTags() {
    assertThat(HtmlUtils.sanitizeDescription("<iframe src=\"evil.com\"></iframe>")).isEqualTo("");
  }

  @Test
  @DisplayName("sanitizeDescription: null 返回 null")
  void sanitizeDescription_null_returnsNull() {
    assertThat(HtmlUtils.sanitizeDescription(null)).isNull();
  }
}
