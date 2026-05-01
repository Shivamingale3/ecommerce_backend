package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.SignInRequest;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface SignInRequestRepository extends BaseRepository<SignInRequest> {

    Optional<SignInRequest> findByEmailAndDeletedFalse(String email);
}