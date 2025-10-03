package com.example.user_backend.controller;

import com.example.user_backend.dto.UserRequest;
import com.example.user_backend.dto.UserResponse;
import com.example.user_backend.model.User;
import com.example.user_backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Page<UserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        log.info("Received request to get all users page={}, size={}, sort={}", page, size, sort);
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortBy = sortParts[0];
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return userService.findAll(pageable).map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(u -> ResponseEntity.ok(new UserResponse(u.getId(), u.getName(), u.getEmail())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<UserResponse> getUserByName(@PathVariable String name) {
        return userService.findByName(name)
                .map(u -> ResponseEntity.ok(new UserResponse(u.getId(), u.getName(), u.getEmail())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserResponse> addUser(@Validated @RequestBody UserRequest request) {
        User saved = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(saved.getId(), saved.getName(), saved.getEmail()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Validated @RequestBody UserRequest request) {
        return userService.update(id, request)
                .map(u -> ResponseEntity.ok(new UserResponse(u.getId(), u.getName(), u.getEmail())))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteById(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
