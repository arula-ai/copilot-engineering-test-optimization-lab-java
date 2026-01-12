/**
 * GOLDEN EXAMPLE: Repository/Database Testing with Spring Data JPA
 *
 * PATTERN: Database Layer Testing
 *
 * WHEN TO USE:
 * - Testing repository query methods
 * - Testing custom JPQL/native queries
 * - Verifying entity relationships
 * - Testing database constraints
 *
 * ANTI-PATTERNS THIS SOLVES:
 * ❌ Using production database in tests
 * ❌ Not testing query edge cases
 * ❌ Missing transaction rollback
 * ❌ Not flushing persistence context
 *
 * KEY PRINCIPLES:
 * 1. Use @DataJpaTest for slice testing
 * 2. Use @Transactional for test isolation
 * 3. Flush and clear persistence context when needed
 * 4. Test both positive and negative scenarios
 */

package com.example.lab.golden;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Demonstrates repository testing patterns for the Test Optimization Lab.
 *
 * NOTE: This is a template - actual execution requires Spring Boot context.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Repository Testing Golden Examples")
class RepositoryPatternsGoldenTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    // ============================================================
    // PATTERN 1: Basic CRUD Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 1: Basic CRUD Operations")
    class BasicCrudOperations {

        @Test
        @DisplayName("should save and retrieve entity")
        void shouldSaveAndRetrieveEntity() {
            // Arrange
            Product product = new Product("SKU-001", "Test Product", new BigDecimal("29.99"));

            // Act
            Product saved = productRepository.save(product);

            // Flush to ensure persistence
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<Product> found = productRepository.findById(saved.getId());
            assertThat(found)
                .isPresent()
                .hasValueSatisfying(p -> {
                    assertThat(p.getSku()).isEqualTo("SKU-001");
                    assertThat(p.getName()).isEqualTo("Test Product");
                    assertThat(p.getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
                });
        }

        @Test
        @DisplayName("should update existing entity")
        void shouldUpdateExistingEntity() {
            // Arrange
            Product product = entityManager.persist(
                new Product("SKU-001", "Original Name", new BigDecimal("10.00"))
            );
            entityManager.flush();
            entityManager.clear();

            // Act
            Product toUpdate = productRepository.findById(product.getId()).orElseThrow();
            toUpdate.setName("Updated Name");
            toUpdate.setPrice(new BigDecimal("20.00"));
            productRepository.save(toUpdate);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Product updated = productRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getName()).isEqualTo("Updated Name");
            assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("should delete entity")
        void shouldDeleteEntity() {
            // Arrange
            Product product = entityManager.persist(
                new Product("SKU-001", "To Delete", new BigDecimal("10.00"))
            );
            Long productId = product.getId();
            entityManager.flush();
            entityManager.clear();

            // Act
            productRepository.deleteById(productId);
            entityManager.flush();

            // Assert
            assertThat(productRepository.findById(productId)).isEmpty();
        }

        @Test
        @DisplayName("should return empty for non-existent ID")
        void shouldReturnEmptyForNonExistentId() {
            // Act
            Optional<Product> result = productRepository.findById(999999L);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // ============================================================
    // PATTERN 2: Custom Query Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 2: Custom Query Methods")
    class CustomQueryMethods {

        @BeforeEach
        void setUp() {
            // Set up test data
            entityManager.persist(new Product("SKU-A01", "Widget A", new BigDecimal("10.00"), "ACTIVE"));
            entityManager.persist(new Product("SKU-A02", "Widget B", new BigDecimal("20.00"), "ACTIVE"));
            entityManager.persist(new Product("SKU-B01", "Gadget A", new BigDecimal("30.00"), "ACTIVE"));
            entityManager.persist(new Product("SKU-C01", "Tool A", new BigDecimal("40.00"), "INACTIVE"));
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("should find by SKU")
        void shouldFindBySku() {
            // Act
            Optional<Product> result = productRepository.findBySku("SKU-A01");

            // Assert
            assertThat(result)
                .isPresent()
                .hasValueSatisfying(p -> assertThat(p.getName()).isEqualTo("Widget A"));
        }

        @Test
        @DisplayName("should find all by status")
        void shouldFindAllByStatus() {
            // Act
            List<Product> activeProducts = productRepository.findAllByStatus("ACTIVE");

            // Assert
            assertThat(activeProducts)
                .hasSize(3)
                .extracting(Product::getStatus)
                .containsOnly("ACTIVE");
        }

        @Test
        @DisplayName("should find by SKU prefix")
        void shouldFindBySkuPrefix() {
            // Act
            List<Product> productsWithPrefixA = productRepository.findBySkuStartingWith("SKU-A");

            // Assert
            assertThat(productsWithPrefixA)
                .hasSize(2)
                .extracting(Product::getSku)
                .allMatch(sku -> sku.startsWith("SKU-A"));
        }

        @Test
        @DisplayName("should find by price range")
        void shouldFindByPriceRange() {
            // Act
            List<Product> midRangeProducts = productRepository.findByPriceBetween(
                new BigDecimal("15.00"),
                new BigDecimal("35.00")
            );

            // Assert
            assertThat(midRangeProducts)
                .hasSize(2)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Widget B", "Gadget A");
        }

        @Test
        @DisplayName("should search by name containing")
        void shouldSearchByNameContaining() {
            // Act
            List<Product> widgets = productRepository.findByNameContainingIgnoreCase("widget");

            // Assert
            assertThat(widgets)
                .hasSize(2)
                .extracting(Product::getName)
                .allMatch(name -> name.toLowerCase().contains("widget"));
        }
    }

    // ============================================================
    // PATTERN 3: Pagination and Sorting
    // ============================================================
    @Nested
    @DisplayName("Pattern 3: Pagination and Sorting")
    class PaginationAndSorting {

        @BeforeEach
        void setUp() {
            for (int i = 1; i <= 25; i++) {
                entityManager.persist(new Product(
                    "SKU-" + String.format("%03d", i),
                    "Product " + i,
                    new BigDecimal(i * 10),
                    "ACTIVE"
                ));
            }
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("should return paginated results")
        void shouldReturnPaginatedResults() {
            // Arrange
            PageRequest pageRequest = PageRequest.of(0, 10);

            // Act
            Page<Product> page = productRepository.findAll(pageRequest);

            // Assert
            assertThat(page.getContent()).hasSize(10);
            assertThat(page.getTotalElements()).isEqualTo(25);
            assertThat(page.getTotalPages()).isEqualTo(3);
            assertThat(page.isFirst()).isTrue();
            assertThat(page.hasNext()).isTrue();
        }

        @Test
        @DisplayName("should return sorted results")
        void shouldReturnSortedResults() {
            // Arrange
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "price"));

            // Act
            Page<Product> page = productRepository.findAll(pageRequest);

            // Assert
            assertThat(page.getContent())
                .extracting(Product::getPrice)
                .isSortedAccordingTo((a, b) -> b.compareTo(a)); // Descending
        }

        @Test
        @DisplayName("should handle last page correctly")
        void shouldHandleLastPageCorrectly() {
            // Arrange
            PageRequest lastPage = PageRequest.of(2, 10); // Page 3 of 3

            // Act
            Page<Product> page = productRepository.findAll(lastPage);

            // Assert
            assertThat(page.getContent()).hasSize(5); // Only 5 items on last page
            assertThat(page.isLast()).isTrue();
            assertThat(page.hasNext()).isFalse();
        }

        @Test
        @DisplayName("should return empty page for out of range")
        void shouldReturnEmptyPageForOutOfRange() {
            // Arrange
            PageRequest outOfRange = PageRequest.of(100, 10);

            // Act
            Page<Product> page = productRepository.findAll(outOfRange);

            // Assert
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isEqualTo(25); // Total unchanged
        }
    }

    // ============================================================
    // PATTERN 4: Relationship Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 4: Entity Relationships")
    class EntityRelationships {

        @Test
        @DisplayName("should persist order with items (One-to-Many)")
        void shouldPersistOrderWithItems() {
            // Arrange
            Order order = new Order("ORD-001", "PENDING");
            order.addItem(new OrderItem("SKU-A", 2, new BigDecimal("10.00")));
            order.addItem(new OrderItem("SKU-B", 1, new BigDecimal("20.00")));

            // Act
            Order saved = orderRepository.save(order);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Order found = orderRepository.findById(saved.getId()).orElseThrow();
            assertThat(found.getItems()).hasSize(2);
            assertThat(found.getItems())
                .extracting(OrderItem::getSku)
                .containsExactlyInAnyOrder("SKU-A", "SKU-B");
        }

        @Test
        @DisplayName("should cascade delete to children")
        void shouldCascadeDeleteToChildren() {
            // Arrange
            Order order = new Order("ORD-001", "PENDING");
            order.addItem(new OrderItem("SKU-A", 1, new BigDecimal("10.00")));
            Order saved = orderRepository.save(order);
            entityManager.flush();
            entityManager.clear();

            // Act
            orderRepository.deleteById(saved.getId());
            entityManager.flush();

            // Assert - Order and items deleted
            assertThat(orderRepository.findById(saved.getId())).isEmpty();
            // Note: Would need to verify items are also deleted if testing cascade
        }

        @Test
        @DisplayName("should fetch lazy association")
        void shouldFetchLazyAssociation() {
            // Arrange
            Order order = new Order("ORD-001", "PENDING");
            order.addItem(new OrderItem("SKU-A", 2, new BigDecimal("10.00")));
            orderRepository.save(order);
            entityManager.flush();
            entityManager.clear();

            // Act - Fetch with items (eager or join fetch)
            Order found = orderRepository.findByIdWithItems(order.getId()).orElseThrow();

            // Assert - Items loaded
            assertThat(found.getItems()).hasSize(1);
        }
    }

    // ============================================================
    // PATTERN 5: Native Query and JPQL Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 5: Native and JPQL Queries")
    class NativeAndJpqlQueries {

        @BeforeEach
        void setUp() {
            entityManager.persist(new Product("SKU-001", "Active Product", new BigDecimal("50.00"), "ACTIVE"));
            entityManager.persist(new Product("SKU-002", "Inactive Product", new BigDecimal("30.00"), "INACTIVE"));
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("should execute JPQL query")
        void shouldExecuteJpqlQuery() {
            // Act
            List<Product> result = productRepository.findActiveProductsByMinPrice(new BigDecimal("40.00"));

            // Assert
            assertThat(result)
                .hasSize(1)
                .extracting(Product::getSku)
                .containsExactly("SKU-001");
        }

        @Test
        @DisplayName("should execute native query")
        void shouldExecuteNativeQuery() {
            // Act
            long count = productRepository.countByStatusNative("ACTIVE");

            // Assert
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("should project to DTO")
        void shouldProjectToDto() {
            // Act
            List<ProductSummary> summaries = productRepository.findAllSummaries();

            // Assert
            assertThat(summaries)
                .hasSize(2)
                .extracting(ProductSummary::getSku)
                .containsExactlyInAnyOrder("SKU-001", "SKU-002");
        }
    }

    // ============================================================
    // PATTERN 6: Constraint and Validation Testing
    // ============================================================
    @Nested
    @DisplayName("Pattern 6: Constraints and Validation")
    class ConstraintsAndValidation {

        @Test
        @DisplayName("should enforce unique constraint")
        void shouldEnforceUniqueConstraint() {
            // Arrange
            entityManager.persist(new Product("SKU-001", "Product 1", new BigDecimal("10.00")));
            entityManager.flush();

            // Act & Assert
            Product duplicate = new Product("SKU-001", "Product 2", new BigDecimal("20.00"));

            assertThatThrownBy(() -> {
                productRepository.save(duplicate);
                entityManager.flush();
            }).hasCauseInstanceOf(Exception.class); // ConstraintViolationException
        }

        @Test
        @DisplayName("should enforce not null constraint")
        void shouldEnforceNotNullConstraint() {
            // Arrange
            Product invalidProduct = new Product(null, "No SKU", new BigDecimal("10.00"));

            // Act & Assert
            assertThatThrownBy(() -> {
                productRepository.save(invalidProduct);
                entityManager.flush();
            }).hasCauseInstanceOf(Exception.class); // ConstraintViolationException
        }

        @Test
        @DisplayName("should set audit fields automatically")
        void shouldSetAuditFieldsAutomatically() {
            // Arrange
            Product product = new Product("SKU-001", "Audited Product", new BigDecimal("10.00"));

            // Act
            Product saved = productRepository.save(product);
            entityManager.flush();
            entityManager.clear();

            // Assert - Assuming @PrePersist sets createdAt
            Product found = productRepository.findById(saved.getId()).orElseThrow();
            assertThat(found.getCreatedAt()).isNotNull();
            assertThat(found.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    // ============================================================
    // Helper Classes (Simulated Entities and Repositories)
    // ============================================================

    // These would be actual @Entity classes in a real project
    static class Product {
        private Long id;
        private String sku;
        private String name;
        private BigDecimal price;
        private String status;
        private LocalDateTime createdAt;

        Product() {}
        Product(String sku, String name, BigDecimal price) {
            this.sku = sku; this.name = name; this.price = price; this.status = "ACTIVE";
        }
        Product(String sku, String name, BigDecimal price, String status) {
            this.sku = sku; this.name = name; this.price = price; this.status = status;
        }

        Long getId() { return id; }
        String getSku() { return sku; }
        String getName() { return name; }
        BigDecimal getPrice() { return price; }
        String getStatus() { return status; }
        LocalDateTime getCreatedAt() { return createdAt; }

        void setName(String name) { this.name = name; }
        void setPrice(BigDecimal price) { this.price = price; }
    }

    static class Order {
        private Long id;
        private String orderNumber;
        private String status;
        private List<OrderItem> items = new java.util.ArrayList<>();

        Order() {}
        Order(String orderNumber, String status) {
            this.orderNumber = orderNumber; this.status = status;
        }

        Long getId() { return id; }
        String getOrderNumber() { return orderNumber; }
        List<OrderItem> getItems() { return items; }

        void addItem(OrderItem item) {
            items.add(item);
            item.setOrder(this);
        }
    }

    static class OrderItem {
        private Long id;
        private String sku;
        private int quantity;
        private BigDecimal price;
        private Order order;

        OrderItem() {}
        OrderItem(String sku, int quantity, BigDecimal price) {
            this.sku = sku; this.quantity = quantity; this.price = price;
        }

        String getSku() { return sku; }
        int getQuantity() { return quantity; }

        void setOrder(Order order) { this.order = order; }
    }

    interface ProductSummary {
        String getSku();
        String getName();
    }

    // Repository interfaces (would extend JpaRepository in real project)
    interface ProductRepository {
        Product save(Product product);
        Optional<Product> findById(Long id);
        void deleteById(Long id);
        Page<Product> findAll(PageRequest pageRequest);
        Optional<Product> findBySku(String sku);
        List<Product> findAllByStatus(String status);
        List<Product> findBySkuStartingWith(String prefix);
        List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);
        List<Product> findByNameContainingIgnoreCase(String name);
        List<Product> findActiveProductsByMinPrice(BigDecimal minPrice);
        long countByStatusNative(String status);
        List<ProductSummary> findAllSummaries();
    }

    interface OrderRepository {
        Order save(Order order);
        Optional<Order> findById(Long id);
        void deleteById(Long id);
        Optional<Order> findByIdWithItems(Long id);
    }
}

/**
 * SUMMARY OF REPOSITORY TESTING PATTERNS:
 *
 * 1. Use @DataJpaTest for slice testing
 * 2. Use TestEntityManager for test data setup
 * 3. Always flush() after save to trigger constraints
 * 4. Clear persistence context before assertions
 * 5. Test all CRUD operations
 * 6. Test custom queries with various inputs
 * 7. Test pagination boundaries
 * 8. Test entity relationships and cascades
 * 9. Test unique and not-null constraints
 * 10. Test audit fields and lifecycle callbacks
 */
