/**
 * GOLDEN EXAMPLE: Exception and Error Handling Testing
 *
 * PATTERN: Comprehensive Exception Testing
 *
 * WHEN TO USE:
 * - Testing exception throwing conditions
 * - Verifying error messages and types
 * - Testing error propagation chains
 * - Validating error recovery logic
 *
 * ANTI-PATTERNS THIS SOLVES:
 * ❌ Catching generic Exception
 * ❌ Not verifying exception messages
 * ❌ Missing exception cause verification
 * ❌ Only testing happy path
 *
 * KEY PRINCIPLES:
 * 1. Use assertThrows for expected exceptions
 * 2. Verify exception type, message, and cause
 * 3. Test all error paths, not just one
 * 4. Use parameterized tests for multiple error scenarios
 */

package com.example.lab.golden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Demonstrates error handling test patterns for the Test Optimization Lab.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Error Handling Golden Examples")
class ErrorHandlingPatternsGoldenTest {

    // ============================================================
    // PATTERN 1: Basic Exception Testing with assertThrows
    // ============================================================
    @Nested
    @DisplayName("Pattern 1: assertThrows Basics")
    class AssertThrowsBasics {

        private PaymentValidator validator;

        @BeforeEach
        void setUp() {
            validator = new PaymentValidator();
        }

        @Test
        @DisplayName("should throw when amount is negative")
        void shouldThrowWhenAmountNegative() {
            // Arrange
            BigDecimal negativeAmount = new BigDecimal("-100.00");

            // Act & Assert
            InvalidPaymentException exception = assertThrows(
                InvalidPaymentException.class,
                () -> validator.validateAmount(negativeAmount)
            );

            // Verify exception details
            assertThat(exception.getMessage()).contains("negative");
            assertThat(exception.getErrorCode()).isEqualTo("INVALID_AMOUNT");
        }

        @Test
        @DisplayName("should throw specific exception type")
        void shouldThrowSpecificExceptionType() {
            // Act & Assert - Verify exact type
            assertThrows(
                InvalidPaymentException.class,  // NOT RuntimeException
                () -> validator.validateAmount(null)
            );
        }

        @Test
        @DisplayName("should NOT throw for valid input")
        void shouldNotThrowForValidInput() {
            // Act & Assert - Verify no exception
            assertDoesNotThrow(() ->
                validator.validateAmount(new BigDecimal("100.00"))
            );
        }
    }

    // ============================================================
    // PATTERN 2: Exception Message Verification
    // ============================================================
    @Nested
    @DisplayName("Pattern 2: Message Verification")
    class MessageVerification {

        private OrderService orderService;

        @BeforeEach
        void setUp() {
            orderService = new OrderService();
        }

        @Test
        @DisplayName("should include relevant details in error message")
        void shouldIncludeRelevantDetailsInMessage() {
            // Arrange
            String orderId = "ORD-12345";

            // Act
            OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrder(orderId)
            );

            // Assert - Message contains useful info
            assertThat(exception.getMessage())
                .contains(orderId)
                .contains("not found")
                .doesNotContain("null")
                .doesNotContain("Exception");

            assertThat(exception.getOrderId()).isEqualTo(orderId);
        }

        @ParameterizedTest(name = "validation of {0} should fail with message containing \"{1}\"")
        @CsvSource({
            "'', must not be empty",
            "AB, must be at least 3 characters",
            "INVALID-FORMAT, must match pattern",
            "ORD-999999999999, ID too long"
        })
        void shouldProvideDescriptiveValidationMessages(String orderId, String expectedMessage) {
            // Act
            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> orderService.validateOrderId(orderId)
            );

