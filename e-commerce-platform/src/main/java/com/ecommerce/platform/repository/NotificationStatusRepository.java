package com.ecommerce.platform.repository;

import com.ecommerce.platform.domain.NotificationStatusRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationStatusRepository extends JpaRepository<NotificationStatusRecord, Long> {
    Optional<NotificationStatusRecord> findByOrderId(Long orderId);
}
