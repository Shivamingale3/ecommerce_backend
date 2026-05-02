package com.shivamingale.ecom.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.shivamingale.ecom.dto.request.UpdateAdminUserDto;
import com.shivamingale.ecom.entity.Admin;
import com.shivamingale.ecom.exception.AppException;
import com.shivamingale.ecom.repository.AdminRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    public void updateProfile(String adminId, UpdateAdminUserDto updateAdminUserDto) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Admin not found!"));
        if (updateAdminUserDto.getName() != null && !updateAdminUserDto.getName().equals(admin.getName())) {
            admin.setName(updateAdminUserDto.getName());
        }
        if (updateAdminUserDto.getMobile() != null && !updateAdminUserDto.getMobile().equals(admin.getMobile())) {
            admin.setMobile(updateAdminUserDto.getMobile());
        }
        adminRepository.save(admin);
    }
}
