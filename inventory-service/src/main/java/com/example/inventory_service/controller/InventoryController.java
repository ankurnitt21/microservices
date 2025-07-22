package com.example.inventory_service.controller;

import com.example.inventory_service.model.Inventory;
import com.example.inventory_service.service.InventoryService;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Inventory> getAllInventories() {
        return inventoryService.findAllInventories();
    }

    @GetMapping("/{sku}")
    @ResponseStatus(HttpStatus.OK)
    public boolean isInStock(@PathVariable String sku) {
        return inventoryService.isInStock(sku);
    }

    @GetMapping("/quantity/{sku}")
    public ResponseEntity<Map<String, Object>> getStockQuantity(
            @PathVariable String sku
    ) {
        Integer quantity = inventoryService.getStockQuantity(sku);
        Map<String, Object> response = Map.of("sku", sku, "quantity", quantity);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Inventory> addInventory(
            @RequestBody Map<String, Object> payload
    ) {
        String sku = (String) payload.get("sku");
        Integer quantity = (Integer) payload.get("quantity");

        if (sku == null || quantity == null) {
            return ResponseEntity.badRequest().build();
        }

        Inventory savedInventory = inventoryService.addInventory(sku, quantity);
        return new ResponseEntity<>(savedInventory, HttpStatus.CREATED);
    }
}