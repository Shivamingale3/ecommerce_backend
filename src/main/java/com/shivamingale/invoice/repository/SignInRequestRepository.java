package com.shivamingale.invoice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivamingale.invoice.entity.SignInRequest;

public interface SignInRequestRepository extends JpaRepository<SignInRequest, String> {

}