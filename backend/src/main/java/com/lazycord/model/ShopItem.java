package com.lazycord.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shop_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    @Column(nullable = false)
    private int price;

    @Column
    private String imageUrl;

    @Column
    private Integer levelRequired;

    @Column(nullable = false)
    private boolean active = true;

    @Column
    private Integer stockLimit;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ItemType {
        AVATAR_FRAME,
        BADGE,
        THEME,
        TITLE,
        EMOTE,
        BOOST
    }
}
