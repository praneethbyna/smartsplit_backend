package com.smartsplit.backend.controller;


import com.smartsplit.backend.dto.ExpenseDTO;
import com.smartsplit.backend.exception.ResourceNotFoundException;
import com.smartsplit.backend.model.Expense;
import com.smartsplit.backend.request.UpdateExpenseRequest;
import com.smartsplit.backend.response.ApiResponse;
import com.smartsplit.backend.service.ExpenseService;
import com.smartsplit.backend.service.IExpenseService;
import lombok.RequiredArgsConstructor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/expenses/")
public class ExpenseController {

    private final IExpenseService expenseService;
    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);

    @PostMapping("/{groupId}")
    public ResponseEntity<ApiResponse> createExpense(
            @PathVariable Long groupId,
            @RequestBody ExpenseDTO expenseDTO) {
        try {
            logger.info("Expense API called 111");
            ExpenseDTO createdExpense = expenseService.createExpense(groupId, expenseDTO);
            logger.info("Expense API called");
            return ResponseEntity.ok(new ApiResponse("Expense created successfully", createdExpense));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse> getGroupExpenses(@PathVariable Long groupId) {
        try {
            List<ExpenseDTO> expensesDTO = expenseService.getExpensesForGroup(groupId);
            return ResponseEntity.ok(new ApiResponse("Expenses fetched successfully", expensesDTO));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @PutMapping("/{groupId}/{expenseId}")
    public ResponseEntity<?> updateExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId,
            @RequestBody UpdateExpenseRequest updateExpenseRequest
    ) {
        logger.info("Update request payload: {}", updateExpenseRequest);
        try {
            // Validate splits map keys
            for (Map.Entry<Long, Double> entry : updateExpenseRequest.getSplits().entrySet()) {
                if (entry.getKey() == null || entry.getKey() <= 0) {
                    throw new IllegalArgumentException("Invalid user ID in splits map");
                }
            }

            Expense updatedExpense = expenseService.updateExpense(groupId, expenseId, updateExpenseRequest);
            return ResponseEntity.ok(new ApiResponse("Expense updated successfully", updatedExpense));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
        }
    }


    @DeleteMapping("/{groupId}/{expenseId}")
    public ResponseEntity<?> deleteExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId
    ) {
        try {
            expenseService.deleteExpense(groupId, expenseId);
            return ResponseEntity.ok(new ApiResponse("Expense deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse( e.getMessage(), null));
        }
    }






}
