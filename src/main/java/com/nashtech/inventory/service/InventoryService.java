package com.nashtech.inventory.service;

import com.nashtech.inventory.model.InventoryItem;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    private final Map<String, InventoryItem> inventory;
    private final Random random;
    private final MeterRegistry meterRegistry;

    public InventoryService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.random = new Random();
        this.inventory = new HashMap<>();
        initializeInventory();
    }

    private void initializeInventory() {
        inventory.put("ITEM001", new InventoryItem("ITEM001", "Laptop", 50, 999.99));
        inventory.put("ITEM002", new InventoryItem("ITEM002", "Mouse", 200, 29.99));
        inventory.put("ITEM003", new InventoryItem("ITEM003", "Keyboard", 150, 79.99));
        inventory.put("ITEM004", new InventoryItem("ITEM004", "Monitor", 75, 299.99));
        inventory.put("ITEM005", new InventoryItem("ITEM005", "Headphones", 100, 149.99));
        logger.info("Inventory initialized with {} items", inventory.size());
    }

    public InventoryItem getItem(String itemId) {
        logger.debug("Fetching inventory item with ID: {}", itemId);

        // Simulate random processing delay (20% chance)
        if (random.nextInt(100) < 20) {
            int delay = random.nextInt(2000) + 500; // 500-2500ms delay
            logger.info("Simulating processing delay of {}ms for itemId: {}", delay, itemId);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Thread interrupted during simulated delay", e);
            }
        }

        // Simulate random exception (15% chance)
        if (random.nextInt(100) < 15) {
            logger.error("Simulated error occurred while fetching itemId: {}", itemId);
            incrementRequestCounter("error");
            throw new RuntimeException("Simulated database connection failure for item: " + itemId);
        }

        InventoryItem item = inventory.get(itemId);

        if (item == null) {
            logger.warn("Item not found with ID: {}", itemId);
            incrementRequestCounter("error");
            throw new IllegalArgumentException("Item not found: " + itemId);
        }

        logger.info("Successfully retrieved item: {} with quantity: {}", item.getName(), item.getQuantity());
        incrementRequestCounter("success");
        return item;
    }

    private void incrementRequestCounter(String status) {
        Counter.builder("inventory.requests.total")
                .description("Total number of inventory requests")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
        logger.debug("Incremented request counter with status: {}", status);
    }
}
