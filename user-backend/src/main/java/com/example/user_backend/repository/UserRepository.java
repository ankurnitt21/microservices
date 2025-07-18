package com.example.user_backend.repository;

import com.example.user_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNameIgnoreCase(String name);
}
