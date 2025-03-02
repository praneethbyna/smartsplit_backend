package com.smartsplit.backend.controller;

import com.smartsplit.backend.dto.UserDTO;
import com.smartsplit.backend.exception.AlreadyExistsException;
import com.smartsplit.backend.exception.ResourceNotFoundException;
import com.smartsplit.backend.model.User;
import com.smartsplit.backend.request.AddUserRequest;
import com.smartsplit.backend.response.ApiResponse;
import com.smartsplit.backend.service.IUserService;
import com.smartsplit.backend.service.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final IUserService userService;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private String getLoggedInUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@RequestBody @Valid AddUserRequest request) {
        try{
            User user = userService.registerUser(request);
            return ResponseEntity.ok(new ApiResponse("User registered successfully. Please check your email to verify your account and login", user));
        }catch (AlreadyExistsException e){
            throw new AlreadyExistsException("User already exists");
        }

    }


    @GetMapping("/verify")
    public ResponseEntity<ApiResponse> verifyUser(@RequestParam String token) {
        logger.info("Verification API called with token: {}", token); // Debug log
        try {
            User user = userService.verifyUser(token);
            return ResponseEntity.ok(new ApiResponse("Account verified successfully!", user));
        } catch (Exception e) {
            logger.error("Verification failed: {}", e.getMessage()); // Log the error
            throw new RuntimeException(e.getMessage());
        }
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse> loginUser(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");
        logger.info("Login API called for email: {}", email);
        try {
            String token = userService.loginUser(email, password);
            return ResponseEntity.ok(new ApiResponse("Login successful", token));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }


    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            userService.resetPassword(email);
            return ResponseEntity.ok(new ApiResponse("Password reset link sent to email", null));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }



    @PostMapping("/verify-password-reset-token")
    public ResponseEntity<ApiResponse> verifyPasswordResetToken(@RequestParam String token) {
        logger.info("Verification token API called with token: {}", token); // Log the incoming token
        try {
            userService.verifyPasswordResetToken(token);
            logger.info("Token verified successfully for token: {}", token); // Log successful verification
            return ResponseEntity.ok(new ApiResponse("Token verified", null));
        } catch (ResourceNotFoundException e) {
            logger.error("Verification token failed: {}", e.getMessage()); // Log the error message
            throw new ResourceNotFoundException(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Token verification runtime exception: {}", e.getMessage()); // Log runtime exception
            throw new RuntimeException(e.getMessage());
        }
    }


    @PostMapping("/update-password")
    public ResponseEntity<ApiResponse> updatePassword(@RequestParam String token, @RequestParam String newPassword){
        try{
            userService.updatePassword(token, newPassword);
            return ResponseEntity.ok(new ApiResponse("Password updated successfully, please login to continue!!", null));
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }

    @PutMapping("/update-profile")
    public ResponseEntity<ApiResponse> updateUserProfile(@RequestBody @Valid UserDTO userDTO) {
        try{
            User updatedUser = userService.updateUserProfile(userDTO);
            return ResponseEntity.ok(new ApiResponse("User Profile updated successfully", updatedUser));
        }catch (ResourceNotFoundException e){
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse> getUserProfile() {
        try {
            String email = getLoggedInUserEmail(); // Extract logged-in user's email from JWT
            UserDTO userDTO = userService.getUserProfile(email);
            return ResponseEntity.ok(new ApiResponse("User profile retrieved successfully", userDTO));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }



}
