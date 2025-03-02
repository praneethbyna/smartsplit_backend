package com.smartsplit.backend.service;

import com.smartsplit.backend.dto.ExpenseDTO;
import com.smartsplit.backend.exception.ResourceNotFoundException;
import com.smartsplit.backend.model.Expense;
import com.smartsplit.backend.model.Group;
import com.smartsplit.backend.model.User;
import com.smartsplit.backend.repository.ExpenseRepository;
import com.smartsplit.backend.repository.GroupRepository;
import com.smartsplit.backend.repository.UserRepository;
import com.smartsplit.backend.request.UpdateExpenseRequest;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.hibernate.Hibernate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExpenseService implements IExpenseService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final ModelMapper modelMapper;

    public ExpenseService(GroupRepository groupRepository, UserRepository userRepository, ExpenseRepository expenseRepository, ModelMapper modelMapper) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.modelMapper = modelMapper;
    }


    @Override
    @Transactional
    public ExpenseDTO createExpense(Long groupId, ExpenseDTO expenseDTO) {
        try {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

            User paidBy = userRepository.findById(expenseDTO.getPaidById())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            Expense expense = new Expense();
            expense.setDescription(expenseDTO.getDescription());
            expense.setAmount(expenseDTO.getAmount());
            expense.setGroup(group);
            expense.setPaidBy(paidBy);

            // Handle splits
            if (expenseDTO.getSelectedMemberIds() == null || expenseDTO.getSelectedMemberIds().isEmpty()) {
                // Equal split among all members
                int membersCount = group.getMembers().size();
                double splitAmount = expense.getAmount() / membersCount;

                group.getMembers().forEach(member -> expense.getSplits().put(member, splitAmount));
            } else {
                // Split among selected members only
                List<User> selectedMembers = userRepository.findAllById(expenseDTO.getSelectedMemberIds());
                if (selectedMembers.isEmpty()) {
                    throw new IllegalArgumentException("No valid members selected for the split");
                }

                double splitAmount = expense.getAmount() / selectedMembers.size();
                selectedMembers.forEach(member -> expense.getSplits().put(member, splitAmount));

                // Ensure other members are not included in the split
                group.getMembers().forEach(member -> {
                    if (!selectedMembers.contains(member)) {
                        expense.getSplits().put(member, 0.0);
                    }
                });
            }

            Expense savedExpense = expenseRepository.save(expense);

            // Initialize splits
            Hibernate.initialize(savedExpense.getSplits());

            return convertToExpenseDTO(savedExpense);
        } catch (Exception e) {
            // Log detailed error for debugging
            throw new RuntimeException("Error creating expense: " + e.getMessage(), e);
        }
    }




    @Override
    public List<ExpenseDTO> getExpensesForGroup(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        List<Expense> expenses = expenseRepository.findByGroupWithSplits(groupId);
        return expenses.stream()
                .map(this::convertToExpenseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Expense updateExpense(Long groupId, Long expenseId, UpdateExpenseRequest request) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (!expense.getGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("Expense does not belong to the specified group");
        }

        // Fetch Users based on IDs in the splits map
        Map<User, Double> userSplits = request.getSplits().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> userRepository.findById(entry.getKey())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + entry.getKey())),
                        Map.Entry::getValue
                ));

        // Update fields
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setPaidBy(userRepository.findById(request.getPaidById())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getPaidById())));
        expense.setSplits(userSplits);

        return expenseRepository.save(expense);
    }


    @Override
    public void deleteExpense(Long groupId, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (!expense.getGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("Expense does not belong to the specified group");
        }

        expenseRepository.delete(expense);
    }


    private ExpenseDTO convertToExpenseDTO(Expense expense) {
        Hibernate.initialize(expense.getSplits());

        ExpenseDTO expenseDTO = new ExpenseDTO();
        expenseDTO.setId(expense.getId());
        expenseDTO.setDescription(expense.getDescription());
        expenseDTO.setAmount(expense.getAmount());
        expenseDTO.setGroupId(expense.getGroup().getId());
        expenseDTO.setPaidById(expense.getPaidBy().getId());
        expenseDTO.setPaidByName(expense.getPaidBy().getFirstName() + " " + expense.getPaidBy().getLastName()); // Add payer's name

        // Map splits with user details
        Map<String, Double> splitsBreakdown = expense.getSplits().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getFirstName() + " " + entry.getKey().getLastName(),
                        Map.Entry::getValue
                ));
        expenseDTO.setSplitsBreakdown(splitsBreakdown);

        return expenseDTO;
    }

}
