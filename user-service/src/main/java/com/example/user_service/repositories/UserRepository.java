package com.example.user_service.repositories;

import com.example.user_service.entities.users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<users, UUID> {

    Optional<users> findByUsername(String username);

    Optional<users> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
