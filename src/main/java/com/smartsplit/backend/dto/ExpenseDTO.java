package com.smartsplit.backend.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExpenseDTO {
    private Long id;
    private String description;
    private Double amount;
    private Long groupId;
    private Long paidById;
    private Map<Long, Double> splits; // Key: User ID, Value: Split Amount
    private List<Long> selectedMemberIds; // Optional: IDs of selected members for the expense split
    private String paidByName; // Added fields
    private Map<String, Double> splitsBreakdown;
}
