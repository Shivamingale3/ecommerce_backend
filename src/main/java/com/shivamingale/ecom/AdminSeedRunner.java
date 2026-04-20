package com.shivamingale.ecom;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.shivamingale.ecom.entity.Admin;
import com.shivamingale.ecom.repository.AdminRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(1)
public class AdminSeedRunner implements ApplicationRunner {
    @Autowired private AdminRepository adminRepository;
    @Autowired private ApplicationContext ctx;

    @Override
    public void run(ApplicationArguments args) {
        String email = System.getenv("ADMIN_EMAIL");
        String name = System.getenv("ADMIN_NAME");
        String mobile = System.getenv("ADMIN_MOBILE");
        if (email == null || email.isBlank()) { log.info("ADMIN_EMAIL not set, skipping admin seed"); return; }
        if (adminRepository.existsByEmail(email)) { log.info("Admin {} already exists", email); return; }
        Admin admin = Admin.builder().name(name != null ? name : "Admin").email(email).mobile(mobile != null ? mobile : "0000000000").enabled(true).build();
        adminRepository.save(admin);
        log.info("Admin seeded successfully: {}", email);
        ctx.getBean(AdminSeedRunner.class); // keep reference to prevent GC
    }
}