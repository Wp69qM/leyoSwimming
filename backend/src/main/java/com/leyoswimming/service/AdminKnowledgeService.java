package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.KnowledgeAddRequest;
import com.leyoswimming.dto.request.KnowledgeDeleteRequest;
import com.leyoswimming.dto.request.KnowledgeListRequest;
import com.leyoswimming.dto.request.KnowledgeToggleRequest;
import com.leyoswimming.dto.request.KnowledgeUploadRequest;
import com.leyoswimming.dto.response.DownloadFile;
import com.leyoswimming.dto.response.KnowledgeDetailResponse;
import com.leyoswimming.dto.response.KnowledgeListItemResponse;
import com.leyoswimming.dto.response.KnowledgeListResponse;
import com.leyoswimming.entity.AdminUser;
import com.leyoswimming.entity.AiKnowledgeDocument;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AdminUserMapper;
import com.leyoswimming.repository.AiKnowledgeDocumentMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminKnowledgeService {

  private static final Set<String> VALID_CATEGORIES =
      Set.of("safety", "technique", "emergency", "other");
  private static final Set<String> VALID_CONTENT_TYPES = Set.of("text", "markdown");
  private static final Set<String> VALID_FILE_EXTENSIONS = Set.of("txt", "md");
  private static final int STATUS_ENABLED = 0;
  private static final int STATUS_DISABLED = 1;
  private static final String SOURCE_MANUAL = "manual";
  private static final String SOURCE_FILE = "file";

  private final AiKnowledgeDocumentMapper knowledgeDocumentMapper;
  private final AdminUserMapper adminUserMapper;
  private final AdminPermissionHelper permissionHelper;
  private final AiAssistantGatewayService aiAssistantGatewayService;

  @Transactional(readOnly = true)
  public KnowledgeListResponse list(Long adminId, KnowledgeListRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_KNOWLEDGE_READ);

    LambdaQueryWrapper<AiKnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.isNotBlank(request.category())) {
      wrapper.eq(AiKnowledgeDocument::getCategory, request.category().trim());
    }
    if (request.status() != null) {
      wrapper.eq(AiKnowledgeDocument::getStatus, request.status());
    }
    if (StringUtils.isNotBlank(request.keyword())) {
      wrapper.like(AiKnowledgeDocument::getTitle, request.keyword().trim());
    }
    wrapper.orderByDesc(AiKnowledgeDocument::getCreatedAt);

    Page<AiKnowledgeDocument> pageParam = new Page<>(request.page(), request.pageSize());
    Page<AiKnowledgeDocument> result = knowledgeDocumentMapper.selectPage(pageParam, wrapper);

    Map<Long, String> adminNameMap = buildAdminNameMapFromDocuments(result.getRecords());
    List<KnowledgeListItemResponse> list =
        result.getRecords().stream()
            .map(doc -> toItemResponse(doc, adminNameMap))
            .toList();
    return new KnowledgeListResponse(
        list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
  }

  @Transactional
  public KnowledgeListItemResponse add(Long adminId, KnowledgeAddRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_KNOWLEDGE_WRITE);
    validateAddRequest(request);

    AiKnowledgeDocument document = new AiKnowledgeDocument();
    document.setTitle(request.title().trim());
    document.setCategory(request.category().trim());
    document.setContentType(request.contentType().trim());
    document.setContent(request.content().trim());
    document.setSourceType(SOURCE_MANUAL);
    document.setStatus(STATUS_ENABLED);
    document.setCreatedBy(adminId);

    try {
      knowledgeDocumentMapper.insert(document);
    } catch (DuplicateKeyException e) {
      throw new BusinessException(ErrorCode.KNOWLEDGE_TITLE_DUPLICATE);
    }

    ingestDocument(document);
    return toItemResponse(document, adminNameMap(adminId));
  }

  @Transactional
  public KnowledgeListItemResponse upload(Long adminId, KnowledgeUploadRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_KNOWLEDGE_WRITE);
    validateUploadRequest(request);

    String content = readFileContent(request.file());
    if (StringUtils.isBlank(content)) {
      throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_EMPTY);
    }

    String contentType = resolveContentTypeFromFileName(request.file().getOriginalFilename());

    AiKnowledgeDocument document = new AiKnowledgeDocument();
    document.setTitle(request.title().trim());
    document.setCategory(request.category().trim());
    document.setContentType(contentType);
    document.setContent(content.trim());
    document.setSourceType(SOURCE_FILE);
    document.setStatus(STATUS_ENABLED);
    document.setCreatedBy(adminId);

    try {
      knowledgeDocumentMapper.insert(document);
    } catch (DuplicateKeyException e) {
      throw new BusinessException(ErrorCode.KNOWLEDGE_TITLE_DUPLICATE);
    }

    ingestDocument(document);
    return toItemResponse(document, adminNameMap(adminId));
  }

  @Transactional
  public KnowledgeListItemResponse toggle(Long adminId, KnowledgeToggleRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_KNOWLEDGE_WRITE);
    AiKnowledgeDocument document = findDocument(request.documentId());

    int newStatus =
        document.getStatus() == null || document.getStatus() == STATUS_ENABLED
            ? STATUS_DISABLED
            : STATUS_ENABLED;
    document.setStatus(newStatus);
    knowledgeDocumentMapper.updateById(document);

    if (newStatus == STATUS_DISABLED) {
      aiAssistantGatewayService.knowledgeDelete(String.valueOf(document.getId()));
    } else {
      aiAssistantGatewayService.knowledgeRebuild(
          String.valueOf(document.getId()),
          document.getTitle(),
          document.getCategory(),
          document.getContent());
    }

    return toItemResponse(document, adminNameMap(adminId));
  }

  @Transactional
  public void delete(Long adminId, KnowledgeDeleteRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_KNOWLEDGE_WRITE);
    AiKnowledgeDocument document = findDocument(request.documentId());

    knowledgeDocumentMapper.deleteById(document.getId());
    aiAssistantGatewayService.knowledgeDelete(String.valueOf(document.getId()));
  }

  @Transactional(readOnly = true)
  public KnowledgeDetailResponse detail(Long adminId, Long documentId) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_KNOWLEDGE_READ);
    AiKnowledgeDocument document = findDocument(documentId);
    Map<Long, String> adminNameMap = adminNameMap(document.getCreatedBy());
    return toDetailResponse(document, adminNameMap);
  }

  @Transactional(readOnly = true)
  public DownloadFile download(Long adminId, Long documentId) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_KNOWLEDGE_READ);
    AiKnowledgeDocument document = findDocument(documentId);

    String content = StringUtils.defaultString(document.getContent(), "");
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    InputStream inputStream = new ByteArrayInputStream(bytes);
    String filename = buildDownloadFilename(document);
    String contentType = "text/plain;charset=UTF-8";
    return new DownloadFile(inputStream, bytes.length, filename, contentType);
  }

  private String buildDownloadFilename(AiKnowledgeDocument document) {
    String extension = "markdown".equalsIgnoreCase(document.getContentType()) ? ".md" : ".txt";
    String title = StringUtils.defaultString(document.getTitle(), "document");
    return title + extension;
  }

  private AiKnowledgeDocument findDocument(Long documentId) {
    AiKnowledgeDocument document = knowledgeDocumentMapper.selectById(documentId);
    if (document == null) {
      throw new BusinessException(ErrorCode.KNOWLEDGE_NOT_FOUND);
    }
    return document;
  }

  private void ingestDocument(AiKnowledgeDocument document) {
    try {
      aiAssistantGatewayService.knowledgeIngest(
          String.valueOf(document.getId()),
          document.getTitle(),
          document.getCategory(),
          document.getContent());
    } catch (BusinessException e) {
      throw e;
    } catch (RuntimeException e) {
      log.error("Knowledge ingest failed for documentId={}", document.getId(), e);
      throw new BusinessException(ErrorCode.KNOWLEDGE_INGEST_FAILED);
    }
  }

  private void validateAddRequest(KnowledgeAddRequest request) {
    if (!VALID_CATEGORIES.contains(request.category())) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "分类参数错误");
    }
    if (!VALID_CONTENT_TYPES.contains(request.contentType())) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "内容类型参数错误");
    }
    if (titleExists(request.title().trim())) {
      throw new BusinessException(ErrorCode.KNOWLEDGE_TITLE_DUPLICATE);
    }
  }

  private void validateUploadRequest(KnowledgeUploadRequest request) {
    if (!VALID_CATEGORIES.contains(request.category())) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "分类参数错误");
    }
    MultipartFile file = request.file();
    if (file.isEmpty()) {
      throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_EMPTY);
    }
    String extension = getFileExtension(file.getOriginalFilename());
    if (!VALID_FILE_EXTENSIONS.contains(extension)) {
      throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_TYPE_INVALID);
    }
    if (titleExists(request.title().trim())) {
      throw new BusinessException(ErrorCode.KNOWLEDGE_TITLE_DUPLICATE);
    }
  }

  private boolean titleExists(String title) {
    return knowledgeDocumentMapper.selectCountByTitle(title) > 0;
  }

  private String readFileContent(MultipartFile file) {
    try {
      return new String(file.getBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("Failed to read uploaded knowledge file", e);
      throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_EMPTY);
    }
  }

  private String resolveContentTypeFromFileName(String filename) {
    String extension = getFileExtension(filename);
    return "md".equalsIgnoreCase(extension) ? "markdown" : "text";
  }

  private String getFileExtension(String filename) {
    if (StringUtils.isBlank(filename)) {
      return "";
    }
    int lastDot = filename.lastIndexOf('.');
    return lastDot < 0 || lastDot == filename.length() - 1
        ? ""
        : filename.substring(lastDot + 1).toLowerCase();
  }

  private KnowledgeListItemResponse toItemResponse(
      AiKnowledgeDocument document, Map<Long, String> adminNameMap) {
    String createdByName =
        adminNameMap.getOrDefault(
            document.getCreatedBy(),
            document.getCreatedBy() != null ? String.valueOf(document.getCreatedBy()) : "");
    return new KnowledgeListItemResponse(
        document.getId(),
        document.getTitle(),
        document.getCategory(),
        document.getContentType(),
        document.getSourceType(),
        document.getStatus(),
        createdByName,
        document.getCreatedAt(),
        document.getUpdatedAt());
  }

  private Map<Long, String> adminNameMap(Long adminId) {
    if (adminId == null) {
      return Collections.emptyMap();
    }
    return buildAdminNameMap(List.of(adminId));
  }

  private Map<Long, String> buildAdminNameMapFromDocuments(List<AiKnowledgeDocument> documents) {
    if (documents == null || documents.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Long> adminIds =
        documents.stream()
            .map(AiKnowledgeDocument::getCreatedBy)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    return buildAdminNameMap(adminIds);
  }

  private Map<Long, String> buildAdminNameMap(List<Long> adminIds) {
    if (adminIds == null || adminIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return adminUserMapper.selectBatchIds(adminIds).stream()
        .collect(Collectors.toMap(AdminUser::getId, u -> StringUtils.defaultString(u.getName(), u.getUsername())));
  }

  private KnowledgeDetailResponse toDetailResponse(
      AiKnowledgeDocument document, Map<Long, String> adminNameMap) {
    String createdByName =
        adminNameMap.getOrDefault(
            document.getCreatedBy(),
            document.getCreatedBy() != null ? String.valueOf(document.getCreatedBy()) : "");
    return new KnowledgeDetailResponse(
        document.getId(),
        document.getTitle(),
        document.getCategory(),
        document.getContentType(),
        document.getSourceType(),
        document.getContent(),
        document.getStatus(),
        createdByName,
        document.getCreatedAt(),
        document.getUpdatedAt());
  }
}
