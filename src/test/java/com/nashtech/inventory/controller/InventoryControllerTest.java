package com.nashtech.inventory.controller;

import com.nashtech.inventory.model.InventoryItem;
import com.nashtech.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    private InventoryItem testItem;

    @BeforeEach
    void setUp() {
        testItem = new InventoryItem("ITEM001", "Laptop", 50, 999.99);
    }

    @Test
    void getItem_Success_ReturnsItem() throws Exception {
        when(inventoryService.getItem("ITEM001")).thenReturn(testItem);

        mockMvc.perform(get("/api/inventory/items/ITEM001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.itemId", is("ITEM001")))
                .andExpect(jsonPath("$.name", is("Laptop")))
                .andExpect(jsonPath("$.quantity", is(50)))
                .andExpect(jsonPath("$.price", is(999.99)));

        verify(inventoryService, times(1)).getItem("ITEM001");
    }

    @Test
    void getItem_NotFound_Returns404() throws Exception {
        when(inventoryService.getItem("INVALID"))
                .thenThrow(new IllegalArgumentException("Item not found: INVALID"));

        mockMvc.perform(get("/api/inventory/items/INVALID")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(inventoryService, times(1)).getItem("INVALID");
    }

    @Test
    void getItem_ServerError_Returns500() throws Exception {
        when(inventoryService.getItem("ITEM001"))
                .thenThrow(new RuntimeException("Simulated database connection failure"));

        mockMvc.perform(get("/api/inventory/items/ITEM001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(inventoryService, times(1)).getItem("ITEM001");
    }

    @Test
    void healthCheck_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/inventory/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("inventory-service")));
    }
}
