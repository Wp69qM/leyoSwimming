package com.leyoswimming.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * 显式通过 {@code @TableField(typeHandler = StringListJsonTypeHandler.class)} 使用，
 * 不注册为全局 List 类型处理器，避免与 CoachCertificateListJsonTypeHandler 冲突。
 */
public class StringListJsonTypeHandler extends BaseTypeHandler<List<String>> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<String>> TYPE_REF = new TypeReference<>() {};

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
      throws SQLException {
    try {
      ps.setString(i, OBJECT_MAPPER.writeValueAsString(parameter));
    } catch (Exception e) {
      throw new SQLException("Failed to serialize List<String> to JSON", e);
    }
  }

  @Override
  public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return parse(extractJson(rs.getObject(columnName)));
  }

  @Override
  public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return parse(extractJson(rs.getObject(columnIndex)));
  }

  @Override
  public List<String> getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return parse(extractJson(cs.getObject(columnIndex)));
  }

  private String extractJson(Object value) throws SQLException {
    if (value == null) {
      return null;
    }
    if (value instanceof String s) {
      return s;
    }
    if (value instanceof byte[] bytes) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
    if (value instanceof Clob clob) {
      try {
        return clob.getSubString(1, (int) clob.length());
      } finally {
        try {
          clob.free();
        } catch (Exception ignored) {
          // ignore
        }
      }
    }
    if (value instanceof Blob blob) {
      try {
        byte[] bytes = blob.getBytes(1, (int) blob.length());
        return new String(bytes, StandardCharsets.UTF_8);
      } finally {
        try {
          blob.free();
        } catch (Exception ignored) {
          // ignore
        }
      }
    }
    return value.toString();
  }

  private List<String> parse(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      // H2 JSON 列通过 setString 写入时会被存为 JSON 字符串字面量，
      // 读出来是 "[\"...\"]" 形式，需要先解出内层 JSON 数组再解析。
      if (json.length() >= 2 && json.charAt(0) == '"' && json.charAt(json.length() - 1) == '"') {
        try {
          String inner = OBJECT_MAPPER.readValue(json, String.class);
          return OBJECT_MAPPER.readValue(inner, TYPE_REF);
        } catch (Exception innerEx) {
          // 不是字符串字面量包装，继续按原样解析
        }
      }
      return OBJECT_MAPPER.readValue(json, TYPE_REF);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse JSON to List<String>: " + json, e);
    }
  }
}
