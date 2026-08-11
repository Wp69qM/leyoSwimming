package com.leyoswimming.service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class SensitiveWordFilter {

  private final Map<Character, Node> root = new HashMap<>();

  @PostConstruct
  public void init() {
    loadDefaultWords();
  }

  public boolean containsSensitive(String text) {
    if (text == null || text.isBlank()) {
      return false;
    }
    return !filter(text).equals(text);
  }

  public String filter(String text) {
    if (text == null || text.isBlank()) {
      return text;
    }
    String lower = text.toLowerCase();
    char[] chars = lower.toCharArray();
    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (i < chars.length) {
      Node node = root.get(chars[i]);
      if (node == null) {
        sb.append(text.charAt(i));
        i++;
        continue;
      }
      int matchedLength = matchLength(node, chars, i + 1);
      if (matchedLength > 0) {
        for (int k = 0; k < matchedLength; k++) {
          sb.append('*');
        }
        i += matchedLength;
      } else {
        sb.append(text.charAt(i));
        i++;
      }
    }
    return sb.toString();
  }

  private int matchLength(Node node, char[] chars, int start) {
    int length = 1;
    int max = 1;
    int i = start;
    while (i < chars.length) {
      node = node.children.get(chars[i]);
      if (node == null) {
        break;
      }
      length++;
      if (node.end) {
        max = length;
      }
      i++;
    }
    return max;
  }

  private void loadDefaultWords() {
    String[] words = {
      "法轮功", "台独", "藏独", "疆独", "色情", "淫秽", "赌博", "吸毒", "诈骗", "傻逼", "nmsl"
    };
    for (String word : words) {
      addWord(word.toLowerCase());
    }
  }

  private void addWord(String word) {
    Node node = root.computeIfAbsent(word.charAt(0), k -> new Node());
    for (int i = 1; i < word.length(); i++) {
      node = node.children.computeIfAbsent(word.charAt(i), k -> new Node());
    }
    node.end = true;
  }

  private static class Node {
    private final Map<Character, Node> children = new HashMap<>();
    private boolean end;
  }
}
