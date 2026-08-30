package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileStorageServiceTest {

  private FileStorageService fileStorageService;

  @TempDir java.nio.file.Path tempDir;

  @BeforeEach
  void setUp() {
    fileStorageService = new FileStorageService();
    org.springframework.test.util.ReflectionTestUtils.setField(
        fileStorageService, "uploadDir", tempDir.toString());
    org.springframework.test.util.ReflectionTestUtils.setField(
        fileStorageService, "baseUrl", "/uploads/");
  }

  @Test
  @DisplayName("存储 JPEG 文件成功并使用 jpg 扩展名")
  void store_jpegFile_returnsJpgUrl() throws IOException {
    MultipartFile file = createFile("image/jpeg", "malicious.png", jpegBytes());

    String url = fileStorageService.store(file);

    assertThat(url).startsWith("/uploads/").endsWith(".jpg");
  }

  @Test
  @DisplayName("image/jpg 内容类型按 JPEG 魔数校验通过")
  void store_imageJpgContentType_validJpegMagic_returnsJpgUrl() throws IOException {
    MultipartFile file = createFile("image/jpg", "photo.webp", jpegBytes());

    String url = fileStorageService.store(file);

    assertThat(url).startsWith("/uploads/").endsWith(".jpg");
  }

  @Test
  @DisplayName("扩展名伪造但魔数不匹配时拒绝")
  void store_extensionForgedButMagicMismatch_throwsInvalidFileType() throws IOException {
    MultipartFile file = createFile("image/jpeg", "safe.jpg.png.exe", pngBytes());

    assertThatThrownBy(() -> fileStorageService.store(file))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_TYPE));
  }

  @Test
  @DisplayName("不允许的内容类型直接拒绝")
  void store_unsupportedContentType_throwsInvalidFileType() throws IOException {
    MultipartFile file = createFile("application/pdf", "doc.pdf", new byte[] {0x25, 0x50, 0x44, 0x46});

    assertThatThrownBy(() -> fileStorageService.store(file))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_TYPE));
  }

  private MultipartFile createFile(String contentType, String filename, byte[] content)
      throws IOException {
    return new MockMultipartFile("file", filename, contentType, new ByteArrayInputStream(content));
  }

  private byte[] jpegBytes() {
    return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00, 0x00};
  }

  private byte[] pngBytes() {
    return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
  }
}
