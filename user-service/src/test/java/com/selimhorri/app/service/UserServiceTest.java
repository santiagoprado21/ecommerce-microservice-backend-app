package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        // Setup test user
        Credential credential = Credential.builder()
                .credentialId(1)
                .username("testuser")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        testUser = User.builder()
                .userId(1)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .phone("1234567890")
                .imageUrl("http://example.com/image.jpg")
                .credential(credential)
                .build();

        credential.setUser(testUser);

        // Setup test user DTO
        CredentialDto credentialDto = CredentialDto.builder()
                .credentialId(1)
                .username("testuser")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        testUserDto = UserDto.builder()
                .userId(1)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .phone("1234567890")
                .imageUrl("http://example.com/image.jpg")
                .credentialDto(credentialDto)
                .build();
    }

    @Test
    void findAll_ShouldReturnListOfUsers() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser));

        // Act
        List<UserDto> result = userService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserDto.getUserId(), result.get(0).getUserId());
        assertEquals(testUserDto.getEmail(), result.get(0).getEmail());
        verify(userRepository).findAll();
    }

    @Test
    void findById_WithValidId_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // Act
        UserDto result = userService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals(testUserDto.getUserId(), result.getUserId());
        assertEquals(testUserDto.getEmail(), result.getEmail());
        verify(userRepository).findById(1);
    }

    @Test
    void findById_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserObjectNotFoundException.class, () -> userService.findById(999));
        verify(userRepository).findById(999);
    }

    @Test
    void save_ShouldReturnSavedUser() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserDto result = userService.save(testUserDto);

        // Assert
        assertNotNull(result);
        assertEquals(testUserDto.getUserId(), result.getUserId());
        assertEquals(testUserDto.getEmail(), result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findByUsername_WithValidUsername_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByCredentialUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDto result = userService.findByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals(testUserDto.getUserId(), result.getUserId());
        assertEquals(testUserDto.getCredentialDto().getUsername(), "testuser");
        verify(userRepository).findByCredentialUsername("testuser");
    }
} 