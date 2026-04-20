package com.shivamingale.ecom.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.shivamingale.ecom.dto.response.AppResponse;
import com.shivamingale.ecom.security.AdminPrincipal;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    @GetMapping("/me")
    public ResponseEntity<AppResponse<AdminInfo>> me(@AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseEntity.ok(AppResponse.success(new AdminInfo(principal.getId(), principal.getName(), principal.getEmail(), principal.getMobile()), "Admin profile", org.springframework.http.HttpStatus.OK));
    }

    public record AdminInfo(String id, String name, String email, String mobile) {}
}