package com.smartsplit.backend.request;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateExpenseRequest {
    private String description;
    private Double amount;
    private Long paidById;
    private Map<Long, Double> splits; // {userId: splitAmount}

}
