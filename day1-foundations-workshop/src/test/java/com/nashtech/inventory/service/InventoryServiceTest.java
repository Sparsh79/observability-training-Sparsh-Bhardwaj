package com.nashtech.inventory.service;

import com.nashtech.inventory.model.InventoryItem;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceTest {

    private InventoryService inventoryService;
    private MeterRegistry meterRegistry;
    private ObservationRegistry observationRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        observationRegistry = ObservationRegistry.create();
        inventoryService = new InventoryService(meterRegistry, observationRegistry);
    }

    @Test
    void getItem_ValidItemId_ReturnsItem() {
        InventoryItem item = null;
        int attempts = 0;
        int maxAttempts = 20;

        while (item == null && attempts < maxAttempts) {
            try {
                item = inventoryService.getItem("ITEM001");
            } catch (RuntimeException e) {
                // Retry on simulated error
                attempts++;
            }
        }

        assertNotNull(item, "Should eventually get a successful response");
        assertEquals("ITEM001", item.getItemId());
        assertEquals("Laptop", item.getName());
        assertEquals(50, item.getQuantity());
        assertEquals(999.99, item.getPrice());
    }

    @Test
    void getItem_InvalidItemId_ThrowsException() {
        assertThrows(Exception.class, () -> {
            inventoryService.getItem("INVALID");
        });
    }

    @Test
    void getItem_Success_IncrementsSuccessCounter() {
        // Get the initial count
        double initialSuccessCount = getCounterValue("success");

        int attempts = 0;
        int maxAttempts = 20;
        boolean success = false;

        while (!success && attempts < maxAttempts) {
            try {
                inventoryService.getItem("ITEM001");
                success = true;
            } catch (RuntimeException e) {
                // Retry on simulated error
                attempts++;
            }
        }

        assertTrue(success, "Should eventually get a successful response");

        double finalSuccessCount = getCounterValue("success");
        assertTrue(finalSuccessCount > initialSuccessCount,
                "Success counter should be incremented");
    }

    @Test
    void getItem_NotFound_IncrementsErrorCounter() {
        double initialErrorCount = getCounterValue("error");

        // Make a request that will fail
        try {
            inventoryService.getItem("INVALID");
        } catch (Exception e) {
            // Expected exception - could be IllegalArgumentException or RuntimeException
        }

        // Verify the counter was incremented
        double finalErrorCount = getCounterValue("error");
        assertTrue(finalErrorCount > initialErrorCount,
                "Error counter should be incremented");
    }

    @Test
    void getItem_MultipleRequests_IncrementsCountersCorrectly() {
        double initialSuccessCount = getCounterValue("success");
        double initialErrorCount = getCounterValue("error");

        int successCount = 0;
        while (successCount < 2) {
            try {
                inventoryService.getItem(successCount == 0 ? "ITEM001" : "ITEM002");
                successCount++;
            } catch (RuntimeException e) {
                // Retry on simulated error
            }
        }

        try {
            inventoryService.getItem("INVALID1");
        } catch (Exception e) {
            // Expected - could be IllegalArgumentException or RuntimeException
        }
        try {
            inventoryService.getItem("INVALID2");
        } catch (Exception e) {
            // Expected - could be IllegalArgumentException or RuntimeException
        }

        double finalSuccessCount = getCounterValue("success");
        double finalErrorCount = getCounterValue("error");

        assertEquals(initialSuccessCount + 2, finalSuccessCount,
                "Success counter should be incremented by 2");
        assertEquals(initialErrorCount + 2, finalErrorCount,
                "Error counter should be incremented by 2");
    }

    private double getCounterValue(String status) {
        Counter counter = meterRegistry.find("inventory.requests.total")
                .tag("status", status)
                .counter();
        return counter != null ? counter.count() : 0.0;
    }
}
