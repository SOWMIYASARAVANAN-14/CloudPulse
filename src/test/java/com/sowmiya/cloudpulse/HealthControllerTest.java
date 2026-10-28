package com.sowmiya.cloudpulse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "STRESS_ENABLED=true",
    "STRESS_KEY=mock-test-secret-key-12345"
})
class HealthControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void rootEndpointForwardsToLandingPage() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void indexHtmlReturnsLandingPage() throws Exception {
        mvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(org.springframework.http.MediaType.TEXT_HTML))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("CloudPulse")));
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("CloudPulse"))
            .andExpect(jsonPath("$.version").value("1.1.0"));
    }

    @Test
    void infoEndpointReturnsProjectName() throws Exception {
        mvc.perform(get("/api/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("CloudPulse"))
            .andExpect(jsonPath("$.version").value("1.1.0"));
    }

    @Test
    void stressEndpointRejectsUnauthorizedAccess() throws Exception {
        mvc.perform(get("/api/stress"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void stressEndpointRejectsUrlQueryParam() throws Exception {
        // Query parameter is NOT accepted - should return 401
        mvc.perform(get("/api/stress").param("key", "mock-test-secret-key-12345"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void stressEndpointAcceptsValidHeader() throws Exception {
        mvc.perform(get("/api/stress")
                .header("X-Stress-Key", "mock-test-secret-key-12345")
                .param("duration", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Stress initiated"));
    }
}
