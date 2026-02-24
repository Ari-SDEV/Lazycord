package com.lazycord.service;

import com.lazycord.model.Channel;
import com.lazycord.model.FileAttachment;
import com.lazycord.model.User;
import com.lazycord.repository.FileAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileAttachmentService {

    private final FileAttachmentRepository fileAttachmentRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public FileAttachment uploadFile(MultipartFile file, User user, Channel channel) throws IOException {
        // Check for duplicate (deduplication)
        String fileHash = fileStorageService.calculateHash(file);
        var existingFile = fileAttachmentRepository.findByFileHashAndDeletedFalse(fileHash);
        if (existingFile.isPresent()) {
            log.info("File already exists, returning existing: {}", existingFile.get().getId());
            return existingFile.get();
        }

        // Store file
        String storagePath = fileStorageService.storeFile(file, user);

        // Create attachment record
        FileAttachment attachment = new FileAttachment();
        attachment.setFilename(UUID.randomUUID().toString());
        attachment.setOriginalName(file.getOriginalFilename());
        attachment.setMimeType(file.getContentType());
        attachment.setSize(file.getSize());
        attachment.setStoragePath(storagePath);
        attachment.setFileHash(fileHash);
        attachment.setUploadedBy(user);
        attachment.setChannel(channel);
        attachment.setDeleted(false);

        return fileAttachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public FileAttachment getFile(UUID fileId) {
        return fileAttachmentRepository.findById(fileId)
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    @Transactional(readOnly = true)
    public Resource loadFileAsResource(UUID fileId) {
        FileAttachment attachment = getFile(fileId);
        return fileStorageService.loadFileAsResource(attachment.getStoragePath());
    }

    @Transactional(readOnly = true)
    public List<FileAttachment> getChannelFiles(Channel channel) {
        return fileAttachmentRepository.findByChannelAndDeletedFalse(channel);
    }

    @Transactional(readOnly = true)
    public List<FileAttachment> getUserFiles(User user) {
        return fileAttachmentRepository.findByUploadedByAndDeletedFalse(user);
    }

    @Transactional
    public void deleteFile(UUID fileId, User user) {
        FileAttachment attachment = fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Check permission
        if (!attachment.getUploadedBy().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to delete this file");
        }

        // Soft delete
        attachment.setDeleted(true);
        fileAttachmentRepository.save(attachment);

        // Hard delete from storage
        fileStorageService.deleteFile(attachment.getStoragePath());

        log.info("File deleted: {} by user: {}", fileId, user.getUsername());
    }

    @Transactional(readOnly = true)
    public long getUserStorageUsed(User user) {
        Long total = fileAttachmentRepository.getTotalStorageUsedByUser(user);
        return total != null ? total : 0;
    }

    @Transactional(readOnly = true)
    public long getUserFileCount(User user) {
        Long count = fileAttachmentRepository.countByUser(user);
        return count != null ? count : 0;
    }
}
