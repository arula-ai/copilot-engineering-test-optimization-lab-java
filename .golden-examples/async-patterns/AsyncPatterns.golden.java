/**
 * GOLDEN EXAMPLE: Async Testing with CompletableFuture
 *
 * PATTERN: Deterministic Async Testing
 *
 * WHEN TO USE:
 * - Testing CompletableFuture operations
 * - Testing async service calls
 * - Testing concurrent operations
 * - Testing timeout scenarios
 *
 * ANTI-PATTERNS THIS SOLVES:
 * ❌ Thread.sleep() in tests
 * ❌ Flaky tests from race conditions
 * ❌ Tests that hang indefinitely
 * ❌ Not testing exception completion
 *
 * KEY PRINCIPLES:
 * 1. Use get(timeout) instead of get()
 * 2. Test both success and exception completion
 * 3. Use assertj-core for CompletableFuture assertions
 * 4. Test concurrent operations deterministically
 */

package com.example.lab.golden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Demonstrates async testing patterns for the Test Optimization Lab.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Async Testing Golden Examples")
class AsyncPatternsGoldenTest {

    // ============================================================
    // PATTERN 1: Basic CompletableFuture Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 1: Basic CompletableFuture")
    class BasicCompletableFuture {

        private AsyncCalculator calculator;

        @BeforeEach
        void setUp() {
            calculator = new AsyncCalculator();
        }

        @Test
        @DisplayName("should complete with expected value")
        @Timeout(5) // Fail fast if test hangs
        void shouldCompleteWithExpectedValue() throws Exception {
            // Act
            CompletableFuture<Integer> future = calculator.calculateAsync(10, 20);

            // Assert - Use get with timeout
            Integer result = future.get(1, TimeUnit.SECONDS);
            assertThat(result).isEqualTo(30);
        }

        @Test
        @DisplayName("should use assertj for CompletableFuture assertions")
        void shouldUseAssertJForFutureAssertions() {
            // Act
            CompletableFuture<Integer> future = calculator.calculateAsync(5, 5);

            // Assert - AssertJ fluent style
            assertThat(future)
                .succeedsWithin(Duration.ofSeconds(1))
                .isEqualTo(10);
        }

        @Test
        @DisplayName("should verify future completes successfully")
        void shouldVerifyFutureCompletesSuccessfully() {
            // Act
            CompletableFuture<String> future = calculator.processAsync("input");

            // Assert - Verify completion state
            assertThat(future)
                .isCompletedWithValue("PROCESSED: input");
        }
    }

    // ============================================================
    // PATTERN 2: Exception Completion Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 2: Exception Completion")
    class ExceptionCompletion {

        private AsyncCalculator calculator;

        @BeforeEach
        void setUp() {
            calculator = new AsyncCalculator();
        }

        @Test
        @DisplayName("should complete exceptionally on error")
        void shouldCompleteExceptionallyOnError() {
            // Act
            CompletableFuture<Integer> future = calculator.divideAsync(10, 0);

            // Assert - Verify exception completion
            assertThat(future)
                .failsWithin(Duration.ofSeconds(1))
                .withThrowableOfType(ExecutionException.class)
                .havingCause()
                .isInstanceOf(ArithmeticException.class)
                .withMessage("Division by zero");
        }

        @Test
        @DisplayName("should handle exception with exceptionally")
        void shouldHandleExceptionWithExceptionally() throws Exception {
            // Act - Error with fallback
            CompletableFuture<Integer> future = calculator.divideAsync(10, 0)
                .exceptionally(ex -> -1);

            // Assert - Fallback value used
            Integer result = future.get(1, TimeUnit.SECONDS);
            assertThat(result).isEqualTo(-1);
        }

        @Test
        @DisplayName("should propagate exception through chain")
        void shouldPropagateExceptionThroughChain() {
            // Act - Chain of operations
            CompletableFuture<String> future = calculator.divideAsync(10, 0)
                .thenApply(result -> "Result: " + result)
                .thenApply(String::toUpperCase);

            // Assert - Exception propagates
            assertThat(future)
                .failsWithin(Duration.ofSeconds(1))
                .withThrowableOfType(ExecutionException.class)
                .havingCause()
                .isInstanceOf(ArithmeticException.class);
        }
    }

    // ============================================================
    // PATTERN 3: Mocking Async Services
    // ============================================================
    @Nested
    @DisplayName("Pattern 3: Mocking Async Services")
    class MockingAsyncServices {

