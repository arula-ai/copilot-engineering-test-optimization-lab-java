package com.example.lab.service;

import com.example.lab.model.dto.CreateUserRequest;
import com.example.lab.model.entity.User;
import com.example.lab.model.enums.UserStatus;
import com.example.lab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // REDUNDANT: This setup is repeated in each test instead of using @BeforeEach properly
    private void setupMocks() {
        // Duplicate setup logic
    }

    @BeforeEach
    void setUp() {
        setupMocks();
    }

    @ParameterizedTest
    @CsvSource({
        "test@example.com, true",
        "user.name@domain.org, true",
        "a+b@test.co, true",
        "invalidemail, false",
        "test@, false",
        "@domain.com, false"
    })
    void shouldValidateEmail(String email, boolean expected) {
        assertEquals(expected, userService.isValidEmail(email));
    }

    @ParameterizedTest
    @CsvSource({
        "Short1!, false, '8 characters',",
        "lowercase1!, false, 'uppercase',",
        "UPPERCASE1!, false, 'lowercase',",
        "NoNumber!, false, 'number',",
        "NoSpecial1, false, 'special',",
        "ValidPass1!, true, ''"
    })
    void shouldValidatePasswords(String password, boolean expectedValid, String expectedErrorFragment) {
        var result = userService.validatePassword(password);
        assertEquals(expectedValid, result.valid());
        if (!expectedValid) {
            assertNotNull(result.error());
            assertTrue(result.error().contains(expectedErrorFragment));
        } else {
            assertNull(result.error());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "1234567890, true",
        "123-456-7890, true",
        "+1 234 567 8900, true",
        "12345, false"
    })
    void shouldValidatePhoneNumbers(String phone, boolean expected) {
        assertEquals(expected, userService.isValidPhone(phone));
    }

    @Test
    void shouldCreateUserSuccessfully() {
        CreateUserRequest request = CreateUserRequest.builder()
            .email("new@example.com")
            .password("ValidPass1!")
            .firstName("New")
            .lastName("User")
            .build();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-123");
            return user;
        });

        User result = userService.createUser(request);

        assertNotNull(result);
        assertEquals("new@example.com", result.getEmail());
    }

    // MISSING TESTS:
    // - createUser with invalid email
    // - createUser with weak password
    // - createUser with short first name
    // - createUser with short last name
    // - createUser with duplicate email
    // - getUser
    // - getUserByEmail
    // - getAllUsers
    // - updateUser
    // - updateUserStatus
    // - changePassword
    // - deactivateUser
}
