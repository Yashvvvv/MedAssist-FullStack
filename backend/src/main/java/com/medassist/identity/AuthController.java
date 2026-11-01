package com.medassist.identity;

import com.medassist.identity.dto.AuthDtos.AuthResponse;
import com.medassist.identity.dto.AuthDtos.ChangePasswordRequest;
import com.medassist.identity.dto.AuthDtos.EmailRequest;
import com.medassist.identity.dto.AuthDtos.LoginRequest;
import com.medassist.identity.dto.AuthDtos.MeResponse;
import com.medassist.identity.dto.AuthDtos.RefreshRequest;
import com.medassist.identity.dto.AuthDtos.RegisterPharmacyRequest;
import com.medassist.identity.dto.AuthDtos.RegisterRequest;
import com.medassist.identity.dto.AuthDtos.ResetPasswordRequest;
import com.medassist.identity.dto.AuthDtos.SwitchOrgRequest;
import com.medassist.identity.dto.AuthDtos.UpdateProfileRequest;
import com.medassist.identity.dto.AuthDtos.UserView;
import com.medassist.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Accounts, sessions, org switching")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @Operation(summary = "Sign up as a consumer (patient)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req,
                                 @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String ua) {
        return auth.register(req, ua);
    }

    @Operation(summary = "Sign up a pharmacy: owner account + organization + first branch + 14-day PRO trial")
    @PostMapping("/register-pharmacy")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerPharmacy(@Valid @RequestBody RegisterPharmacyRequest req,
                                         @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String ua) {
        return auth.registerPharmacy(req, ua);
    }

    @Operation(summary = "Log in. With exactly one org membership the session is scoped to it.")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req,
                              @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String ua) {
        return auth.login(req, ua);
    }

    @Operation(summary = "Rotate the refresh token and get a new access token")
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req,
                                @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String ua) {
        return auth.refresh(req.refreshToken(), ua);
    }

    @Operation(summary = "Revoke this session (the refresh token's whole family)")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req.refreshToken());
    }

    @Operation(summary = "Scope the session to another org you belong to (orgId null = personal)")
    @PostMapping("/switch-org")
    public AuthResponse switchOrg(CurrentUser user, @RequestBody SwitchOrgRequest req,
                                  @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String ua) {
        return auth.switchOrg(user, req.orgId(), ua);
    }

    @GetMapping("/me")
    public MeResponse me(CurrentUser user) {
        return auth.me(user);
    }

    @PutMapping("/me")
    public UserView updateProfile(CurrentUser user, @Valid @RequestBody UpdateProfileRequest req) {
        return auth.updateProfile(user, req);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(CurrentUser user, @Valid @RequestBody ChangePasswordRequest req) {
        auth.changePassword(user, req);
    }

    @Operation(summary = "Email a password-reset link (always 202, even for unknown emails)")
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody EmailRequest req) {
        auth.forgotPassword(req.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        auth.resetPassword(req);
    }

    @Operation(summary = "Confirm an email address (the link in the verification email)")
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token) {
        auth.verifyEmail(token);
        return "Email verified. You can close this page and return to MedAssist.";
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(CurrentUser user) {
        auth.resendVerification(user);
    }
}
