package com.lazycord.controller;

import com.lazycord.model.ShopItem;
import com.lazycord.model.User;
import com.lazycord.model.UserInventory;
import com.lazycord.service.ShopService;
import com.lazycord.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:1420", "tauri://localhost"})
public class ShopController {

    private final ShopService shopService;
    private final UserService userService;

    @GetMapping("/items")
    public ResponseEntity<List<ShopItem>> getAvailableItems(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(shopService.getAvailableItems(user));
    }

    @GetMapping("/items/type/{type}")
    public ResponseEntity<List<ShopItem>> getItemsByType(@PathVariable ShopItem.ItemType type) {
        return ResponseEntity.ok(shopService.getItemsByType(type));
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<UserInventory>> getMyInventory(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(shopService.getUserInventory(user));
    }

    @GetMapping("/inventory/equipped")
    public ResponseEntity<List<UserInventory>> getEquippedItems(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(shopService.getUserEquippedItems(user));
    }

    @PostMapping("/items/{itemId}/purchase")
    public ResponseEntity<Void> purchaseItem(@PathVariable UUID itemId, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        shopService.purchaseItem(user, itemId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/items/{itemId}/equip")
    public ResponseEntity<Void> equipItem(@PathVariable UUID itemId, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        shopService.equipItem(user, itemId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/items/{itemId}/unequip")
    public ResponseEntity<Void> unequipItem(@PathVariable UUID itemId, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        shopService.unequipItem(user, itemId);
        return ResponseEntity.ok().build();
    }
}
