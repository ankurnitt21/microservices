package com.example.user_backend.controller;

import com.example.user_backend.model.User;
import com.example.user_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserRepository userRepository;


    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<User> getAllUsers() {
        logger.info("This log is here because i'm testing ci/cd pipeline");
        logger.info("Received request to get all users");
        List<User> users = userRepository.findAll();
        logger.debug("Fetched {} users from database", users.size());
        return users;
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<User> getUserByName(@PathVariable String name) {
        logger.info("Received request to get user by name: {}", name);
        return userRepository.findByNameIgnoreCase(name)
                .map(user -> {
                    logger.debug("User found: {}", user);
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    logger.warn("User with name '{}' not found", name);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody User user) {
        logger.info("Received request to add new user: {}", user);
        User savedUser = userRepository.save(user);
        logger.debug("User saved: {}", savedUser);
        return ResponseEntity.ok(savedUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        log.info("Request to update user with ID: {}", id);
        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isEmpty()) {
            log.warn("Cannot update. User with ID: {} not found.", id);
            return ResponseEntity.notFound().build();
        }

        User existingUser = optionalUser.get();
        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());

        User updatedUser = userRepository.save(existingUser);
        log.info("Successfully updated user with ID: {}", updatedUser.getId());
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Request to delete user with ID: {}", id);
        if (!userRepository.existsById(id)) {
            log.warn("Cannot delete. User with ID: {} not found.", id);
            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(id);
        log.info("Successfully deleted user with ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}
