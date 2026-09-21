package com.microfinance.service.impl;

import com.microfinance.dto.request.ChangePasswordRequest;
import com.microfinance.dto.request.ChangeUsernameRequest;
import com.microfinance.dto.request.LoginRequest;
import com.microfinance.dto.response.LoginResponse;
import com.microfinance.entity.Admin;
import com.microfinance.exception.BadRequestException;
import com.microfinance.exception.DuplicateResourceException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.repository.AdminRepository;
import com.microfinance.security.JwtUtil;
import com.microfinance.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final AdminRepository adminRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalStateException("Admin disappeared after successful authentication"));

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(admin.getUsername())
                .fullName(admin.getFullName())
                .role(admin.getRole().name())
                .expiresIn(jwtUtil.getExpirationMs())
                .build();
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        adminRepository.save(admin);
    }

    @Override
    @Transactional
    public LoginResponse changeUsername(String username, ChangeUsernameRequest request) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        String newUsername = request.getNewUsername().trim();

        if (newUsername.equalsIgnoreCase(admin.getUsername())) {
            throw new BadRequestException("New username must be different from the current username");
        }

        if (adminRepository.existsByUsername(newUsername)) {
            throw new DuplicateResourceException("Username '" + newUsername + "' is already taken");
        }

        admin.setUsername(newUsername);
        adminRepository.save(admin);

        // The old JWT's subject (previous username) no longer resolves to this
        // account, so issue a fresh token here to keep the caller logged in.
        UserDetails userDetails = userDetailsService.loadUserByUsername(newUsername);
        String token = jwtUtil.generateToken(userDetails);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(admin.getUsername())
                .fullName(admin.getFullName())
                .role(admin.getRole().name())
                .expiresIn(jwtUtil.getExpirationMs())
                .build();
    }
}
