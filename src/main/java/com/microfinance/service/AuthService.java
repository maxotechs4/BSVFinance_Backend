package com.microfinance.service;

import com.microfinance.dto.request.ChangePasswordRequest;
import com.microfinance.dto.request.ChangeUsernameRequest;
import com.microfinance.dto.request.LoginRequest;
import com.microfinance.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    /** Verifies currentPassword before updating to newPassword for the given username. */
    void changePassword(String username, ChangePasswordRequest request);

    /**
     * Verifies currentPassword before renaming the account to newUsername.
     * Returns a fresh LoginResponse (new JWT) since the old token's subject
     * (the previous username) is no longer valid for this account.
     */
    LoginResponse changeUsername(String username, ChangeUsernameRequest request);
}
