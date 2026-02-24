package com.lazycord.service;

import com.lazycord.model.ShopItem;
import com.lazycord.model.User;
import com.lazycord.model.UserInventory;
import com.lazycord.repository.ShopItemRepository;
import com.lazycord.repository.UserInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopItemRepository shopItemRepository;
    private final UserInventoryRepository userInventoryRepository;
    private final GamificationService gamificationService;

    @Transactional(readOnly = true)
    public List<ShopItem> getAvailableItems(User user, Community community) {
        return shopItemRepository.findByCommunityAndLevelRequiredLessThanEqualAndActiveTrue(community, user.getLevel());
    }

    @Transactional(readOnly = true)
    public List<ShopItem> getItemsByType(Community community, ShopItem.ItemType type) {
        return shopItemRepository.findByCommunityAndTypeAndActiveTrue(community, type);
    }

    @Transactional(readOnly = true)
    public List<UserInventory> getUserInventory(User user) {
        return userInventoryRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<UserInventory> getUserEquippedItems(User user) {
        return userInventoryRepository.findByUserAndEquippedTrue(user);
    }

    @Transactional
    public void purchaseItem(User user, UUID itemId) {
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.isActive()) {
            throw new RuntimeException("Item not available");
        }

        if (item.getLevelRequired() != null && user.getLevel() < item.getLevelRequired()) {
            throw new RuntimeException("Level requirement not met");
        }

        if (userInventoryRepository.existsByUserAndShopItem(user, item)) {
            throw new RuntimeException("Item already owned");
        }

        gamificationService.deductPoints(user, item.getPrice());

        UserInventory inventory = new UserInventory();
        inventory.setUser(user);
        inventory.setShopItem(item);
        inventory.setEquipped(false);

        userInventoryRepository.save(inventory);
        log.info("User {} purchased item {}", user.getUsername(), item.getName());
    }

    @Transactional
    public void equipItem(User user, UUID itemId) {
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        UserInventory inventory = userInventoryRepository.findByUserAndShopItem(user, item)
                .orElseThrow(() -> new RuntimeException("Item not in inventory"));

        // Unequip other items of same type
        List<UserInventory> equippedItems = userInventoryRepository.findByUserAndEquippedTrue(user);
        for (UserInventory equipped : equippedItems) {
            if (equipped.getShopItem().getType() == item.getType()) {
                equipped.setEquipped(false);
                userInventoryRepository.save(equipped);
            }
        }

        inventory.setEquipped(true);
        userInventoryRepository.save(inventory);
        log.info("User {} equipped item {}", user.getUsername(), item.getName());
    }

    @Transactional
    public void unequipItem(User user, UUID itemId) {
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        UserInventory inventory = userInventoryRepository.findByUserAndShopItem(user, item)
                .orElseThrow(() -> new RuntimeException("Item not in inventory"));

        inventory.setEquipped(false);
        userInventoryRepository.save(inventory);
        log.info("User {} unequipped item {}", user.getUsername(), item.getName());
    }
}
