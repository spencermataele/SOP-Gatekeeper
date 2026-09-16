package com.woven.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woven.app.domain.Role;
import com.woven.app.domain.User;
import com.woven.app.repository.UserRepository;
import com.woven.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class AuthenticationApiTest extends DatabaseTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @BeforeEach
    void authorFixture() {
        User author = new User();
        author.setUsername("test.author");
        author.setEmail("test.author@example.test");
        author.setFullName("Test Author");
        author.setRole(Role.USER);
        author.setPassword(passwords.encode("test-password-only"));
        users.saveAndFlush(author);
    }

    @Test
    void anonymousUserCannotReadSops() throws Exception {
        mvc.perform(get("/sops")).andExpect(status().isForbidden());
    }

    @Test
    void authenticatedAuthorCanRetrieveTheirIdentityAndPublishedSops() throws Exception {
        var result = mvc.perform(post("/auth/login").contentType("application/json")
                        .content(json.writeValueAsString(Map.of(
                                "username", "test.author", "password", "test-password-only"))))
                .andExpect(status().isOk()).andExpect(jsonPath("token").isNotEmpty())
                .andReturn();
        String token = json.readTree(result.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("username").value("test.author"));
        mvc.perform(get("/sops").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    void wrongPasswordDoesNotIssueAToken() throws Exception {
        mvc.perform(post("/auth/login").contentType("application/json")
                        .content(json.writeValueAsString(Map.of(
                                "username", "test.author", "password", "wrong-password"))))
                .andExpect(status().is4xxClientError()).andExpect(jsonPath("token").doesNotExist());
    }
}
