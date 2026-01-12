/**
 * GOLDEN EXAMPLE: Mockito Mocking Patterns
 *
 * PATTERN: Dependency Isolation with Mocks
 *
 * WHEN TO USE:
 * - Unit testing services with dependencies
 * - Isolating external service calls
 * - Testing interaction between components
 * - Verifying method invocations
 *
 * ANTI-PATTERNS THIS SOLVES:
 * ❌ Real dependencies in unit tests
 * ❌ Missing mock verification
 * ❌ Over-mocking (mocking the class under test)
 * ❌ Fragile tests tied to implementation
 *
 * KEY PRINCIPLES:
 * 1. Use @ExtendWith(MockitoExtension.class)
 * 2. @Mock for dependencies, @InjectMocks for SUT
 * 3. Verify interactions, not just state
 * 4. Use argument matchers appropriately
 */

package com.example.lab.golden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;

/**
 * Demonstrates all Mockito mocking patterns for the Test Optimization Lab.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Mockito Mocking Golden Examples")
class MockitoPatternsGoldenTest {

    // ============================================================
    // PATTERN 1: Basic Setup with @Mock and @InjectMocks
    // ============================================================
    @Nested
    @DisplayName("Pattern 1: Basic Mock Setup")
    class BasicMockSetup {

        @Mock
        private PaymentRepository paymentRepository;

        @Mock
        private NotificationService notificationService;

        @Mock
        private AuditLogger auditLogger;

        @InjectMocks
        private PaymentService paymentService;

        @Test
        @DisplayName("should process payment with all dependencies mocked")
        void shouldProcessPaymentWithMockedDependencies() {
            // Arrange - Stub dependencies
            Payment mockPayment = new Payment("PAY-001", new BigDecimal("100.00"), "COMPLETED");
            given(paymentRepository.save(any(Payment.class))).willReturn(mockPayment);

            // Act
            PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), "USD", "credit_card");
            Payment result = paymentService.processPayment(request);

            // Assert - Verify result
            assertThat(result.getId()).isEqualTo("PAY-001");
            assertThat(result.getStatus()).isEqualTo("COMPLETED");

            // Verify interactions
            then(paymentRepository).should().save(any(Payment.class));
            then(notificationService).should().sendPaymentConfirmation(eq("PAY-001"));
            then(auditLogger).should().logPaymentProcessed(any(Payment.class));
        }
    }

    // ============================================================
    // PATTERN 2: Stubbing with when().thenReturn()
    // ============================================================
    @Nested
    @DisplayName("Pattern 2: Stubbing Patterns")
    class StubbingPatterns {

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private UserService userService;

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() {
            // Arrange - Stub to return value
            User mockUser = new User("user-123", "john@example.com");
            given(userRepository.findById("user-123")).willReturn(Optional.of(mockUser));

            // Act
            Optional<User> result = userService.findById("user-123");

            // Assert
            assertThat(result)
                .isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user.getId()).isEqualTo("user-123");
                    assertThat(user.getEmail()).isEqualTo("john@example.com");
                });
        }

        @Test
        @DisplayName("should return empty when user not found")
        void shouldReturnEmptyWhenNotFound() {
            // Arrange - Stub to return empty
            given(userRepository.findById(anyString())).willReturn(Optional.empty());

            // Act
            Optional<User> result = userService.findById("nonexistent");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw exception on repository error")
        void shouldThrowOnRepositoryError() {
            // Arrange - Stub to throw exception
            given(userRepository.findById(anyString()))
                .willThrow(new DatabaseException("Connection failed"));

            // Act & Assert
            assertThatThrownBy(() -> userService.findById("user-123"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Unable to fetch user")
                .hasCauseInstanceOf(DatabaseException.class);
        }

        @Test
        @DisplayName("should return different values on consecutive calls")
        void shouldReturnDifferentValuesOnConsecutiveCalls() {
            // Arrange - Stub for consecutive calls
            given(userRepository.countActiveUsers())
                .willReturn(10)  // First call
                .willReturn(15)  // Second call
                .willReturn(20); // Third and subsequent calls

            // Act & Assert
            assertThat(userService.getActiveUserCount()).isEqualTo(10);
            assertThat(userService.getActiveUserCount()).isEqualTo(15);
            assertThat(userService.getActiveUserCount()).isEqualTo(20);
            assertThat(userService.getActiveUserCount()).isEqualTo(20); // Stays at 20
        }
    }

    // ============================================================
    // PATTERN 3: Verification with verify()
    // ============================================================
    @Nested
    @DisplayName("Pattern 3: Verification Patterns")
    class VerificationPatterns {

        @Mock
        private OrderRepository orderRepository;

        @Mock
        private InventoryService inventoryService;

        @Mock
        private EmailService emailService;

        @InjectMocks
        private OrderService orderService;

        @Test
        @DisplayName("should verify method was called exactly once")
        void shouldVerifyMethodCalledOnce() {
            // Arrange
            Order mockOrder = new Order("ORD-001", "PENDING");
            given(orderRepository.save(any())).willReturn(mockOrder);
            given(inventoryService.reserveStock(anyString(), anyInt())).willReturn(true);

            // Act
            orderService.createOrder(new OrderRequest("PROD-001", 2));

            // Assert - Verify exact invocations
            then(orderRepository).should(times(1)).save(any(Order.class));
            then(inventoryService).should(times(1)).reserveStock("PROD-001", 2);
            then(emailService).should(times(1)).sendOrderConfirmation(eq("ORD-001"));
        }

        @Test
        @DisplayName("should verify method was never called")
        void shouldVerifyMethodNeverCalled() {
            // Arrange - Stock reservation fails
            given(inventoryService.reserveStock(anyString(), anyInt())).willReturn(false);

            // Act
            assertThatThrownBy(() ->
                orderService.createOrder(new OrderRequest("PROD-001", 2))
            ).isInstanceOf(InsufficientStockException.class);

            // Assert - Order not saved, email not sent
            then(orderRepository).should(never()).save(any());
            then(emailService).should(never()).sendOrderConfirmation(anyString());
        }

        @Test
        @DisplayName("should verify call order")
        void shouldVerifyCallOrder() {
            // Arrange
            Order mockOrder = new Order("ORD-001", "PENDING");
            given(orderRepository.save(any())).willReturn(mockOrder);
            given(inventoryService.reserveStock(anyString(), anyInt())).willReturn(true);

            // Act
            orderService.createOrder(new OrderRequest("PROD-001", 2));

            // Assert - Verify order of operations
            InOrder inOrder = inOrder(inventoryService, orderRepository, emailService);
            inOrder.verify(inventoryService).reserveStock(anyString(), anyInt());
            inOrder.verify(orderRepository).save(any(Order.class));
            inOrder.verify(emailService).sendOrderConfirmation(anyString());
        }
    }

    // ============================================================
    // PATTERN 4: Argument Matchers
    // ============================================================
    @Nested
    @DisplayName("Pattern 4: Argument Matchers")
    class ArgumentMatcherPatterns {

        @Mock
        private PaymentGateway paymentGateway;

        @InjectMocks
        private PaymentProcessor paymentProcessor;

        @Test
        @DisplayName("should use any() for flexible matching")
        void shouldUseAnyMatcher() {
            // Arrange
            given(paymentGateway.charge(any(PaymentDetails.class)))
                .willReturn(new ChargeResult("CHG-001", true));

            // Act
            paymentProcessor.processCharge(new PaymentDetails("4111111111111111", "100.00"));

            // Assert
            then(paymentGateway).should().charge(any(PaymentDetails.class));
        }

        @Test
        @DisplayName("should use eq() for exact matching")
        void shouldUseEqMatcher() {
            // Arrange
            given(paymentGateway.refund(eq("CHG-001"), any(BigDecimal.class)))
                .willReturn(true);

            // Act
            boolean result = paymentProcessor.refund("CHG-001", new BigDecimal("50.00"));

            // Assert
            assertThat(result).isTrue();
            then(paymentGateway).should().refund(eq("CHG-001"), any(BigDecimal.class));
        }

        @Test
        @DisplayName("should use argThat() for custom matching")
        void shouldUseArgThatMatcher() {
            // Arrange
            given(paymentGateway.charge(argThat(details ->
                details.getAmount().compareTo(new BigDecimal("100")) > 0
            ))).willReturn(new ChargeResult("CHG-001", true));

            // Act
            paymentProcessor.processCharge(new PaymentDetails("4111111111111111", "150.00"));

            // Assert
            then(paymentGateway).should().charge(argThat(details ->
                details.getAmount().compareTo(new BigDecimal("100")) > 0
            ));
        }
    }

    // ============================================================
    // PATTERN 5: Argument Captor
    // ============================================================
    @Nested
    @DisplayName("Pattern 5: Argument Captor")
    class ArgumentCaptorPatterns {

        @Mock
        private NotificationService notificationService;

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private UserRegistrationService registrationService;

        @Captor
        private ArgumentCaptor<Notification> notificationCaptor;

        @Test
        @DisplayName("should capture and verify argument details")
        void shouldCaptureArguments() {
            // Arrange
            given(userRepository.save(any())).willReturn(new User("user-123", "john@example.com"));

            // Act
            registrationService.registerUser("john@example.com", "password123");

            // Assert - Capture the notification argument
            then(notificationService).should().send(notificationCaptor.capture());

            Notification capturedNotification = notificationCaptor.getValue();
            assertThat(capturedNotification.getType()).isEqualTo("WELCOME");
            assertThat(capturedNotification.getRecipient()).isEqualTo("john@example.com");
            assertThat(capturedNotification.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("should capture multiple invocations")
        void shouldCaptureMultipleInvocations() {
            // Arrange
            given(userRepository.save(any())).willReturn(new User("user-123", "john@example.com"));

            // Act
            registrationService.registerUserWithVerification("john@example.com", "password123");

            // Assert - Capture all notifications
            then(notificationService).should(times(2)).send(notificationCaptor.capture());

            assertThat(notificationCaptor.getAllValues())
                .hasSize(2)
                .extracting(Notification::getType)
                .containsExactly("WELCOME", "VERIFY_EMAIL");
        }
    }

    // ============================================================
    // PATTERN 6: Spy - Partial Mocking
    // ============================================================
    @Nested
    @DisplayName("Pattern 6: Spy Pattern")
    class SpyPatterns {

        @Spy
        private PriceCalculator priceCalculator = new PriceCalculator();

        @InjectMocks
        private CheckoutService checkoutService;

        @Test
        @DisplayName("should use spy for partial mocking")
        void shouldUseSpyForPartialMocking() {
            // Arrange - Override only specific method
            doReturn(new BigDecimal("10.00")).when(priceCalculator).getShippingCost(any());

            // Act - Real method calculateSubtotal is called, shipping is mocked
            BigDecimal total = checkoutService.calculateTotal(new Cart(new BigDecimal("100.00")));

            // Assert
            assertThat(total).isEqualByComparingTo(new BigDecimal("110.00"));
        }
    }

    // ============================================================
    // PATTERN 7: BDD Style with given/when/then
    // ============================================================
    @Nested
    @DisplayName("Pattern 7: BDD Style")
    class BddStylePatterns {

        @Mock
        private ProductRepository productRepository;

        @InjectMocks
        private ProductService productService;

        @Test
        @DisplayName("BDD: should find product by SKU")
        void shouldFindProductBySku() {
            // Given
            Product mockProduct = new Product("SKU-001", "Widget", new BigDecimal("29.99"));
            given(productRepository.findBySku("SKU-001")).willReturn(Optional.of(mockProduct));

            // When
            Optional<Product> result = productService.findBySku("SKU-001");

            // Then
            then(productRepository).should().findBySku("SKU-001");
            assertThat(result)
                .isPresent()
                .hasValueSatisfying(product -> {
                    assertThat(product.getSku()).isEqualTo("SKU-001");
                    assertThat(product.getName()).isEqualTo("Widget");
                });
        }
    }

    // ============================================================
    // Helper Classes for Examples
    // ============================================================

    // Payment domain
    static class Payment {
        private final String id;
        private final BigDecimal amount;
        private final String status;
        Payment(String id, BigDecimal amount, String status) { this.id = id; this.amount = amount; this.status = status; }
        String getId() { return id; }
        BigDecimal getAmount() { return amount; }
        String getStatus() { return status; }
    }
    static class PaymentRequest {
        private final BigDecimal amount;
        private final String currency;
        private final String method;
        PaymentRequest(BigDecimal amount, String currency, String method) { this.amount = amount; this.currency = currency; this.method = method; }
    }
    interface PaymentRepository { Payment save(Payment payment); }
    interface NotificationService {
        void sendPaymentConfirmation(String paymentId);
        void send(Notification notification);
    }
    interface AuditLogger { void logPaymentProcessed(Payment payment); }
    static class PaymentService {
        private final PaymentRepository paymentRepository;
        private final NotificationService notificationService;
        private final AuditLogger auditLogger;
        PaymentService(PaymentRepository repo, NotificationService notif, AuditLogger audit) {
            this.paymentRepository = repo; this.notificationService = notif; this.auditLogger = audit;
        }
        Payment processPayment(PaymentRequest request) {
            Payment payment = paymentRepository.save(new Payment(null, request.amount, "COMPLETED"));
            notificationService.sendPaymentConfirmation(payment.getId());
            auditLogger.logPaymentProcessed(payment);
            return payment;
        }
    }

    // User domain
    static class User {
        private final String id;
        private final String email;
        User(String id, String email) { this.id = id; this.email = email; }
        String getId() { return id; }
        String getEmail() { return email; }
    }
    interface UserRepository {
        Optional<User> findById(String id);
        int countActiveUsers();
        User save(User user);
    }
    static class DatabaseException extends RuntimeException {
        DatabaseException(String message) { super(message); }
    }
    static class ServiceException extends RuntimeException {
        ServiceException(String message, Throwable cause) { super(message, cause); }
    }
    static class UserService {
        private final UserRepository userRepository;
        UserService(UserRepository repo) { this.userRepository = repo; }
        Optional<User> findById(String id) {
            try { return userRepository.findById(id); }
            catch (DatabaseException e) { throw new ServiceException("Unable to fetch user", e); }
        }
        int getActiveUserCount() { return userRepository.countActiveUsers(); }
    }

    // Order domain
    static class Order {
        private final String id;
        private final String status;
        Order(String id, String status) { this.id = id; this.status = status; }
        String getId() { return id; }
    }
    static class OrderRequest {
        private final String productId;
        private final int quantity;
        OrderRequest(String productId, int quantity) { this.productId = productId; this.quantity = quantity; }
    }
    interface OrderRepository { Order save(Order order); }
    interface InventoryService { boolean reserveStock(String productId, int quantity); }
    interface EmailService { void sendOrderConfirmation(String orderId); }
    static class InsufficientStockException extends RuntimeException {}
    static class OrderService {
        private final OrderRepository orderRepository;
        private final InventoryService inventoryService;
        private final EmailService emailService;
        OrderService(OrderRepository repo, InventoryService inv, EmailService email) {
            this.orderRepository = repo; this.inventoryService = inv; this.emailService = email;
        }
        Order createOrder(OrderRequest request) {
            if (!inventoryService.reserveStock(request.productId, request.quantity)) {
                throw new InsufficientStockException();
            }
            Order order = orderRepository.save(new Order("ORD-001", "PENDING"));
            emailService.sendOrderConfirmation(order.getId());
            return order;
        }
    }

    // Payment gateway
    static class PaymentDetails {
        private final String cardNumber;
        private final BigDecimal amount;
        PaymentDetails(String cardNumber, String amount) { this.cardNumber = cardNumber; this.amount = new BigDecimal(amount); }
        BigDecimal getAmount() { return amount; }
    }
    static class ChargeResult {
        private final String chargeId;
        private final boolean success;
        ChargeResult(String chargeId, boolean success) { this.chargeId = chargeId; this.success = success; }
    }
    interface PaymentGateway {
        ChargeResult charge(PaymentDetails details);
        boolean refund(String chargeId, BigDecimal amount);
    }
    static class PaymentProcessor {
        private final PaymentGateway paymentGateway;
        PaymentProcessor(PaymentGateway gateway) { this.paymentGateway = gateway; }
        ChargeResult processCharge(PaymentDetails details) { return paymentGateway.charge(details); }
        boolean refund(String chargeId, BigDecimal amount) { return paymentGateway.refund(chargeId, amount); }
    }

    // Notification
    static class Notification {
        private final String type;
        private final String recipient;
        private final LocalDateTime timestamp;
        Notification(String type, String recipient) { this.type = type; this.recipient = recipient; this.timestamp = LocalDateTime.now(); }
        String getType() { return type; }
        String getRecipient() { return recipient; }
        LocalDateTime getTimestamp() { return timestamp; }
    }
    static class UserRegistrationService {
        private final UserRepository userRepository;
        private final NotificationService notificationService;
        UserRegistrationService(UserRepository repo, NotificationService notif) {
            this.userRepository = repo; this.notificationService = notif;
        }
        void registerUser(String email, String password) {
            userRepository.save(new User(null, email));
            notificationService.send(new Notification("WELCOME", email));
        }
        void registerUserWithVerification(String email, String password) {
            userRepository.save(new User(null, email));
            notificationService.send(new Notification("WELCOME", email));
            notificationService.send(new Notification("VERIFY_EMAIL", email));
        }
    }

    // Checkout
    static class Cart {
        private final BigDecimal subtotal;
        Cart(BigDecimal subtotal) { this.subtotal = subtotal; }
        BigDecimal getSubtotal() { return subtotal; }
    }
    static class PriceCalculator {
        BigDecimal calculateSubtotal(Cart cart) { return cart.getSubtotal(); }
        BigDecimal getShippingCost(Cart cart) { return new BigDecimal("5.00"); }
    }
    static class CheckoutService {
        private final PriceCalculator priceCalculator;
        CheckoutService(PriceCalculator calc) { this.priceCalculator = calc; }
        BigDecimal calculateTotal(Cart cart) {
            return priceCalculator.calculateSubtotal(cart).add(priceCalculator.getShippingCost(cart));
        }
    }

    // Product
    static class Product {
        private final String sku;
        private final String name;
        private final BigDecimal price;
        Product(String sku, String name, BigDecimal price) { this.sku = sku; this.name = name; this.price = price; }
        String getSku() { return sku; }
        String getName() { return name; }
    }
    interface ProductRepository { Optional<Product> findBySku(String sku); }
    static class ProductService {
        private final ProductRepository productRepository;
        ProductService(ProductRepository repo) { this.productRepository = repo; }
        Optional<Product> findBySku(String sku) { return productRepository.findBySku(sku); }
    }
}

/**
 * SUMMARY OF MOCKITO PATTERNS:
 *
 * 1. @ExtendWith(MockitoExtension.class) - Enable Mockito
 * 2. @Mock - Create mock for dependency
 * 3. @InjectMocks - Inject mocks into SUT
 * 4. given().willReturn() - BDD stubbing
 * 5. then().should() - BDD verification
 * 6. verify(times/never) - Invocation counts
 * 7. inOrder() - Verify call sequence
 * 8. any/eq/argThat - Argument matchers
 * 9. @Captor - Capture arguments for assertion
 * 10. @Spy - Partial mocking
 */
