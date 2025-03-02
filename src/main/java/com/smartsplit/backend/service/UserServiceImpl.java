package com.smartsplit.backend.service;

import com.smartsplit.backend.dto.UserDTO;
import com.smartsplit.backend.exception.AlreadyExistsException;
import com.smartsplit.backend.exception.ResourceNotFoundException;
import com.smartsplit.backend.model.User;
import com.smartsplit.backend.model.Group;

import com.smartsplit.backend.repository.UserRepository;
import com.smartsplit.backend.request.AddUserRequest;
import com.smartsplit.backend.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserServiceImpl implements IUserService{
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final IEmailService emailService;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder, IEmailService emailService, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public User registerUser(AddUserRequest addUserRequest) {

        logger.info("Starting user registration for email: {}", addUserRequest.getEmail());

        if (userRepository.findByEmail(addUserRequest.getEmail()).isPresent()) {
            logger.warn("User with email {} already exists", addUserRequest.getEmail());
            throw new AlreadyExistsException("User with this email already exists");
        }

        User user = new User();
        user.setUsername(addUserRequest.getUsername());
        user.setPassword(bCryptPasswordEncoder.encode(addUserRequest.getPassword()));
        user.setEmail(addUserRequest.getEmail());
        user.setFirstName(addUserRequest.getFirstName());
        user.setLastName(addUserRequest.getLastName());

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setAccountVerifyTokenExpirationTime(LocalDateTime.now().plusHours(48));

        userRepository.save(user);
        logger.info("User saved to database with email: {}", user.getEmail());

        String verificationLink = "http://localhost:3000/verify/" + verificationToken;

        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "Please Verify Your Account",
                    "Thank you again for registering. Please verify your account using the following link: " + verificationLink
            );
            logger.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send verification email to: {}", user.getEmail(), e);
        }

        return user;
    }

    @Override
    public String loginUser(String email, String password) {
        logger.info("Entering Login:");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if the user account is verified
        if (!user.getIsVerified()) {
            logger.warn("User login attempt failed: Account not verified");
            throw new ResourceNotFoundException("Your account is not verified. Please verify your email to proceed.");
        }

        // Check if the password matches
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            logger.warn("User login attempt failed: Wrong password");
            throw new ResourceNotFoundException("Wrong password");
        }

        logger.info("close to JWT");
        return jwtUtil.generateToken(email);
    }


    @Override
    public User verifyUser(String token) {
        logger.info("Verifying user with token: {}", token); // Log token

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

        logger.info("User found for verification token: {}", user.getEmail());

        if (user.getAccountVerifyTokenExpirationTime().isBefore(LocalDateTime.now())) {
            logger.error("Verification token expired for user: {}", user.getEmail());
            throw new RuntimeException("Verification token expired");
        }

        user.setIsVerified(true);
        user.setVerificationToken(null);
        user.setAccountVerifyTokenExpirationTime(null);
        logger.info("User verified successfully: {}", user.getEmail());
        return userRepository.save(user);
    }



    @Override
    public User updateUserProfile(UserDTO userDTO) {
        User user = userRepository.findByEmail(userDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setUsername(userDTO.getUsername());

        return userRepository.save(user);
    }

    @Override
    public UserDTO getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Map User entity to UserDTO
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setUsername(user.getUsername());
        userDTO.setIsVerified(user.getIsVerified());

        // Convert groups to group IDs
        userDTO.setGroupIds(
                user.getGroups().stream()
                        .map(Group::getId)
                        .collect(Collectors.toSet())
        );

        return userDTO;
    }


    @Override
    public void resetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));


        String resetToken = UUID.randomUUID().toString();
        user.setVerificationToken(resetToken);
        user.setPasswordResetTokenExpirationTime(LocalDateTime.now().plusHours(48));
        userRepository.save(user);

        emailService.sendEmail(user.getEmail(), "Reset password",
                "Click the link to reset your password: http://localhost:3000/password-reset/" + resetToken);
    }

    @Override
    public User verifyPasswordResetToken(String token) {
        logger.info("Starting verification for reset token: {}", token); // Log the token being verified

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> {
                    logger.error("Invalid verification token: {}", token); // Log if the token is invalid
                    return new ResourceNotFoundException("Invalid verification token");
                });

        // Check if the token is expired
        if (user.getPasswordResetTokenExpirationTime().isBefore(LocalDateTime.now())) {
            logger.error("Reset token expired for user: {} at {}", user.getEmail(), user.getPasswordResetTokenExpirationTime());
            throw new RuntimeException("Password Reset token expired");
        }

        logger.info("Reset token is valid for user: {}", user.getEmail()); // Log successful verification
        return user;
    }


    @Override
    public void updatePassword(String token, String newPassword) {
        // Validate the reset token
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid reset token"));



        if (user.getPasswordResetTokenExpirationTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token expired");
        }

        // Update the user's password
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        user.setVerificationToken(null);
        user.setPasswordResetTokenExpirationTime(null);
        userRepository.save(user);
    }

}