        @Mock
        private ExternalApiClient apiClient;

        @InjectMocks
        private DataAggregator dataAggregator;

        @Test
        @DisplayName("should mock async service call")
        void shouldMockAsyncServiceCall() throws Exception {
            // Arrange - Mock returns completed future
            given(apiClient.fetchDataAsync(anyString()))
                .willReturn(CompletableFuture.completedFuture(new ApiResponse("data")));

            // Act
            CompletableFuture<ProcessedData> future = dataAggregator.aggregateAsync("key");

            // Assert
            ProcessedData result = future.get(1, TimeUnit.SECONDS);
            assertThat(result.getValue()).isEqualTo("AGGREGATED: data");
        }

        @Test
        @DisplayName("should mock async service failure")
        void shouldMockAsyncServiceFailure() {
            // Arrange - Mock returns failed future
            given(apiClient.fetchDataAsync(anyString()))
                .willReturn(CompletableFuture.failedFuture(new ApiException("Service unavailable")));

            // Act
            CompletableFuture<ProcessedData> future = dataAggregator.aggregateAsync("key");

            // Assert
            assertThat(future)
                .failsWithin(Duration.ofSeconds(1))
                .withThrowableOfType(ExecutionException.class)
                .havingCause()
                .isInstanceOf(AggregationException.class)
                .withMessageContaining("Failed to aggregate");
        }

        @Test
        @DisplayName("should mock delayed async response")
        void shouldMockDelayedAsyncResponse() throws Exception {
            // Arrange - Mock returns future that completes after delay
            CompletableFuture<ApiResponse> delayedFuture = CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return new ApiResponse("delayed-data");
            });
            given(apiClient.fetchDataAsync(anyString())).willReturn(delayedFuture);

            // Act
            CompletableFuture<ProcessedData> future = dataAggregator.aggregateAsync("key");

