package com.smartsplit.backend.service;

import com.smartsplit.backend.dto.UserDTO;
import com.smartsplit.backend.model.User;
import com.smartsplit.backend.request.AddUserRequest;
import org.springframework.stereotype.Service;

public interface IUserService {

    User registerUser(AddUserRequest addUserRequest);
    String loginUser(String email, String password);
    User verifyUser(String token);

    User updateUserProfile(UserDTO userDTO);

    UserDTO getUserProfile(String email);

    void resetPassword(String email);

    User verifyPasswordResetToken(String token);

    void updatePassword(String token, String newPassword);
}
