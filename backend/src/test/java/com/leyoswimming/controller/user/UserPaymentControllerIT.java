package com.leyoswimming.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.Order;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.PackageTemplateCoach;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.enums.OrderStatus;
import com.leyoswimming.enums.OrderType;
import com.leyoswimming.enums.PackageMode;
import com.leyoswimming.enums.PackageTemplateStatus;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.OrderMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.PackageTemplateCoachMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
import com.leyoswimming.repository.PaymentMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.security.JwtTokenProvider;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserPaymentControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private UserMapper userMapper;
  @Autowired private CoachMapper coachMapper;
  @Autowired private PackageTemplateMapper packageTemplateMapper;
  @Autowired private PackageTemplateCoachMapper packageTemplateCoachMapper;
  @Autowired private OrderMapper orderMapper;
  @Autowired private PackageMapper packageMapper;
  @Autowired private PaymentMapper paymentMapper;

  private String token;
  private Long userId;

  @BeforeEach
  void setUp() {
    User user = createUser("学员 A");
    userId = user.getId();
    token = jwtTokenProvider.generateUserAccessToken(userId, true);
  }

  @Test
  @DisplayName("POST /api/order/pay 支付标准套餐订单成功")
  void pay_standardOrder_returnsPendingPayment() throws Exception {
    Long orderId = createStandardOrder(userId, new BigDecimal("1200.00"));

    mockMvc
        .perform(
            post("/api/order/pay")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"orderId\":%d,\"channel\":0}", orderId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.paymentId").isNumber())
        .andExpect(jsonPath("$.data.status").value("pending"));
  }

  @Test
  @DisplayName("POST /api/payment/mock-callback 支付成功后创建套餐")
  void mockCallback_success_createsPackage() throws Exception {
    Long orderId = createStandardOrder(userId, new BigDecimal("1200.00"));

    MvcResult payResult =
        mockMvc
            .perform(
                post("/api/order/pay")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("{\"orderId\":%d,\"channel\":0}", orderId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    JsonNode data = objectMapper.readTree(payResult.getResponse().getContentAsString()).path("data");
    String channelTradeNo = data.path("channelTradeNo").asText();

    mockMvc
        .perform(
            post("/api/payment/mock-callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"channel\":0,\"orderId\":%d,\"channelTradeNo\":\"%s\",\"amount\":1200.00,\"success\":true}",
                    orderId, channelTradeNo)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.code").value("SUCCESS"));

    Order order = orderMapper.selectById(orderId);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID.getValue());
    assertThat(order.getPackageId()).isNotNull();

    CoursePackage pkg = packageMapper.selectById(order.getPackageId());
    assertThat(pkg).isNotNull();
    assertThat(pkg.getStatus()).isEqualTo("active");
    assertThat(pkg.getAvailableCount()).isEqualTo(6);
    assertThat(pkg.getPaidAmount()).isEqualByComparingTo(new BigDecimal("1200.00"));
  }

  @Test
  @DisplayName("POST /api/payment/mock-callback 重复回调幂等")
  void mockCallback_duplicateCallback_idempotent() throws Exception {
    Long orderId = createStandardOrder(userId, new BigDecimal("1200.00"));

    MvcResult payResult =
        mockMvc
            .perform(
                post("/api/order/pay")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("{\"orderId\":%d,\"channel\":0}", orderId)))
            .andReturn();
    String channelTradeNo =
        objectMapper
            .readTree(payResult.getResponse().getContentAsString())
            .path("data")
            .path("channelTradeNo")
            .asText();

    String body =
        String.format(
            "{\"channel\":0,\"orderId\":%d,\"channelTradeNo\":\"%s\",\"amount\":1200.00,\"success\":true}",
            orderId, channelTradeNo);
    mockMvc
        .perform(post("/api/payment/mock-callback").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());
    mockMvc
        .perform(post("/api/payment/mock-callback").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());

    assertThat(packageMapper.selectCount(null)).isEqualTo(1);
  }

  @Test
  @DisplayName("POST /api/order/pay 非本人订单返回 420006")
  void pay_otherUserOrder_returnsNotFound() throws Exception {
    User other = createUser("学员 B");
    Long orderId = createStandardOrder(other.getId(), new BigDecimal("1200.00"));

    mockMvc
        .perform(
            post("/api/order/pay")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"orderId\":%d,\"channel\":0}", orderId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(420006));
  }

  private User createUser(String name) {
    User user = new User();
    user.setOpenid("openid-" + name);
    user.setPhone("1380000" + (int) (Math.random() * 10000));
    user.setName(name);
    user.setStatus(0);
    userMapper.insert(user);
    return user;
  }

  private Coach createCoach(String name, BigDecimal referencePrice) {
    Coach coach = new Coach();
    coach.setOpenid("openid-" + name);
    coach.setPhone("1390000" + (int) (Math.random() * 10000));
    coach.setName(name);
    coach.setStatus(CoachStatus.APPROVED.getValue());
    coach.setReferencePrice(referencePrice);
    coachMapper.insert(coach);
    return coach;
  }

  private PackageTemplate createActiveTemplate(String name, int totalHours, BigDecimal price) {
    PackageTemplate template = new PackageTemplate();
    template.setName(name);
    template.setPackageMode(PackageMode.STANDARD.getValue());
    template.setTeachingType("one_on_one");
    template.setTotalHours(totalHours);
    template.setDurationMinutes(60);
    template.setValidDays(30);
    template.setOriginalPrice(price);
    template.setPrice(price);
    template.setRefundEnabled(false);
    template.setRefundRatio(BigDecimal.ZERO);
    template.setRefundValidDays(0);
    template.setStatus(PackageTemplateStatus.ACTIVE.getValue());
    packageTemplateMapper.insert(template);
    return template;
  }

  private Long createStandardOrder(Long userId, BigDecimal amount) {
    Coach coach = createCoach("教练 P", new BigDecimal("200.00"));
    PackageTemplate template = createActiveTemplate("标准 6 节", 6, amount);
    PackageTemplateCoach link = new PackageTemplateCoach();
    link.setPackageTemplateId(template.getId());
    link.setCoachId(coach.getId());
    link.setReferencePriceSnapshot(coach.getReferencePrice());
    packageTemplateCoachMapper.insert(link);

    Order order = new Order();
    order.setOrderNo("O" + System.currentTimeMillis());
    order.setType(OrderType.PURCHASE.getValue());
    order.setStatus(OrderStatus.PENDING_PAYMENT.getValue());
    order.setUserId(userId);
    order.setCoachId(coach.getId());
    order.setPackageTemplateId(template.getId());
    order.setPackageMode(PackageMode.STANDARD.getValue());
    order.setPackageName(template.getName());
    order.setCoachName(coach.getName());
    order.setTeachingType(template.getTeachingType());
    order.setTotalHours(template.getTotalHours());
    order.setDurationMinutes(template.getDurationMinutes());
    order.setValidDays(template.getValidDays());
    order.setRefundEnabled(template.getRefundEnabled());
    order.setRefundRatio(template.getRefundRatio());
    order.setRefundValidDays(template.getRefundValidDays());
    order.setOriginalAmount(amount);
    order.setPaidAmount(BigDecimal.ZERO);
    orderMapper.insert(order);
    return order.getId();
  }
}
