package com.leyoswimming.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.entity.Coach;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * 显式通过 {@code @TableField(typeHandler = CoachCertificateListJsonTypeHandler.class)} 使用，
 * 不注册为全局 List 类型处理器，避免与 StringListJsonTypeHandler 冲突。
 */
public class CoachCertificateListJsonTypeHandler
    extends BaseTypeHandler<List<Coach.CoachCertificate>> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<Coach.CoachCertificate>> TYPE_REF =
      new TypeReference<>() {};

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, List<Coach.CoachCertificate> parameter, JdbcType jdbcType)
      throws SQLException {
    try {
      ps.setString(i, OBJECT_MAPPER.writeValueAsString(parameter));
    } catch (Exception e) {
      throw new SQLException("Failed to serialize coach certificates to JSON", e);
    }
  }

  @Override
  public List<Coach.CoachCertificate> getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return parse(rs.getString(columnName));
  }

  @Override
  public List<Coach.CoachCertificate> getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return parse(rs.getString(columnIndex));
  }

  @Override
  public List<Coach.CoachCertificate> getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return parse(cs.getString(columnIndex));
  }

  private List<Coach.CoachCertificate> parse(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return OBJECT_MAPPER.readValue(json, TYPE_REF);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse JSON to List<CoachCertificate>: " + json, e);
    }
  }
}
