package com.leyoswimming.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.entity.AiKnowledgeDocument;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AiKnowledgeDocumentMapper;
import com.leyoswimming.service.AiAssistantGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminKnowledgeControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AiKnowledgeDocumentMapper knowledgeDocumentMapper;
  @Autowired private PlatformTransactionManager transactionManager;

  @MockBean private AiAssistantGatewayService aiAssistantGatewayService;

  private String token;
  private TransactionTemplate requiresNewTemplate;

  @BeforeEach
  void setUp() throws Exception {
    requiresNewTemplate = new TransactionTemplate(transactionManager);
    requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    token = obtainAdminToken();
    doNothing().when(aiAssistantGatewayService).knowledgeIngest(any(), any(), any(), any());
    doNothing().when(aiAssistantGatewayService).knowledgeDelete(any());
    doNothing().when(aiAssistantGatewayService).knowledgeRebuild(any(), any(), any(), any());
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/add 无 token 返回 401")
  void add_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/knowledge/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/add 参数校验失败返回 100005")
  void add_invalidRequest_returnsValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/knowledge/add")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()));
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/add 手动新增知识库文档成功")
  void add_validTextDocument_returnsCreatedDocument() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/knowledge/add")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addRequest("安全须知", "safety", "text", "泳池安全须知内容")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.documentId").isNumber())
        .andExpect(jsonPath("$.data.title").value("安全须知"))
        .andExpect(jsonPath("$.data.category").value("safety"))
        .andExpect(jsonPath("$.data.contentType").value("text"))
        .andExpect(jsonPath("$.data.sourceType").value("manual"))
        .andExpect(jsonPath("$.data.status").value(0));

    verify(aiAssistantGatewayService, times(1))
        .knowledgeIngest(any(), any(), any(), any());
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/upload 上传 md 文件成功")
  void upload_validMarkdownFile_returnsCreatedDocument() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "guide.md",
            "text/markdown",
            "# 蛙泳技巧\n\n保持身体流线型。".getBytes());

    mockMvc
        .perform(
            multipart("/api/admin/knowledge/upload")
                .file(file)
                .param("title", "蛙泳技巧")
                .param("category", "technique")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.documentId").isNumber())
        .andExpect(jsonPath("$.data.title").value("蛙泳技巧"))
        .andExpect(jsonPath("$.data.category").value("technique"))
        .andExpect(jsonPath("$.data.contentType").value("markdown"))
        .andExpect(jsonPath("$.data.sourceType").value("file"))
        .andExpect(jsonPath("$.data.status").value(0));

    verify(aiAssistantGatewayService, times(1))
        .knowledgeIngest(any(), any(), any(), any());
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/add 标题重复返回 610001")
  void add_duplicateTitle_returnsDuplicateError() throws Exception {
    addDocument("去重标题", "safety", "text", "内容");

    mockMvc
        .perform(
            post("/api/admin/knowledge/add")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addRequest("去重标题", "emergency", "markdown", "其他内容")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.KNOWLEDGE_TITLE_DUPLICATE.getCode()));

    verify(aiAssistantGatewayService, times(1))
        .knowledgeIngest(any(), any(), any(), any());
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/list 支持分类、状态、关键字过滤")
  void list_withFilters_returnsMatchingDocuments() throws Exception {
    addDocument("安全手册", "safety", "text", "内容 A");
    Long disabledId = addDocument("急救指南", "emergency", "markdown", "内容 B");
    toggleDocument(disabledId);

    mockMvc
        .perform(
            post("/api/admin/knowledge/list")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page\":1,\"pageSize\":10,\"category\":\"safety\",\"keyword\":\"手册\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.list").isArray())
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.list[0].title").value("安全手册"))
        .andExpect(jsonPath("$.data.list[0].createdBy").value("系统管理员"));

    mockMvc
        .perform(
            post("/api/admin/knowledge/list")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page\":1,\"pageSize\":10,\"status\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.list[0].title").value("急救指南"));
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/toggle 禁用文档并调用 AI 服务删除索引")
  void toggle_existingDocument_disablesAndCallsDelete() throws Exception {
    Long documentId = addDocument("待禁用", "other", "text", "内容");

    mockMvc
        .perform(
            post("/api/admin/knowledge/toggle")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documentId\":" + documentId + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value(1));

    verify(aiAssistantGatewayService, times(1)).knowledgeDelete(String.valueOf(documentId));
    verify(aiAssistantGatewayService, never()).knowledgeRebuild(any(), any(), any(), any());
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/toggle 重新启用文档并调用 AI 服务重建索引")
  void toggle_disabledDocument_enablesAndCallsRebuild() throws Exception {
    Long documentId = addDocument("待启用", "technique", "markdown", "重建内容");
    toggleDocument(documentId);

    mockMvc
        .perform(
            post("/api/admin/knowledge/toggle")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documentId\":" + documentId + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value(0));

    verify(aiAssistantGatewayService, times(1)).knowledgeDelete(String.valueOf(documentId));
    verify(aiAssistantGatewayService, times(1))
        .knowledgeRebuild(String.valueOf(documentId), "待启用", "technique", "重建内容");
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/delete 删除文档并调用 AI 服务删除索引")
  void delete_existingDocument_removesDocument() throws Exception {
    Long documentId = addDocument("待删除", "safety", "text", "内容");

    mockMvc
        .perform(
            post("/api/admin/knowledge/delete")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documentId\":" + documentId + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    verify(aiAssistantGatewayService, times(1)).knowledgeDelete(String.valueOf(documentId));
    assertThat(knowledgeDocumentMapper.selectById(documentId)).isNull();
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/add AI 服务失败时回滚 MySQL 插入")
  void add_aiServiceFailure_rollsBackInsert() throws Exception {
    doThrow(new BusinessException(ErrorCode.KNOWLEDGE_INGEST_FAILED))
        .when(aiAssistantGatewayService)
        .knowledgeIngest(any(), any(), any(), any());

    mockMvc
        .perform(
            post("/api/admin/knowledge/add")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addRequest("回滚测试", "safety", "text", "内容")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.KNOWLEDGE_INGEST_FAILED.getCode()));

    assertThat(countByTitle("回滚测试")).isZero();
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/upload AI 服务失败时回滚 MySQL 插入")
  void upload_aiServiceFailure_rollsBackInsert() throws Exception {
    doThrow(new BusinessException(ErrorCode.KNOWLEDGE_INGEST_FAILED))
        .when(aiAssistantGatewayService)
        .knowledgeIngest(any(), any(), any(), any());

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "rollback.txt",
            "text/plain",
            "回滚测试内容".getBytes());

    mockMvc
        .perform(
            multipart("/api/admin/knowledge/upload")
                .file(file)
                .param("title", "回滚上传")
                .param("category", "other")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.KNOWLEDGE_INGEST_FAILED.getCode()));

    assertThat(countByTitle("回滚上传")).isZero();
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/upload 空文件返回 610004")
  void upload_emptyFile_returnsEmptyFileError() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "empty.md",
            "text/markdown",
            new byte[0]);

    mockMvc
        .perform(
            multipart("/api/admin/knowledge/upload")
                .file(file)
                .param("title", "空文件")
                .param("category", "other")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.KNOWLEDGE_FILE_EMPTY.getCode()));
  }

  @Test
  @DisplayName("POST /api/admin/knowledge/upload 非法文件类型返回 610005")
  void upload_invalidFileType_returnsInvalidFileTypeError() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "virus.exe",
            "application/octet-stream",
            "内容".getBytes());

    mockMvc
        .perform(
            multipart("/api/admin/knowledge/upload")
                .file(file)
                .param("title", "非法文件")
                .param("category", "other")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.KNOWLEDGE_FILE_TYPE_INVALID.getCode()));
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

  private Long addDocument(String title, String category, String contentType, String content)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/admin/knowledge/add")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(addRequest(title, category, contentType, content)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .path("data")
        .path("documentId")
        .asLong();
  }

  private void toggleDocument(Long documentId) throws Exception {
    mockMvc
        .perform(
            post("/api/admin/knowledge/toggle")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documentId\":" + documentId + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }

  private long countByTitle(String title) {
    Long count = requiresNewTemplate.execute(status -> knowledgeDocumentMapper.selectCountByTitle(title));
    return count != null ? count : 0L;
  }

  private String addRequest(String title, String category, String contentType, String content) {
    return """
        {
          "title": "%s",
          "category": "%s",
          "contentType": "%s",
          "content": "%s"
        }
        """
        .formatted(title, category, contentType, content);
  }
}
