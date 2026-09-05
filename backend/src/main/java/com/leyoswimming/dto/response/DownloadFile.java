package com.leyoswimming.dto.response;

import java.io.InputStream;

public record DownloadFile(
    InputStream inputStream,
    long contentLength,
    String filename,
    String contentType) {}
