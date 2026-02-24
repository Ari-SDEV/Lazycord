package com.lazycord.repository;

import com.lazycord.model.Community;
import com.lazycord.model.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShopItemRepository extends JpaRepository<ShopItem, UUID> {

    List<ShopItem> findByCommunityAndActiveTrue(Community community);

    List<ShopItem> findByCommunityAndTypeAndActiveTrue(Community community, ShopItem.ItemType type);

    List<ShopItem> findByCommunityAndLevelRequiredLessThanEqualAndActiveTrue(Community community, Integer level);

    // Legacy method
    List<ShopItem> findByActiveTrue();
}
