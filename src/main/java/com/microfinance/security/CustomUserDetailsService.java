package com.microfinance.security;

import com.microfinance.entity.Admin;
import com.microfinance.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No admin found with username: " + username));

        return User.builder()
                .username(admin.getUsername())
                .password(admin.getPassword())
                .disabled(!admin.isEnabled())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name())))
                .build();
    }
}
