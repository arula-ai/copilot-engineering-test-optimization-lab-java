package com.example.lab.service;

import com.example.lab.model.dto.CreateUserRequest;
import com.example.lab.model.entity.User;
import com.example.lab.model.enums.UserStatus;
import com.example.lab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[\\d\\s\\-()]{10,}$"
    );

    @Transactional
    public User createUser(CreateUserRequest request) {
        // Validate email
        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already registered");
        }

        // Validate password
        var passwordValidation = validatePassword(request.getPassword());
        if (!passwordValidation.valid) {
            throw new IllegalArgumentException(passwordValidation.error);
        }

        // Validate names
        if (request.getFirstName() == null || request.getFirstName().trim().length() < 2) {
            throw new IllegalArgumentException("First name must be at least 2 characters");
        }

        if (request.getLastName() == null || request.getLastName().trim().length() < 2) {
            throw new IllegalArgumentException("Last name must be at least 2 characters");
        }

        User user = User.builder()
            .email(request.getEmail().toLowerCase())
            .password(hashPassword(request.getPassword()))
            .firstName(request.getFirstName().trim())
            .lastName(request.getLastName().trim())
            .phone(request.getPhone())
            .status(UserStatus.ACTIVE)
            .build();

        return userRepository.save(user);
    }

    public Optional<User> getUser(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByStatus(UserStatus status) {
        return userRepository.findByStatus(status);
    }

    @Transactional
    public User updateUser(String id, String firstName, String lastName, String phone) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (firstName != null && firstName.trim().length() >= 2) {
            user.setFirstName(firstName.trim());
        }

        if (lastName != null && lastName.trim().length() >= 2) {
            user.setLastName(lastName.trim());
        }

        if (phone != null) {
            if (!isValidPhone(phone)) {
                throw new IllegalArgumentException("Invalid phone number");
            }
            user.setPhone(phone);
        }

        return userRepository.save(user);
    }

    @Transactional
    public User updateUserStatus(String id, UserStatus status) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!verifyPassword(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        var passwordValidation = validatePassword(newPassword);
        if (!passwordValidation.valid) {
            throw new IllegalArgumentException(passwordValidation.error);
        }

        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPassword(hashPassword(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public record PasswordValidation(boolean valid, String error) {}

    public PasswordValidation validatePassword(String password) {
        if (password == null || password.length() < 8) {
            return new PasswordValidation(false, "Password must be at least 8 characters");
        }

        if (!password.matches(".*[A-Z].*")) {
            return new PasswordValidation(false, "Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            return new PasswordValidation(false, "Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            return new PasswordValidation(false, "Password must contain at least one number");
        }

        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            return new PasswordValidation(false, "Password must contain at least one special character");
        }

        return new PasswordValidation(true, null);
    }

    private String hashPassword(String password) {
        // Simplified hash for demo - in production use BCrypt
        return "hashed:" + password.hashCode();
    }

    private boolean verifyPassword(String rawPassword, String hashedPassword) {
        return hashedPassword.equals(hashPassword(rawPassword));
    }
}
