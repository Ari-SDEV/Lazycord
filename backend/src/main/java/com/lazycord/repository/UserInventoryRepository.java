package com.lazycord.repository;

import com.lazycord.model.ShopItem;
import com.lazycord.model.User;
import com.lazycord.model.UserInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInventoryRepository extends JpaRepository<UserInventory, Long> {

    List<UserInventory> findByUser(User user);

    List<UserInventory> findByUserAndEquippedTrue(User user);

    Optional<UserInventory> findByUserAndShopItem(User user, ShopItem shopItem);

    boolean existsByUserAndShopItem(User user, ShopItem shopItem);
}
