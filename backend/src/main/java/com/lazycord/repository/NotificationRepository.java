package com.lazycord.repository;

import com.lazycord.model.Notification;
import com.lazycord.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Notification> findByUserAndReadFalseOrderByCreatedAtDesc(User user);

    long countByUserAndReadFalse(User user);

    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.read = false AND n.createdAt > :since")
    List<Notification> findUnreadSince(@Param("user") User user, @Param("since") LocalDateTime since);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user = :user AND n.read = false")
    void markAllAsRead(@Param("user") User user);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.id = :id")
    void markAsRead(@Param("id") UUID id);

    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.type = :type AND n.read = false")
    List<Notification> findUnreadByType(@Param("user") User user, @Param("type") Notification.NotificationType type);
}
