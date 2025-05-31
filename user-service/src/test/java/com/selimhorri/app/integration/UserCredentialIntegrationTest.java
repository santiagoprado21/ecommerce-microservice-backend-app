package com.selimhorri.app.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.service.CredentialService;
import com.selimhorri.app.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserCredentialIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private CredentialService credentialService;

    @Test
    void whenCreateUserWithCredential_ThenBothShouldBePersisted() {
        // Arrange
        CredentialDto credentialDto = CredentialDto.builder()
                .username("integrationtest")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        UserDto userDto = UserDto.builder()
                .firstName("Integration")
                .lastName("Test")
                .email("integration@test.com")
                .phone("1234567890")
                .imageUrl("http://example.com/integration.jpg")
                .credentialDto(credentialDto)
                .build();

        // Act
        UserDto savedUser = userService.save(userDto);

        // Assert
        assertNotNull(savedUser.getUserId());
        assertNotNull(savedUser.getCredentialDto().getCredentialId());
        
        // Verify user can be retrieved
        UserDto retrievedUser = userService.findById(savedUser.getUserId());
        assertEquals(userDto.getEmail(), retrievedUser.getEmail());
        
        // Verify credential can be retrieved
        CredentialDto retrievedCredential = credentialService.findByUsername("integrationtest");
        assertEquals(credentialDto.getUsername(), retrievedCredential.getUsername());
        assertEquals(savedUser.getUserId(), retrievedCredential.getUserDto().getUserId());
    }

    @Test
    void whenUpdateUserCredential_ThenBothShouldBeUpdated() {
        // Arrange - Create initial user with credential
        CredentialDto credentialDto = CredentialDto.builder()
                .username("updatetest")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        UserDto userDto = UserDto.builder()
                .firstName("Update")
                .lastName("Test")
                .email("update@test.com")
                .phone("1234567890")
                .imageUrl("http://example.com/update.jpg")
                .credentialDto(credentialDto)
                .build();

        UserDto savedUser = userService.save(userDto);

        // Act - Update user and credential
        savedUser.setFirstName("Updated");
        savedUser.getCredentialDto().setPassword("newpassword123");
        UserDto updatedUser = userService.update(savedUser);

        // Assert
        assertEquals("Updated", updatedUser.getFirstName());
        
        // Verify credential was updated
        CredentialDto updatedCredential = credentialService.findById(updatedUser.getCredentialDto().getCredentialId());
        assertEquals("newpassword123", updatedCredential.getPassword());
    }

    @Test
    void whenFindUserByUsername_ThenShouldReturnUserWithCredential() {
        // Arrange
        CredentialDto credentialDto = CredentialDto.builder()
                .username("findbyusername")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        UserDto userDto = UserDto.builder()
                .firstName("Find")
                .lastName("ByUsername")
                .email("find@test.com")
                .phone("1234567890")
                .imageUrl("http://example.com/find.jpg")
                .credentialDto(credentialDto)
                .build();

        userService.save(userDto);

        // Act
        UserDto foundUser = userService.findByUsername("findbyusername");

        // Assert
        assertNotNull(foundUser);
        assertEquals("Find", foundUser.getFirstName());
        assertEquals("findbyusername", foundUser.getCredentialDto().getUsername());
    }
} 