package com.shivamingale.ecom.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivamingale.ecom.dto.request.UpdateAdminUserDto;
import com.shivamingale.ecom.dto.response.AppResponse;
import com.shivamingale.ecom.security.AdminPrincipal;
import com.shivamingale.ecom.service.AdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/me")
    public ResponseEntity<AppResponse<AdminInfo>> me(@AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseEntity.ok(AppResponse.success(
                new AdminInfo(principal.getId(), principal.getName(), principal.getEmail(), principal.getMobile()),
                "Admin profile", org.springframework.http.HttpStatus.OK));
    }

    public record AdminInfo(String id, String name, String email, String mobile) {
    }

    @PatchMapping("")
    public ResponseEntity<AppResponse<Void>> updateProfile(@AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody UpdateAdminUserDto updateAdminUserDto) {
        adminService.updateProfile(principal.getId(), updateAdminUserDto);
        return ResponseEntity.ok(AppResponse.success(null, "Admin profile updated successfully",
                org.springframework.http.HttpStatus.OK));
    }
}