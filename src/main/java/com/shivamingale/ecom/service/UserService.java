package com.shivamingale.ecom.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shivamingale.ecom.entity.User;
import com.shivamingale.ecom.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User registerNewUser(String email, String firstName, String lastName) {
        return userRepository.save(User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .accountNonLocked(true)
                .enabled(true)
                .build());
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }
}
