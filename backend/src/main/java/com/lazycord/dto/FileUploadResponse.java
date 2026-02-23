package com.lazycord.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private UUID id;
    private String originalName;
    private String mimeType;
    private long size;
    private String url;
    private String downloadUrl;
}
