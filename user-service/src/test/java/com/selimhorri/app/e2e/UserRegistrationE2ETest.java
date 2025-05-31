package com.selimhorri.app.e2e;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserRegistrationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Test
    void fullUserRegistrationFlow() throws Exception {
        // Create user registration request
        CredentialDto credentialDto = CredentialDto.builder()
                .username("e2etest")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        UserDto userDto = UserDto.builder()
                .firstName("E2E")
                .lastName("Test")
                .email("e2e@test.com")
                .phone("1234567890")
                .imageUrl("http://example.com/e2e.jpg")
                .credentialDto(credentialDto)
                .build();

        // Register new user
        MvcResult registrationResult = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("e2e@test.com"))
                .andReturn();

        // Extract registered user details
        UserDto registeredUser = objectMapper.readValue(
                registrationResult.getResponse().getContentAsString(),
                UserDto.class);

        // Verify user can be retrieved by ID
        mockMvc.perform(get("/api/users/{userId}", registeredUser.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(registeredUser.getUserId()))
                .andExpect(jsonPath("$.email").value("e2e@test.com"));

        // Verify user can be retrieved by username
        mockMvc.perform(get("/api/users/username/{username}", "e2etest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential.username").value("e2etest"));

        // Update user information
        registeredUser.setFirstName("UpdatedE2E");
        mockMvc.perform(put("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registeredUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("UpdatedE2E"));

        // Delete user
        mockMvc.perform(delete("/api/users/{userId}", registeredUser.getUserId()))
                .andExpect(status().isOk());

        // Verify user no longer exists
        mockMvc.perform(get("/api/users/{userId}", registeredUser.getUserId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void userRegistrationWithInvalidData() throws Exception {
        // Test registration with missing required fields
        UserDto invalidUser = UserDto.builder()
                .firstName("Invalid")
                .build();

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());

        // Test registration with invalid email
        UserDto userWithInvalidEmail = UserDto.builder()
                .firstName("Test")
                .lastName("User")
                .email("invalid-email")
                .credentialDto(CredentialDto.builder()
                        .username("testuser")
                        .password("password123")
                        .build())
                .build();

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userWithInvalidEmail)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userAuthenticationFlow() throws Exception {
        // Create and register a user
        CredentialDto credentialDto = CredentialDto.builder()
                .username("authtest")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        UserDto userDto = UserDto.builder()
                .firstName("Auth")
                .lastName("Test")
                .email("auth@test.com")
                .phone("1234567890")
                .imageUrl("http://example.com/auth.jpg")
                .credentialDto(credentialDto)
                .build();

        UserDto registeredUser = userService.save(userDto);

        // Verify authentication with valid credentials
        mockMvc.perform(get("/api/users/username/{username}", "authtest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential.username").value("authtest"))
                .andExpect(jsonPath("$.credential.isEnabled").value(true));

        // Verify authentication fails with invalid username
        mockMvc.perform(get("/api/users/username/{username}", "nonexistent"))
                .andExpect(status().isNotFound());
    }
} 