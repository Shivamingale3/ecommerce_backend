package com.shivamingale.invoice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivamingale.invoice.entity.SignInRequest;

public interface SignInRequestRepository extends JpaRepository<SignInRequest, String> {

    Optional<SignInRequest> findByEmail(String email);
}