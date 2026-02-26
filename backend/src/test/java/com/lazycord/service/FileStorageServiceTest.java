package com.lazycord.service;

import com.lazycord.model.FileAttachment;
import com.lazycord.model.User;
import com.lazycord.repository.FileAttachmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileAttachmentServiceTest {

    @Mock
    private FileAttachmentRepository fileAttachmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FileAttachmentService fileAttachmentService;

    private User testUser;
    private MultipartFile testFile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        testFile = new MockMultipartFile(
                "test.jpg",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
    }

    @Test
    void uploadFile_NewFile_Success() throws Exception {
        // Arrange
        when(fileAttachmentRepository.findByFileHashAndDeletedFalse(any())).thenReturn(Optional.empty());
        when(fileStorageService.storeFile(any(), any())).thenReturn("/uploads/test_hash_test.jpg");
        when(fileAttachmentRepository.save(any(FileAttachment.class))).thenAnswer(invocation -> {
            FileAttachment attachment = invocation.getArgument(0);
            attachment.setId(UUID.randomUUID());
            return attachment;
        });

        // Act
        FileAttachment result = fileAttachmentService.uploadFile(testFile, testUser, null);

        // Assert
        assertNotNull(result);
        assertEquals("test.jpg", result.getOriginalName());
        assertEquals("image/jpeg", result.getMimeType());
        verify(fileAttachmentRepository, times(1)).save(any(FileAttachment.class));
    }

    @Test
    void uploadFile_DuplicateFile_ReturnsExisting() throws Exception {
        // Arrange
        FileAttachment existingAttachment = new FileAttachment();
        existingAttachment.setId(UUID.randomUUID());
        existingAttachment.setOriginalName("existing.jpg");
        when(fileAttachmentRepository.findByFileHashAndDeletedFalse(any())).thenReturn(Optional.of(existingAttachment));

        // Act
        FileAttachment result = fileAttachmentService.uploadFile(testFile, testUser, null);

        // Assert
        assertNotNull(result);
        assertEquals("existing.jpg", result.getOriginalName());
        verify(fileAttachmentRepository, never()).save(any(FileAttachment.class));
    }

    @Test
    void getFile_Exists_ReturnsFile() {
        // Arrange
        UUID fileId = UUID.randomUUID();
        FileAttachment attachment = new FileAttachment();
        attachment.setId(fileId);
        attachment.setDeleted(false);
        when(fileAttachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));

        // Act
        FileAttachment result = fileAttachmentService.getFile(fileId);

        // Assert
        assertNotNull(result);
        assertEquals(fileId, result.getId());
    }

    @Test
    void getFile_NotExists_ThrowsException() {
        // Arrange
        UUID fileId = UUID.randomUUID();
        when(fileAttachmentRepository.findById(fileId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            fileAttachmentService.getFile(fileId);
        });
    }

    @Test
    void deleteFile_Success() {
        // Arrange
        UUID fileId = UUID.randomUUID();
        FileAttachment attachment = new FileAttachment();
        attachment.setId(fileId);
        attachment.setUploadedBy(testUser);
        attachment.setStoragePath("/uploads/test.jpg");
        when(fileAttachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));
        when(fileAttachmentRepository.save(any(FileAttachment.class))).thenReturn(attachment);

        // Act
        fileAttachmentService.deleteFile(fileId, testUser);

        // Assert
        assertTrue(attachment.isDeleted());
        verify(fileStorageService, times(1)).deleteFile(anyString());
    }

    @Test
    void deleteFile_NotOwner_ThrowsException() {
        // Arrange
        UUID fileId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");

        FileAttachment attachment = new FileAttachment();
        attachment.setId(fileId);
        attachment.setUploadedBy(otherUser);
        when(fileAttachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            fileAttachmentService.deleteFile(fileId, testUser);
        });
    }
}
