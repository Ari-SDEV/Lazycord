package com.lazycord.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_item_id", nullable = false)
    private ShopItem shopItem;

    @Column(nullable = false)
    private boolean equipped = false;

    @CreationTimestamp
    private LocalDateTime acquiredAt;
}
