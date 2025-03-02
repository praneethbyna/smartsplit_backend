package com.smartsplit.backend.service;

import com.smartsplit.backend.dto.ExpenseDTO;
import com.smartsplit.backend.model.Expense;
import com.smartsplit.backend.request.UpdateExpenseRequest;

import java.util.List;

public interface IExpenseService {
    ExpenseDTO createExpense(Long groupId, ExpenseDTO expense);

    List<ExpenseDTO> getExpensesForGroup(Long groupId);

    Expense updateExpense(Long groupId, Long expenseId, UpdateExpenseRequest updateExpenseRequest);

    void deleteExpense(Long groupId, Long expenseId);
}
