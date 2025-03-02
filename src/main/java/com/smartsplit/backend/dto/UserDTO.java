package com.smartsplit.backend.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String username;
    private Boolean isVerified;
    private Set<Long> groupIds;
    private Double amountOwed;// Represent groups as a list of their IDs
}
