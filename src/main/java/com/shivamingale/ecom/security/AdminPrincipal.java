package com.shivamingale.ecom.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.shivamingale.ecom.entity.Admin;
import lombok.Getter;

@Getter
public class AdminPrincipal implements UserDetails {
    private final String id;
    private final String name;
    private final String email;
    private final String mobile;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public AdminPrincipal(Admin admin) {
        this.id = admin.getId();
        this.name = admin.getName();
        this.email = admin.getEmail();
        this.mobile = admin.getMobile();
        this.enabled = admin.isEnabled();
        this.authorities = java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public String getPassword() { return ""; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
}