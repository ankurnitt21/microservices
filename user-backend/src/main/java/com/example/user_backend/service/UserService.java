package com.example.user_backend.service;

import com.example.user_backend.dto.UserRequest;
import com.example.user_backend.model.User;
import com.example.user_backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByName(String name) {
        return userRepository.findByNameIgnoreCase(name);
    }

    @Transactional
    public User create(UserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalStateException("email_already_exists");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        return userRepository.save(user);
    }

    @Transactional
    public Optional<User> update(Long id, UserRequest request) {
        return userRepository.findById(id).map(existing -> {
            if (request.getEmail() != null &&
                    userRepository.existsByEmailIgnoreCaseAndIdNot(request.getEmail(), id)) {
                throw new IllegalStateException("email_already_exists");
            }
            if (request.getName() != null) existing.setName(request.getName());
            if (request.getEmail() != null) existing.setEmail(request.getEmail());
            return userRepository.save(existing);
        });
    }

    @Transactional
    public boolean deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }
}