            // Assert
            assertThat(exception.getMessage()).containsIgnoringCase(expectedMessage);
        }
    }

    // ============================================================
    // PATTERN 3: Exception Cause Chain Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 3: Cause Chain Verification")
    class CauseChainVerification {

        @Mock
        private PaymentGateway paymentGateway;

        @InjectMocks
        private PaymentProcessor paymentProcessor;

        @Test
        @DisplayName("should wrap low-level exception with service exception")
        void shouldWrapLowLevelException() {
            // Arrange - Gateway throws network error
            given(paymentGateway.charge(any()))
                .willThrow(new NetworkException("Connection refused"));

            // Act
            PaymentServiceException exception = assertThrows(
                PaymentServiceException.class,
                () -> paymentProcessor.processPayment(new PaymentRequest())
            );

            // Assert - Verify cause chain
            assertThat(exception)
                .hasMessage("Payment processing failed")
                .hasCauseInstanceOf(NetworkException.class)
                .hasRootCauseMessage("Connection refused");
        }

        @Test
        @DisplayName("should preserve original exception stack trace")
        void shouldPreserveOriginalStackTrace() {
            // Arrange
            NetworkException networkException = new NetworkException("Timeout");
            given(paymentGateway.charge(any())).willThrow(networkException);

            // Act
            PaymentServiceException exception = assertThrows(
                PaymentServiceException.class,
                () -> paymentProcessor.processPayment(new PaymentRequest())
            );

            // Assert - Cause is exact same instance
            assertThat(exception.getCause()).isSameAs(networkException);
        }
    }

    // ============================================================
    // PATTERN 4: Parameterized Exception Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 4: Parameterized Error Scenarios")
    class ParameterizedErrorScenarios {

        @Mock
        private ExternalService externalService;

        @InjectMocks
        private ServiceClient serviceClient;

        @ParameterizedTest(name = "HTTP {0} should map to {1}")
        @MethodSource("provideHttpErrorMappings")
        void shouldMapHttpErrorsToExceptions(
            int httpStatus,
            Class<? extends ServiceException> expectedExceptionType,
            String expectedMessage
        ) {
            // Arrange
            given(externalService.call(any()))
                .willThrow(new HttpException(httpStatus, "Error"));

            // Act
            ServiceException exception = assertThrows(
                expectedExceptionType,
                () -> serviceClient.callService(new ServiceRequest())
            );

            // Assert
            assertThat(exception.getMessage()).contains(expectedMessage);
        }

        static Stream<Arguments> provideHttpErrorMappings() {
            return Stream.of(
                Arguments.of(400, BadRequestException.class, "Invalid request"),
                Arguments.of(401, AuthenticationException.class, "Authentication required"),
                Arguments.of(403, AuthorizationException.class, "Access denied"),
                Arguments.of(404, NotFoundException.class, "Resource not found"),
                Arguments.of(422, ValidationException.class, "Validation failed"),
                Arguments.of(429, RateLimitException.class, "Rate limit exceeded"),
                Arguments.of(500, ServerException.class, "Internal server error"),
                Arguments.of(502, ServerException.class, "Bad gateway"),
                Arguments.of(503, ServiceUnavailableException.class, "Service unavailable")
            );
        }

        @ParameterizedTest(name = "should retry on {0} error")
        @CsvSource({
            "500, true",
            "502, true",
            "503, true",
            "504, true",
            "400, false",
            "401, false",
            "404, false"
        })
        void shouldDetermineRetryableErrors(int httpStatus, boolean shouldRetry) {
            // Arrange
            given(externalService.call(any()))
                .willThrow(new HttpException(httpStatus, "Error"));

            // Act
            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> serviceClient.callService(new ServiceRequest())
            );

            // Assert
            assertThat(exception.isRetryable()).isEqualTo(shouldRetry);
        }
    }

    // ============================================================
    // PATTERN 5: AssertJ Exception Assertions
    // ============================================================
    @Nested
    @DisplayName("Pattern 5: AssertJ Exception Assertions")
    class AssertJExceptionAssertions {

        private UserService userService;

        @BeforeEach
        void setUp() {
            userService = new UserService();
        }

        @Test
        @DisplayName("should use assertThatThrownBy for fluent assertions")
        void shouldUseAssertThatThrownBy() {
            // Act & Assert - Fluent style
            assertThatThrownBy(() -> userService.getUser(null))
                .isInstanceOf(InvalidUserException.class)
                .hasMessageContaining("User ID cannot be null")
                .hasNoCause();
        }

        @Test
        @DisplayName("should use assertThatExceptionOfType for type-first assertions")
        void shouldUseAssertThatExceptionOfType() {
            // Act & Assert - Type-first style
            assertThatExceptionOfType(InvalidUserException.class)
                .isThrownBy(() -> userService.getUser(""))
                .withMessageMatching("User ID.*empty.*")
                .withNoCause();
        }

        @Test
        @DisplayName("should use assertThatCode for no-exception verification")
        void shouldUseAssertThatCode() {
            // Act & Assert - Verify no exception
            assertThatCode(() -> userService.getUser("valid-user-id"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should verify exception fields with extracting")
        void shouldVerifyExceptionFieldsWithExtracting() {
            // Act & Assert
            assertThatThrownBy(() -> userService.updateUser("inactive-user", new UserUpdate()))
                .isInstanceOf(UserStateException.class)
                .extracting("userId", "currentState", "requiredState")
                .containsExactly("inactive-user", "INACTIVE", "ACTIVE");
        }
    }

    // ============================================================
    // PATTERN 6: Error State Verification
    // ============================================================
    @Nested
    @DisplayName("Pattern 6: Error State Verification")
    class ErrorStateVerification {

        @Mock
        private PaymentRepository paymentRepository;

        @InjectMocks
        private PaymentService paymentService;

        @Test
        @DisplayName("should set error state on failure")
        void shouldSetErrorStateOnFailure() {
            // Arrange
            given(paymentRepository.save(any()))
                .willThrow(new DatabaseException("Connection lost"));

            PaymentRequest request = new PaymentRequest();

            // Act
            assertThrows(PaymentServiceException.class,
                () -> paymentService.processPayment(request));

            // Assert - Verify error state was set
            assertThat(paymentService.getLastError()).isNotNull();
            assertThat(paymentService.getLastError().getCode()).isEqualTo("DB_ERROR");
            assertThat(paymentService.hasError()).isTrue();
        }

        @Test
        @DisplayName("should clear error state on success")
        void shouldClearErrorStateOnSuccess() {
            // Arrange - Set initial error state
            paymentService.setError(new PaymentError("PREVIOUS_ERROR", "Previous failure"));

            given(paymentRepository.save(any()))
                .willReturn(new Payment("PAY-001", BigDecimal.TEN, "SUCCESS"));

            // Act
            paymentService.processPayment(new PaymentRequest());

            // Assert - Error cleared
            assertThat(paymentService.hasError()).isFalse();
            assertThat(paymentService.getLastError()).isNull();
        }

        @Test
        @DisplayName("should track error history")
        void shouldTrackErrorHistory() {
            // Arrange & Act - Cause multiple errors
            given(paymentRepository.save(any()))
                .willThrow(new DatabaseException("Error 1"))
                .willThrow(new DatabaseException("Error 2"))
                .willReturn(new Payment("PAY-001", BigDecimal.TEN, "SUCCESS"));

            assertThrows(PaymentServiceException.class,
                () -> paymentService.processPayment(new PaymentRequest()));
            assertThrows(PaymentServiceException.class,
                () -> paymentService.processPayment(new PaymentRequest()));
            paymentService.processPayment(new PaymentRequest()); // Success

            // Assert - History tracked
            assertThat(paymentService.getErrorHistory())
                .hasSize(2)
                .extracting(PaymentError::getMessage)
                .containsExactly("Error 1", "Error 2");
        }
    }

    // ============================================================
    // PATTERN 7: Cleanup Verification on Error
    // ============================================================
    @Nested
    @DisplayName("Pattern 7: Cleanup on Error")
    class CleanupOnError {

        @Mock
        private ResourceManager resourceManager;

        @Mock
        private DataProcessor dataProcessor;

        @InjectMocks
        private BatchProcessor batchProcessor;

        @Test
        @DisplayName("should release resources on processing error")
        void shouldReleaseResourcesOnError() {
            // Arrange
            Resource resource = new Resource("RES-001");
            given(resourceManager.acquire()).willReturn(resource);
            given(dataProcessor.process(any()))
                .willThrow(new ProcessingException("Processing failed"));

            // Act
            assertThrows(BatchProcessingException.class,
                () -> batchProcessor.processBatch(new BatchData()));

            // Assert - Resource was released despite error
            then(resourceManager).should().release(resource);
        }

        @Test
        @DisplayName("should rollback transaction on error")
        void shouldRollbackTransactionOnError() {
            // Arrange
            given(dataProcessor.process(any()))
                .willThrow(new ProcessingException("Processing failed"));

            // Act
            assertThrows(BatchProcessingException.class,
                () -> batchProcessor.processBatchWithTransaction(new BatchData()));

            // Assert - Transaction was rolled back
            assertThat(batchProcessor.getTransactionState()).isEqualTo("ROLLED_BACK");
        }
    }

    // ============================================================
    // Helper Classes for Examples
    // ============================================================

    // Payment domain
    static class PaymentValidator {
        void validateAmount(BigDecimal amount) {
            if (amount == null) throw new InvalidPaymentException("Amount cannot be null", "INVALID_AMOUNT");
            if (amount.compareTo(BigDecimal.ZERO) < 0) throw new InvalidPaymentException("Amount cannot be negative", "INVALID_AMOUNT");
        }
    }

    static class InvalidPaymentException extends RuntimeException {
        private final String errorCode;
        InvalidPaymentException(String message, String errorCode) { super(message); this.errorCode = errorCode; }
        String getErrorCode() { return errorCode; }
    }

    // Order domain
    static class OrderService {
        void getOrder(String orderId) { throw new OrderNotFoundException(orderId); }
        void validateOrderId(String orderId) {
            if (orderId == null || orderId.isEmpty()) throw new ValidationException("Order ID must not be empty");
            if (orderId.length() < 3) throw new ValidationException("Order ID must be at least 3 characters");
            if (!orderId.matches("ORD-\\d+")) throw new ValidationException("Order ID must match pattern ORD-<number>");
            if (orderId.length() > 15) throw new ValidationException("Order ID too long");
        }
    }

    static class OrderNotFoundException extends RuntimeException {
        private final String orderId;
        OrderNotFoundException(String orderId) { super("Order " + orderId + " not found"); this.orderId = orderId; }
        String getOrderId() { return orderId; }
    }

    static class ValidationException extends RuntimeException {
        ValidationException(String message) { super(message); }
    }

    // Network/HTTP domain
    static class NetworkException extends RuntimeException {
        NetworkException(String message) { super(message); }
    }

    static class HttpException extends RuntimeException {
        private final int status;
        HttpException(int status, String message) { super(message); this.status = status; }
        int getStatus() { return status; }
    }

    interface PaymentGateway { void charge(PaymentRequest request); }
    interface ExternalService { void call(ServiceRequest request); }

    static class PaymentProcessor {
        private final PaymentGateway paymentGateway;
        PaymentProcessor(PaymentGateway gateway) { this.paymentGateway = gateway; }
        void processPayment(PaymentRequest request) {
            try { paymentGateway.charge(request); }
            catch (NetworkException e) { throw new PaymentServiceException("Payment processing failed", e); }
        }
    }

    static class PaymentServiceException extends RuntimeException {
        PaymentServiceException(String message, Throwable cause) { super(message, cause); }
    }

    static class ServiceRequest {}
    static class ServiceException extends RuntimeException {
        private final boolean retryable;
        ServiceException(String message, boolean retryable) { super(message); this.retryable = retryable; }
        boolean isRetryable() { return retryable; }
    }
    static class BadRequestException extends ServiceException { BadRequestException() { super("Invalid request", false); } }
    static class AuthenticationException extends ServiceException { AuthenticationException() { super("Authentication required", false); } }
    static class AuthorizationException extends ServiceException { AuthorizationException() { super("Access denied", false); } }
    static class NotFoundException extends ServiceException { NotFoundException() { super("Resource not found", false); } }
    static class RateLimitException extends ServiceException { RateLimitException() { super("Rate limit exceeded", true); } }
    static class ServerException extends ServiceException { ServerException(String message) { super(message, true); } }
    static class ServiceUnavailableException extends ServiceException { ServiceUnavailableException() { super("Service unavailable", true); } }

    static class ServiceClient {
        private final ExternalService externalService;
        ServiceClient(ExternalService service) { this.externalService = service; }
        void callService(ServiceRequest request) {
            try { externalService.call(request); }
            catch (HttpException e) {
                throw switch (e.getStatus()) {
                    case 400 -> new BadRequestException();
                    case 401 -> new AuthenticationException();
                    case 403 -> new AuthorizationException();
                    case 404 -> new NotFoundException();
                    case 422 -> new ValidationException("Validation failed");
                    case 429 -> new RateLimitException();
                    case 503 -> new ServiceUnavailableException();
                    default -> new ServerException(e.getStatus() >= 500 ? "Internal server error" : "Bad gateway");
                };
            }
        }
    }

    // User domain
    static class UserService {
        void getUser(String userId) {
            if (userId == null) throw new InvalidUserException("User ID cannot be null");
            if (userId.isEmpty()) throw new InvalidUserException("User ID cannot be empty");
        }
        void updateUser(String userId, UserUpdate update) {
            if (userId.startsWith("inactive")) throw new UserStateException(userId, "INACTIVE", "ACTIVE");
        }
    }
    static class UserUpdate {}
    static class InvalidUserException extends RuntimeException {
        InvalidUserException(String message) { super(message); }
    }
    static class UserStateException extends RuntimeException {
        private final String userId;
        private final String currentState;
        private final String requiredState;
        UserStateException(String userId, String current, String required) {
            super("User state mismatch");
            this.userId = userId; this.currentState = current; this.requiredState = required;
        }
    }

    // Payment service with error state
    static class PaymentRequest {}
    static class Payment {
        Payment(String id, BigDecimal amount, String status) {}
    }
    interface PaymentRepository { Payment save(Payment payment); }
    static class DatabaseException extends RuntimeException {
        DatabaseException(String message) { super(message); }
    }
    static class PaymentError {
        private final String code;
        private final String message;
        PaymentError(String code, String message) { this.code = code; this.message = message; }
        String getCode() { return code; }
        String getMessage() { return message; }
    }
    static class PaymentService {
        private PaymentError lastError;
        private java.util.List<PaymentError> errorHistory = new java.util.ArrayList<>();
        private final PaymentRepository paymentRepository;
        PaymentService(PaymentRepository repo) { this.paymentRepository = repo; }
        void processPayment(PaymentRequest request) {
            try {
                paymentRepository.save(new Payment(null, null, null));
                lastError = null;
            } catch (DatabaseException e) {
                lastError = new PaymentError("DB_ERROR", e.getMessage());
                errorHistory.add(lastError);
                throw new PaymentServiceException("Payment failed", e);
            }
        }
        PaymentError getLastError() { return lastError; }
        boolean hasError() { return lastError != null; }
        void setError(PaymentError error) { this.lastError = error; }
        java.util.List<PaymentError> getErrorHistory() { return errorHistory; }
    }

    // Batch processing
    static class Resource { Resource(String id) {} }
    interface ResourceManager { Resource acquire(); void release(Resource resource); }
    interface DataProcessor { void process(BatchData data); }
    static class BatchData {}
    static class ProcessingException extends RuntimeException {
        ProcessingException(String message) { super(message); }
    }
    static class BatchProcessingException extends RuntimeException {
        BatchProcessingException(String message, Throwable cause) { super(message, cause); }
    }
    static class BatchProcessor {
        private final ResourceManager resourceManager;
        private final DataProcessor dataProcessor;
        private String transactionState = "NONE";
        BatchProcessor(ResourceManager rm, DataProcessor dp) { this.resourceManager = rm; this.dataProcessor = dp; }
        void processBatch(BatchData data) {
            Resource resource = resourceManager.acquire();
            try { dataProcessor.process(data); }
            catch (ProcessingException e) { throw new BatchProcessingException("Batch failed", e); }
            finally { resourceManager.release(resource); }
        }
        void processBatchWithTransaction(BatchData data) {
            transactionState = "STARTED";
            try { dataProcessor.process(data); transactionState = "COMMITTED"; }
            catch (ProcessingException e) { transactionState = "ROLLED_BACK"; throw new BatchProcessingException("Batch failed", e); }
        }
        String getTransactionState() { return transactionState; }
    }
}

/**
 * SUMMARY OF ERROR HANDLING PATTERNS:
 *
 * 1. assertThrows - Basic exception testing
 * 2. Verify message, type, and cause
 * 3. Use parameterized tests for error mappings
 * 4. assertThatThrownBy - AssertJ fluent style
 * 5. Verify error state is set/cleared
 * 6. Verify cleanup happens on error
 * 7. Track error history for diagnostics
 */
