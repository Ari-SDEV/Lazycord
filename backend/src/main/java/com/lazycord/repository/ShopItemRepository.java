package com.lazycord.repository;

import com.lazycord.model.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShopItemRepository extends JpaRepository<ShopItem, UUID> {

    List<ShopItem> findByActiveTrue();

    List<ShopItem> findByTypeAndActiveTrue(ShopItem.ItemType type);

    List<ShopItem> findByLevelRequiredLessThanEqualAndActiveTrue(Integer level);
}
