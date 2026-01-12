package com.example.lab.repository;

import com.example.lab.model.entity.Payment;
import com.example.lab.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByUserId(String userId);

    List<Payment> findByOrderId(String orderId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status);
}
