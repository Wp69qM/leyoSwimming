package com.leyoswimming.dto.ai.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiGatewayDtoSerializationTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  @DisplayName("AiSessionCreateResponse 可从 snake_case JSON 反序列化并序列化为 camelCase")
  void sessionCreateResponse_deserializesFromSnakeCase() throws Exception {
    String json = """
        {"session_id":"sess_abc123","welcome_message":"欢迎"}""";

    AiSessionCreateResponse response = mapper.readValue(json, AiSessionCreateResponse.class);

    assertThat(response.sessionId()).isEqualTo("sess_abc123");
    assertThat(response.welcomeMessage()).isEqualTo("欢迎");

    String output = mapper.writeValueAsString(response);
    assertThat(output).contains("\"sessionId\":\"sess_abc123\"");
    assertThat(output).contains("\"welcomeMessage\":\"欢迎\"");
  }

  @Test
  @DisplayName("AiChatResponse 可从 snake_case JSON 反序列化")
  void chatResponse_deserializesFromSnakeCase() throws Exception {
    String json = """
        {
          "session_id":"sess_abc123",
          "message_id":"msg_abc123",
          "reply":{
            "text":"你好",
            "recommendations":[
              {
                "type":"coach",
                "id":1,
                "coach_id":10,
                "name":"教练A",
                "coach_name":"教练A",
                "avatar_url":"http://example.com/avatar.jpg",
                "rating":4.9,
                "teaching_years":5,
                "reference_price":200,
                "price":180,
                "hours":10,
                "price_per_hour":180,
                "total_price":1800,
                "class_size":"一对一",
                "validity_days":90,
                "strokes":["自由泳"],
                "reason":"推荐原因"
              }
            ],
            "suggested_questions":["问题1","问题2"]
          }
        }""";

    AiChatResponse response = mapper.readValue(json, AiChatResponse.class);

    assertThat(response.sessionId()).isEqualTo("sess_abc123");
    assertThat(response.messageId()).isEqualTo("msg_abc123");
    assertThat(response.reply().text()).isEqualTo("你好");
    assertThat(response.reply().suggestedQuestions()).containsExactly("问题1", "问题2");

    AiRecommendationResponse recommendation = response.reply().recommendations().get(0);
    assertThat(recommendation.coachId()).isEqualTo(10L);
    assertThat(recommendation.avatarUrl()).isEqualTo("http://example.com/avatar.jpg");
    assertThat(recommendation.teachingYears()).isEqualTo(5);
    assertThat(recommendation.pricePerHour()).isEqualTo(180);
    assertThat(recommendation.totalPrice()).isEqualTo(1800);
    assertThat(recommendation.classSize()).isEqualTo("一对一");
    assertThat(recommendation.validityDays()).isEqualTo(90);
  }
}
