package com.lazycord.repository;

import com.lazycord.model.Channel;
import com.lazycord.model.FileAttachment;
import com.lazycord.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, UUID> {

    List<FileAttachment> findByChannelAndDeletedFalse(Channel channel);

    List<FileAttachment> findByUploadedByAndDeletedFalse(User user);

    Optional<FileAttachment> findByFileHashAndDeletedFalse(String fileHash);

    boolean existsByFileHashAndDeletedFalse(String fileHash);

    @Query("SELECT SUM(f.size) FROM FileAttachment f WHERE f.uploadedBy = :user AND f.deleted = false")
    Long getTotalStorageUsedByUser(@Param("user") User user);

    @Query("SELECT COUNT(f) FROM FileAttachment f WHERE f.uploadedBy = :user AND f.deleted = false")
    Long countByUser(@Param("user") User user);

    List<FileAttachment> findByDeletedFalseOrderByCreatedAtDesc();
}
