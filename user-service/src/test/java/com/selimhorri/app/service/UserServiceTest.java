package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
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

    // ===== NEW COMPREHENSIVE UNIT TESTS =====

    /**
     * NEW TEST 1: Verify user profile update with partial data
     */
    @Test
    void update_WithPartialData_ShouldUpdateOnlyProvidedFields() {
        // Arrange
        UserDto partialUpdateDto = UserDto.builder()
                .userId(1)
                .firstName("UpdatedFirstName")
                .phone("9999999999")
                .build();

        User existingUser = User.builder()
                .userId(1)
                .firstName("OldFirstName")
                .lastName("OriginalLastName")
                .email("original@example.com")
                .phone("1111111111")
                .build();

        User updatedUser = User.builder()
                .userId(1)
                .firstName("UpdatedFirstName")
                .lastName("OriginalLastName")
                .email("original@example.com")
                .phone("9999999999")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.update(partialUpdateDto);

        // Assert
        assertNotNull(result);
        assertEquals("UpdatedFirstName", result.getFirstName());
        assertEquals("OriginalLastName", result.getLastName()); // Should remain unchanged
        assertEquals("9999999999", result.getPhone());
        verify(userRepository).findById(1);
        verify(userRepository).save(any(User.class));
    }

    /**
     * NEW TEST 2: Verify delete by ID functionality
     */
    @Test
    void deleteById_WithValidId_ShouldDeleteUser() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).deleteById(1);

        // Act
        userService.deleteById(1);

        // Assert
        verify(userRepository).findById(1);
        verify(userRepository).deleteById(1);
    }

    /**
     * NEW TEST 3: Verify delete by ID with non-existent user
     */
    @Test
    void deleteById_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserObjectNotFoundException.class, () -> {
            userService.deleteById(999);
        });
        verify(userRepository).findById(999);
        verify(userRepository, never()).deleteById(999);
    }

    /**
     * NEW TEST 4: Verify username existence check
     */
    @Test
    void findByUsername_WithNonExistentUsername_ShouldThrowException() {
        // Arrange
        String nonExistentUsername = "nonexistent";
        when(userRepository.findByCredentialUsername(nonExistentUsername)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserObjectNotFoundException.class, () -> {
            userService.findByUsername(nonExistentUsername);
        });
        verify(userRepository).findByCredentialUsername(nonExistentUsername);
    }

    /**
     * NEW TEST 5: Verify save with null credential
     */
    @Test
    void save_WithNullCredential_ShouldHandleGracefully() {
        // Arrange
        UserDto userDtoWithoutCredential = UserDto.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .phone("1234567890")
                .credentialDto(null)
                .build();

        User userWithoutCredential = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .phone("1234567890")
                .credential(null)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(userWithoutCredential);

        // Act
        UserDto result = userService.save(userDtoWithoutCredential);

        // Assert
        assertNotNull(result);
        assertEquals("Test", result.getFirstName());
        assertNull(result.getCredentialDto());
        verify(userRepository).save(any(User.class));
    }

    /**
     * NEW TEST 6: Verify large dataset handling
     */
    @Test
    void findAll_WithLargeDataset_ShouldReturnAllUsers() {
        // Arrange
        List<User> largeUserList = Arrays.asList(
                testUser,
                User.builder().userId(2).firstName("User2").lastName("Last2").email("user2@test.com").build(),
                User.builder().userId(3).firstName("User3").lastName("Last3").email("user3@test.com").build(),
                User.builder().userId(4).firstName("User4").lastName("Last4").email("user4@test.com").build(),
                User.builder().userId(5).firstName("User5").lastName("Last5").email("user5@test.com").build()
        );

        when(userRepository.findAll()).thenReturn(largeUserList);

        // Act
        List<UserDto> result = userService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("User2", result.get(1).getFirstName());
        assertEquals("User5", result.get(4).getFirstName());
        verify(userRepository).findAll();
    }

    /**
     * NEW TEST 7: Verify error handling with repository exception
     */
    @Test
    void save_WithRepositoryException_ShouldPropagateException() {
        // Arrange
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.save(testUserDto);
        });

        assertEquals("Database connection failed", exception.getMessage());
        verify(userRepository).save(any(User.class));
    }
} 