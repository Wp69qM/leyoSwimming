package com.leyoswimming.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.entity.Booking;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.Order;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.OrderStatus;
import com.leyoswimming.enums.OrderType;
import com.leyoswimming.repository.BookingMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.OrderMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.service.DistributedLockHelper;
import com.leyoswimming.util.OrderNoGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminPackageControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserMapper userMapper;
  @Autowired private CoachMapper coachMapper;
  @Autowired private PackageMapper packageMapper;
  @Autowired private OrderMapper orderMapper;
  @Autowired private BookingMapper bookingMapper;
  @MockBean private DistributedLockHelper lockHelper;

  private String token;
  private Long userId;
  private Long coachId;

  @BeforeEach
  void setUp() throws Exception {
    token = obtainAdminToken();
    userId = createUser("学员 A");
    coachId = createCoach("教练 A");

    given(lockHelper.lock(any(), any(), any()))
        .willReturn(new DistributedLockHelper.LockToken("lock:test", "token"));
    doNothing().when(lockHelper).unlockAfterTransaction(any());
  }

  @Test
  @DisplayName("POST /api/admin/package/list 无 token 返回 401")
  void list_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/package/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(200001));
  }

  @Test
  @DisplayName("POST /api/admin/package/list 带 token 返回分页列表")
  void list_withAdminToken_returnsPage() throws Exception {
    createPackage("active", 10, 8, 0);

    mockMvc
        .perform(
            post("/api/admin/package/list")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page\":1,\"pageSize\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.list").isArray())
        .andExpect(jsonPath("$.data.list[0].userName").value("学员 A"))
        .andExpect(jsonPath("$.data.list[0].coachName").value("教练 A"));
  }

  @Test
  @DisplayName("POST /api/admin/package/detail 查询详情成功")
  void detail_existingPackage_returnsDetails() throws Exception {
    Long packageId = createPackage("active", 10, 8, 0);
    createPurchaseOrder(packageId);

    mockMvc
        .perform(
            post("/api/admin/package/detail")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"packageId\":" + packageId + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.packageId").value(packageId.intValue()))
        .andExpect(jsonPath("$.data.userName").value("学员 A"))
        .andExpect(jsonPath("$.data.coachName").value("教练 A"))
        .andExpect(jsonPath("$.data.version").value(0))
        .andExpect(jsonPath("$.data.relatedOrders[0].type").value("purchase"));
  }

  @Test
  @DisplayName("POST /api/admin/package/freeze 冻结 active 套餐成功")
  void freeze_activePackage_returnsSuccess() throws Exception {
    Long packageId = createPackage("active", 10, 8, 2);
    createFutureBooking(packageId);

    mockMvc
        .perform(
            post("/api/admin/package/freeze")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"packageId\":"
                        + packageId
                        + ",\"reasonDetail\":\"投诉处理中\",\"version\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.message").value("套餐已冻结"));

    CoursePackage pkg = packageMapper.selectById(packageId);
    assertThat(pkg).isNotNull();
    assertThat(pkg.getStatus()).isEqualTo("frozen");
    assertThat(pkg.getFrozenReason()).isEqualTo("admin_frozen");
    assertThat(pkg.getReservedCount()).isEqualTo(0);
    assertThat(pkg.getAvailableCount()).isEqualTo(10);
  }

  @Test
  @DisplayName("POST /api/admin/package/freeze 非 active 套餐返回 409")
  void freeze_notActivePackage_returnsConflict() throws Exception {
    Long packageId = createPackage("frozen", 10, 8, 0);

    mockMvc
        .perform(
            post("/api/admin/package/freeze")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"packageId\":"
                        + packageId
                        + ",\"reasonDetail\":\"投诉处理中\",\"version\":0}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(420106));
  }

  @Test
  @DisplayName("POST /api/admin/package/unfreeze 解冻 frozen 套餐成功")
  void unfreeze_frozenPackage_returnsSuccess() throws Exception {
    Long packageId = createPackage("frozen", 10, 8, 0);
    CoursePackage pkg = packageMapper.selectById(packageId);
    pkg.setFrozenReason("admin_frozen");
    packageMapper.updateById(pkg);
    int version = packageMapper.selectById(packageId).getVersion();

    mockMvc
        .perform(
            post("/api/admin/package/unfreeze")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"packageId\":" + packageId + ",\"version\":" + version + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.message").value("套餐已解冻"));

    CoursePackage updated = packageMapper.selectById(packageId);
    assertThat(updated).isNotNull();
    assertThat(updated.getStatus()).isEqualTo("active");
    assertThat(updated.getFrozenReason()).isNull();
  }

  @Test
  @DisplayName("POST /api/admin/package/unfreeze 非 frozen 套餐返回 409")
  void unfreeze_notFrozenPackage_returnsConflict() throws Exception {
    Long packageId = createPackage("active", 10, 8, 0);

    mockMvc
        .perform(
            post("/api/admin/package/unfreeze")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"packageId\":" + packageId + ",\"version\":0}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(420107));
  }

  @Test
  @DisplayName("POST /api/admin/package/extend 延期 expired 套餐成功")
  void extend_expiredPackage_returnsSuccess() throws Exception {
    Long packageId = createPackage("expired", 10, 8, 0);
    String newExpireAt =
        LocalDateTime.now().plusDays(60).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    mockMvc
        .perform(
            post("/api/admin/package/extend")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"packageId\":"
                        + packageId
                        + ",\"newExpireAt\":\""
                        + newExpireAt
                        + "\",\"reason\":\"学员出差一个月\",\"version\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.message").value("套餐已延期"));

    CoursePackage updated = packageMapper.selectById(packageId);
    assertThat(updated).isNotNull();
    assertThat(updated.getStatus()).isEqualTo("active");
    assertThat(updated.getExtendReason()).isEqualTo("学员出差一个月");
  }

  @Test
  @DisplayName("POST /api/admin/package/extend 不可延期套餐返回 409")
  void extend_notExtendablePackage_returnsConflict() throws Exception {
    Long packageId = createPackage("exhausted", 10, 0, 0);
    String newExpireAt =
        LocalDateTime.now().plusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    mockMvc
        .perform(
            post("/api/admin/package/extend")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"packageId\":"
                        + packageId
                        + ",\"newExpireAt\":\""
                        + newExpireAt
                        + "\",\"reason\":\"学员出差一个月\",\"version\":0}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(420108));
  }

  @Test
  @DisplayName("POST /api/admin/package/refund 发起退款成功")
  void refund_activePackage_returnsSuccess() throws Exception {
    Long packageId = createRefundablePackage("active", 10, 2, 0);
    createPurchaseOrder(packageId);

    mockMvc
        .perform(
            post("/api/admin/package/refund")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"packageId\":"
                        + packageId
                        + ",\"reason\":\"协商退款\",\"refundAmount\":1200.00,\"adjustReason\":\"协商一致\",\"version\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.message").value("退款订单已生成，请前往订单管理审批"))
        .andExpect(jsonPath("$.data.orderNo").isString());

    CoursePackage updated = packageMapper.selectById(packageId);
    assertThat(updated).isNotNull();
    assertThat(updated.getStatus()).isEqualTo("frozen");
    assertThat(updated.getFrozenReason()).isEqualTo("refund_pending");
  }

  @Test
  @DisplayName("POST /api/admin/package/refund 不可退款套餐返回 409")
  void refund_notRefundablePackage_returnsConflict() throws Exception {
    Long packageId = createPackage("active", 10, 8, 0);

    mockMvc
        .perform(
            post("/api/admin/package/refund")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"packageId\":"
                        + packageId
                        + ",\"reason\":\"协商退款\",\"version\":0}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(420003));
  }

  @Test
  @DisplayName("POST /api/admin/package/refund 重复发起退款返回 409")
  void refund_pendingExists_returnsConflict() throws Exception {
    Long packageId = createRefundablePackage("active", 10, 2, 0);
    createPurchaseOrder(packageId);

    mockMvc
        .perform(
            post("/api/admin/package/refund")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"packageId\":"
                        + packageId
                        + ",\"reason\":\"协商退款\",\"version\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    CoursePackage pkg = packageMapper.selectById(packageId);
    pkg.setStatus("active");
    pkg.setFrozenReason(null);
    packageMapper.updateById(pkg);
    int version = packageMapper.selectById(packageId).getVersion();

    mockMvc
        .perform(
            post("/api/admin/package/refund")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"packageId\":"
                        + packageId
                        + ",\"reason\":\"协商退款\",\"version\":" + version + "}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(420005));
  }

  @Test
  @DisplayName("POST /api/admin/package/freeze 版本不一致返回 409")
  void freeze_concurrentUpdate_returnsConflict() throws Exception {
    Long packageId = createPackage("active", 10, 8, 0);

    mockMvc
        .perform(
            post("/api/admin/package/freeze")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"packageId\":"
                        + packageId
                        + ",\"reasonDetail\":\"投诉处理中\",\"version\":99}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(420110));
  }

  private String obtainAdminToken() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                      {"username":"admin","password":"admin123"}
                      """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }

  private Long createUser(String name) {
    User user = new User();
    user.setOpenid("openid-" + name);
    user.setPhone("1380000" + (int) (Math.random() * 10000));
    user.setName(name);
    user.setStatus(0);
    userMapper.insert(user);
    return user.getId();
  }

  private Long createCoach(String name) {
    Coach coach = new Coach();
    coach.setOpenid("openid-" + name);
    coach.setPhone("1390000" + (int) (Math.random() * 10000));
    coach.setName(name);
    coach.setStatus(2);
    coach.setReferencePrice(new BigDecimal("300.00"));
    coachMapper.insert(coach);
    return coach.getId();
  }

  private Long createPackage(String status, int totalHours, int availableCount, int reservedCount) {
    CoursePackage pkg = new CoursePackage();
    pkg.setUserId(userId);
    pkg.setCoachId(coachId);
    pkg.setPackageMode("standard");
    pkg.setTotalHours(totalHours);
    pkg.setConsumedCount(totalHours - availableCount - reservedCount);
    pkg.setReservedCount(reservedCount);
    pkg.setAvailableCount(availableCount);
    pkg.setPricePerHour(new BigDecimal("300.00"));
    pkg.setPaidAmount(new BigDecimal("1800.00"));
    pkg.setOriginalPrice(new BigDecimal("2000.00"));
    pkg.setRefundEnabled(false);
    pkg.setRefundRatio(BigDecimal.ONE);
    pkg.setRefundValidDays(0);
    pkg.setStatus(status);
    pkg.setExpireAt(LocalDateTime.now().plusDays(30));
    packageMapper.insert(pkg);
    return pkg.getId();
  }

  private Long createRefundablePackage(
      String status, int totalHours, int consumedCount, int reservedCount) {
    CoursePackage pkg = new CoursePackage();
    pkg.setUserId(userId);
    pkg.setCoachId(coachId);
    pkg.setPackageMode("standard");
    pkg.setTotalHours(totalHours);
    pkg.setConsumedCount(consumedCount);
    pkg.setReservedCount(reservedCount);
    pkg.setAvailableCount(totalHours - consumedCount - reservedCount);
    pkg.setPricePerHour(new BigDecimal("180.00"));
    pkg.setPaidAmount(new BigDecimal("1800.00"));
    pkg.setOriginalPrice(new BigDecimal("2000.00"));
    pkg.setRefundEnabled(true);
    pkg.setRefundRatio(BigDecimal.ONE);
    pkg.setRefundValidDays(30);
    pkg.setStatus(status);
    pkg.setExpireAt(LocalDateTime.now().plusDays(30));
    packageMapper.insert(pkg);
    return pkg.getId();
  }

  private Long createPurchaseOrder(Long packageId) {
    Order order = new Order();
    order.setOrderNo(OrderNoGenerator.generateOrderNo("P"));
    order.setType(OrderType.PURCHASE.getValue());
    order.setStatus(OrderStatus.PAID.getValue());
    order.setUserId(userId);
    order.setCoachId(coachId);
    order.setPackageId(packageId);
    order.setOriginalAmount(new BigDecimal("1800.00"));
    order.setPaidAmount(new BigDecimal("1800.00"));
    order.setPaymentMethod("wechat");
    orderMapper.insert(order);
    return order.getId();
  }

  private Long createFutureBooking(Long packageId) {
    Booking booking = new Booking();
    booking.setPackageId(packageId);
    booking.setCoachId(coachId);
    booking.setUserId(userId);
    booking.setStartTime(LocalDateTime.now().plusDays(1));
    booking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
    booking.setStatus("booked");
    bookingMapper.insert(booking);
    return booking.getId();
  }
}
