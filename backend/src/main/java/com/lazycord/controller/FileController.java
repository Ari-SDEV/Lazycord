package com.lazycord.controller;

import com.lazycord.dto.FileUploadResponse;
import com.lazycord.model.Channel;
import com.lazycord.model.FileAttachment;
import com.lazycord.model.User;
import com.lazycord.service.ChannelService;
import com.lazycord.service.FileAttachmentService;
import com.lazycord.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:1420", "tauri://localhost"})
public class FileController {

    private final FileAttachmentService fileAttachmentService;
    private final ChannelService channelService;
    private final UserService userService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "channelId", required = false) UUID channelId,
            Authentication authentication) {
        
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Channel channel = null;
            if (channelId != null) {
                channel = channelService.findById(channelId)
                        .orElseThrow(() -> new RuntimeException("Channel not found"));
            }

            FileAttachment attachment = fileAttachmentService.uploadFile(file, user, channel);
            
            FileUploadResponse response = new FileUploadResponse(
                    attachment.getId(),
                    attachment.getOriginalName(),
                    attachment.getMimeType(),
                    attachment.getSize(),
                    "/api/files/" + attachment.getId(),
                    "/api/files/" + attachment.getId() + "/download"
            );

            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("Failed to upload file", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (RuntimeException e) {
            log.error("Upload error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID fileId) {
        FileAttachment attachment = fileAttachmentService.getFile(fileId);
        Resource resource = fileAttachmentService.loadFileAsResource(fileId);

        String contentType = attachment.getMimeType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getOriginalName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(attachment.getSize()))
                .body(resource);
    }

    @GetMapping("/{fileId}/preview")
    public ResponseEntity<Resource> previewFile(@PathVariable UUID fileId) {
        FileAttachment attachment = fileAttachmentService.getFile(fileId);
        
        // Only allow preview for images
        if (!attachment.getMimeType().startsWith("image/")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Preview only available for images");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        Resource resource = fileAttachmentService.loadFileAsResource(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .body(resource);
    }

    @GetMapping("/channel/{channelId}")
    public ResponseEntity<List<FileUploadResponse>> getChannelFiles(@PathVariable UUID channelId) {
        Channel channel = channelService.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        List<FileAttachment> files = fileAttachmentService.getChannelFiles(channel);
        List<FileUploadResponse> responses = files.stream()
                .map(f -> new FileUploadResponse(
                        f.getId(),
                        f.getOriginalName(),
                        f.getMimeType(),
                        f.getSize(),
                        "/api/files/" + f.getId(),
                        "/api/files/" + f.getId() + "/download"
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/my")
    public ResponseEntity<List<FileUploadResponse>> getMyFiles(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<FileAttachment> files = fileAttachmentService.getUserFiles(user);
        List<FileUploadResponse> responses = files.stream()
                .map(f -> new FileUploadResponse(
                        f.getId(),
                        f.getOriginalName(),
                        f.getMimeType(),
                        f.getSize(),
                        "/api/files/" + f.getId(),
                        "/api/files/" + f.getId() + "/download"
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/storage")
    public ResponseEntity<Map<String, Object>> getStorageInfo(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        long usedBytes = fileAttachmentService.getUserStorageUsed(user);
        long fileCount = fileAttachmentService.getUserFileCount(user);

        Map<String, Object> info = new HashMap<>();
        info.put("usedBytes", usedBytes);
        info.put("usedMB", usedBytes / (1024.0 * 1024.0));
        info.put("fileCount", fileCount);

        return ResponseEntity.ok(info);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID fileId, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        fileAttachmentService.deleteFile(fileId, user);
        return ResponseEntity.ok().build();
    }
}
