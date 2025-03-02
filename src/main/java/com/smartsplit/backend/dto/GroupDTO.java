package com.smartsplit.backend.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class GroupDTO {
    private Long id;
    private String name;
    private Long adminId; // Represent admin as their ID
    private Double totalOwed;
    private List<UserDTO> members;// Dynamically calculated total owed by the user
}