            // Assert - Still completes within timeout
            assertThat(future)
                .succeedsWithin(Duration.ofSeconds(2))
                .extracting(ProcessedData::getValue)
                .isEqualTo("AGGREGATED: delayed-data");
        }
    }

    // ============================================================
    // PATTERN 4: Concurrent Operations Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 4: Concurrent Operations")
    class ConcurrentOperations {

        @Mock
        private ExternalApiClient apiClient;

        @InjectMocks
        private ParallelProcessor parallelProcessor;

        @Test
        @DisplayName("should process multiple items in parallel")
        void shouldProcessMultipleItemsInParallel() throws Exception {
            // Arrange
            given(apiClient.fetchDataAsync("item1"))
                .willReturn(CompletableFuture.completedFuture(new ApiResponse("data1")));
            given(apiClient.fetchDataAsync("item2"))
                .willReturn(CompletableFuture.completedFuture(new ApiResponse("data2")));
            given(apiClient.fetchDataAsync("item3"))
                .willReturn(CompletableFuture.completedFuture(new ApiResponse("data3")));

            // Act
            CompletableFuture<List<ProcessedData>> future =
                parallelProcessor.processAllAsync(List.of("item1", "item2", "item3"));

            // Assert
            List<ProcessedData> results = future.get(2, TimeUnit.SECONDS);
            assertThat(results)
                .hasSize(3)
                .extracting(ProcessedData::getValue)
                .containsExactlyInAnyOrder("data1", "data2", "data3");
        }

        @Test
        @DisplayName("should handle partial failures in parallel processing")
        void shouldHandlePartialFailures() throws Exception {
            // Arrange - One fails, others succeed
            given(apiClient.fetchDataAsync("item1"))
                .willReturn(CompletableFuture.completedFuture(new ApiResponse("data1")));
            given(apiClient.fetchDataAsync("item2"))
                .willReturn(CompletableFuture.failedFuture(new ApiException("Failed")));
            given(apiClient.fetchDataAsync("item3"))
                .willReturn(CompletableFuture.completedFuture(new ApiResponse("data3")));

            // Act
            CompletableFuture<BatchResult> future =
                parallelProcessor.processAllWithPartialFailure(List.of("item1", "item2", "item3"));

            // Assert - Partial results returned
            BatchResult result = future.get(2, TimeUnit.SECONDS);
            assertThat(result.getSuccessful()).hasSize(2);
            assertThat(result.getFailed()).hasSize(1);
        }

        @Test
        @DisplayName("should use allOf for waiting on multiple futures")
        void shouldUseAllOfForMultipleFutures() throws Exception {
            // Arrange
            CompletableFuture<String> future1 = CompletableFuture.completedFuture("result1");
            CompletableFuture<String> future2 = CompletableFuture.completedFuture("result2");
            CompletableFuture<String> future3 = CompletableFuture.completedFuture("result3");

            // Act - Wait for all
            CompletableFuture<Void> allFuture = CompletableFuture.allOf(future1, future2, future3);

            // Assert
            assertThat(allFuture)
                .succeedsWithin(Duration.ofSeconds(1));

            // Access individual results
            assertThat(future1.join()).isEqualTo("result1");
            assertThat(future2.join()).isEqualTo("result2");
            assertThat(future3.join()).isEqualTo("result3");
        }
    }

    // ============================================================
    // PATTERN 5: Timeout Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 5: Timeout Testing")
    class TimeoutTesting {

        @Mock
        private SlowService slowService;

        @InjectMocks
        private TimeoutHandler timeoutHandler;

        @Test
        @DisplayName("should timeout slow operations")
        @Timeout(5)
        void shouldTimeoutSlowOperations() {
            // Arrange - Service never completes
            given(slowService.processAsync(anyString()))
                .willReturn(new CompletableFuture<>()); // Never completes

            // Act
            CompletableFuture<String> future = timeoutHandler.processWithTimeout("data", 100);

            // Assert - Should complete with timeout exception
            assertThat(future)
                .failsWithin(Duration.ofSeconds(1))
                .withThrowableOfType(ExecutionException.class)
                .havingCause()
                .isInstanceOf(TimeoutException.class);
        }

        @Test
        @DisplayName("should complete before timeout")
        void shouldCompleteBeforeTimeout() throws Exception {
            // Arrange - Fast response
            given(slowService.processAsync(anyString()))
                .willReturn(CompletableFuture.completedFuture("quick-result"));

            // Act
            CompletableFuture<String> future = timeoutHandler.processWithTimeout("data", 1000);

            // Assert
            String result = future.get(500, TimeUnit.MILLISECONDS);
            assertThat(result).isEqualTo("quick-result");
        }

        @Test
        @DisplayName("should use orTimeout for timeout handling")
        void shouldUseOrTimeoutForTimeoutHandling() {
            // Act - Future with timeout
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return "slow-result";
                })
                .orTimeout(100, TimeUnit.MILLISECONDS);

            // Assert
            assertThat(future)
                .failsWithin(Duration.ofSeconds(1))
                .withThrowableOfType(ExecutionException.class)
                .havingCause()
                .isInstanceOf(TimeoutException.class);
        }

        @Test
        @DisplayName("should use completeOnTimeout for fallback")
        void shouldUseCompleteOnTimeoutForFallback() throws Exception {
            // Act - Future with timeout fallback
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return "slow-result";
                })
                .completeOnTimeout("default-result", 100, TimeUnit.MILLISECONDS);

            // Assert - Fallback used
            String result = future.get(500, TimeUnit.MILLISECONDS);
            assertThat(result).isEqualTo("default-result");
        }
    }

    // ============================================================
    // PATTERN 6: Race Condition Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 6: Race Condition Testing")
    class RaceConditionTesting {

        @Test
        @DisplayName("should handle concurrent modifications safely")
        void shouldHandleConcurrentModificationsSafely() throws Exception {
            // Arrange
            ConcurrentCounter counter = new ConcurrentCounter();
            ExecutorService executor = Executors.newFixedThreadPool(10);

            // Act - 100 concurrent increments
            List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 100; i++) {
                futures.add(CompletableFuture.runAsync(counter::increment, executor));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);

            // Assert - All increments applied (no race condition)
            assertThat(counter.getValue()).isEqualTo(100);

            executor.shutdown();
        }

        @Test
        @DisplayName("should use anyOf for first-to-complete semantics")
        void shouldUseAnyOfForFirstToComplete() throws Exception {
            // Arrange - Multiple futures, one fast, others slow
            CompletableFuture<String> slow1 = CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return "slow1";
            });
            CompletableFuture<String> fast = CompletableFuture.completedFuture("fast");
            CompletableFuture<String> slow2 = CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return "slow2";
            });

            // Act
            Object result = CompletableFuture.anyOf(slow1, fast, slow2).get(500, TimeUnit.MILLISECONDS);

            // Assert - Fast one wins
            assertThat(result).isEqualTo("fast");
        }
    }

    // ============================================================
    // Helper Classes for Examples
    // ============================================================

    // Calculator
    static class AsyncCalculator {
        CompletableFuture<Integer> calculateAsync(int a, int b) {
            return CompletableFuture.completedFuture(a + b);
        }

        CompletableFuture<String> processAsync(String input) {
            return CompletableFuture.completedFuture("PROCESSED: " + input);
        }

        CompletableFuture<Integer> divideAsync(int a, int b) {
            if (b == 0) {
                return CompletableFuture.failedFuture(new ArithmeticException("Division by zero"));
            }
            return CompletableFuture.completedFuture(a / b);
        }
    }

    // API Client
    interface ExternalApiClient {
        CompletableFuture<ApiResponse> fetchDataAsync(String key);
    }

    static class ApiResponse {
        private final String data;
        ApiResponse(String data) { this.data = data; }
        String getData() { return data; }
    }

    static class ApiException extends RuntimeException {
        ApiException(String message) { super(message); }
    }

    // Data Aggregator
    static class ProcessedData {
        private final String value;
        ProcessedData(String value) { this.value = value; }
        String getValue() { return value; }
    }

    static class AggregationException extends RuntimeException {
        AggregationException(String message, Throwable cause) { super(message, cause); }
    }

    static class DataAggregator {
        private final ExternalApiClient apiClient;
        DataAggregator(ExternalApiClient client) { this.apiClient = client; }

        CompletableFuture<ProcessedData> aggregateAsync(String key) {
            return apiClient.fetchDataAsync(key)
                .thenApply(response -> new ProcessedData("AGGREGATED: " + response.getData()))
                .exceptionally(ex -> { throw new AggregationException("Failed to aggregate", ex); });
        }
    }

    // Parallel Processor
    static class BatchResult {
        private final List<ProcessedData> successful;
        private final List<String> failed;
        BatchResult(List<ProcessedData> successful, List<String> failed) {
            this.successful = successful; this.failed = failed;
        }
        List<ProcessedData> getSuccessful() { return successful; }
        List<String> getFailed() { return failed; }
    }

    static class ParallelProcessor {
        private final ExternalApiClient apiClient;
        ParallelProcessor(ExternalApiClient client) { this.apiClient = client; }

        CompletableFuture<List<ProcessedData>> processAllAsync(List<String> items) {
            List<CompletableFuture<ProcessedData>> futures = items.stream()
                .map(item -> apiClient.fetchDataAsync(item)
                    .thenApply(response -> new ProcessedData(response.getData())))
                .toList();

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
        }

        CompletableFuture<BatchResult> processAllWithPartialFailure(List<String> items) {
            List<ProcessedData> successful = new java.util.ArrayList<>();
            List<String> failed = new java.util.ArrayList<>();

            List<CompletableFuture<Void>> futures = items.stream()
                .map(item -> apiClient.fetchDataAsync(item)
                    .thenAccept(response -> successful.add(new ProcessedData(response.getData())))
                    .exceptionally(ex -> { failed.add(item); return null; }))
                .toList();

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> new BatchResult(successful, failed));
        }
    }

    // Timeout Handler
    interface SlowService {
        CompletableFuture<String> processAsync(String data);
    }

    static class TimeoutHandler {
        private final SlowService slowService;
        TimeoutHandler(SlowService service) { this.slowService = service; }

        CompletableFuture<String> processWithTimeout(String data, long timeoutMs) {
            return slowService.processAsync(data)
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        }
    }

    // Concurrent Counter
    static class ConcurrentCounter {
        private final AtomicInteger count = new AtomicInteger(0);

        void increment() {
            count.incrementAndGet();
        }

        int getValue() {
            return count.get();
        }
    }
}

/**
 * SUMMARY OF ASYNC TESTING PATTERNS:
 *
 * 1. Use @Timeout to fail fast
 * 2. Use get(timeout) instead of get()
 * 3. Use assertj succeedsWithin/failsWithin
 * 4. Mock async services with completedFuture/failedFuture
 * 5. Test concurrent operations with allOf/anyOf
 * 6. Use orTimeout for timeout handling
 * 7. Use completeOnTimeout for fallback values
 * 8. Test race conditions with proper synchronization
 */
