package com.shivamingale.ecom.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivamingale.ecom.entity.SignInRequest;

public interface SignInRequestRepository extends JpaRepository<SignInRequest, String> {

    Optional<SignInRequest> findByEmail(String email);
}