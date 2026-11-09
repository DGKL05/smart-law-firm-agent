package com.example.dgkl.lawyer.controller;

import com.example.dgkl.lawyer.service.LawyerService;
import com.example.dgkl.lawyer.service.PublicLawyerService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicHomeStatsTest {
    @Test
    void returnsDirectoryCountsAtPublicHomepageEndpoint() throws Exception {
        LawyerService lawyerService = mock(LawyerService.class);
        when(lawyerService.stats()).thenReturn(Map.of("lawFirmCount", 4L, "lawyerCount", 6L));
        PublicLawyerController controller = new PublicLawyerController(mock(PublicLawyerService.class), lawyerService);

        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(get("/api/public/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lawFirmCount").value(4))
                .andExpect(jsonPath("$.data.lawyerCount").value(6));
    }
}
