package com.leyoswimming.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.entity.AdminUser;
import com.leyoswimming.repository.AdminUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

  private final AdminUserMapper adminUserMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(String... args) {
    seedAdminUser();
    seedDisabledAdminUser();
  }

  private void seedAdminUser() {
    String username = "admin";
    AdminUser existing =
        adminUserMapper.selectOne(
            new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username));
    if (existing != null) {
      return;
    }
    AdminUser admin = new AdminUser();
    admin.setUsername(username);
    admin.setPasswordHash(passwordEncoder.encode("admin123"));
    admin.setName("系统管理员");
    admin.setRole("super_admin");
    admin.setStatus(0);
    adminUserMapper.insert(admin);
    log.info("Seeded default admin user: {}", username);
  }

  private void seedDisabledAdminUser() {
    String username = "disabled_admin";
    AdminUser existing =
        adminUserMapper.selectOne(
            new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username));
    if (existing != null) {
      return;
    }
    AdminUser admin = new AdminUser();
    admin.setUsername(username);
    admin.setPasswordHash(passwordEncoder.encode("admin123"));
    admin.setName("已禁用管理员");
    admin.setRole("admin");
    admin.setStatus(1);
    adminUserMapper.insert(admin);
    log.info("Seeded disabled admin user: {}", username);
  }
}
