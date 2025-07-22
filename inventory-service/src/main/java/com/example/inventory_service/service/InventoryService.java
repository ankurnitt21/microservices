package com.example.inventory_service.service;

import com.example.inventory_service.model.Inventory;
import com.example.inventory_service.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Inventory> findAllInventories() {
        return inventoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean isInStock(String sku) {
        return inventoryRepository
                .findBySku(sku)
                .map(inventory -> inventory.getQuantity() > 0)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Integer getStockQuantity(String sku) {
        return inventoryRepository
                .findBySku(sku)
                .map(Inventory::getQuantity)
                .orElse(0);
    }

    @Transactional
    public Inventory addInventory(String sku, Integer initialQuantity) {
        Inventory newInventory = new Inventory();
        newInventory.setSku(sku);
        newInventory.setQuantity(initialQuantity);
        return inventoryRepository.save(newInventory);
    }
}