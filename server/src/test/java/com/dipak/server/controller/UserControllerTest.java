package com.dipak.server.controller;

import com.dipak.server.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void registerAndLogin() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setName("Frank Test");
        reg.setEmail("frank@usertest.com");
        reg.setPassword("frank_password");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();

        if (result.getResponse().getStatus() == 409) {
            String loginBody = "{\"email\":\"frank@usertest.com\",\"password\":\"frank_password\"}";
            result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody))
                    .andReturn();
        }

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        accessToken = json.get("accessToken").asText();
    }

    @Test
    void getMeReturnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("frank@usertest.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchUsers() throws Exception {
        RegisterRequest reg2 = new RegisterRequest();
        reg2.setName("Grace Searchable");
        reg2.setEmail("grace@usertest.com");
        reg2.setPassword("grace_password");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg2)))
                .andReturn();

        mockMvc.perform(get("/api/users/search")
                        .param("q", "grac")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void searchWithQueryTooShortReturns400() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .param("q", "a")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }
}
