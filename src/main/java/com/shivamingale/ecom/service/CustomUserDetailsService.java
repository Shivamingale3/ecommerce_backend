package com.shivamingale.ecom.service;

import com.shivamingale.ecom.entity.User;
import com.shivamingale.ecom.repository.UserRepository;
import com.shivamingale.ecom.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        @Transactional(readOnly = true)
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                User user = userRepository
                                .findByEmail(email)
                                .orElseThrow(
                                                () -> new UsernameNotFoundException(
                                                                "User not found with email: " + email));
                return new UserPrincipal(user);
        }
}
