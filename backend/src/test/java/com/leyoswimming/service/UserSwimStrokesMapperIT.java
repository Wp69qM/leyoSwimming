package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.leyoswimming.entity.User;
import com.leyoswimming.repository.UserMapper;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserSwimStrokesMapperIT {

  @Autowired private UserMapper userMapper;
  @Autowired private DataSource dataSource;

  @Test
  @DisplayName("swimStrokes JSON 字段写入后能正确读出")
  void swimStrokes_writeAndRead_roundTrip() throws Exception {
    User user = new User();
    user.setOpenid("openid_swim_test_" + System.nanoTime());
    user.setPhone("enc_phone_swim_test");
    user.setPhoneHash("hash_swim_test" + System.nanoTime());
    user.setStatus(0);
    user.setName("泳姿测试");
    user.setProfileCompleted(true);
    user.setAge(25);
    user.setGender("male");
    user.setHasSwimBasis(true);
    user.setSwimStrokes(List.of("breaststroke", "freestyle"));

    userMapper.insert(user);
    Long userId = user.getId();
    assertThat(userId).isNotNull();

    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT swim_strokes FROM `user` WHERE id = " + userId)) {
      rs.next();
      Object raw = rs.getObject(1);
      String decoded = raw instanceof byte[] b ? new String(b, java.nio.charset.StandardCharsets.UTF_8) : String.valueOf(raw);
      System.out.println("RAW swim_strokes class=" + (raw == null ? "null" : raw.getClass().getName()) + ", decoded=[" + decoded + "]");
    }

    User loaded = userMapper.selectById(userId);
    System.out.println("ENTITY swimStrokes=" + loaded.getSwimStrokes());
    assertThat(loaded.getSwimStrokes())
        .containsExactlyInAnyOrder("breaststroke", "freestyle");
  }
}
