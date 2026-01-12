package com.example.lab.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Order order;

    @Column(nullable = false)
    private String productId;

    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Builder.Default
    private int discount = 0;

    public BigDecimal getTotal() {
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
            BigDecimal.valueOf(discount).divide(BigDecimal.valueOf(100))
        );
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).multiply(discountMultiplier);
    }
}
