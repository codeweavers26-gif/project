package com.project.backend.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.IdempotencyKey;

@Repository
public interface IdempotencyKeyRepository  extends JpaRepository<IdempotencyKey, Long>  {
Optional<IdempotencyKey> findByIdempotencyKey(String key);
}