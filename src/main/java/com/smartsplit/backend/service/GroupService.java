package com.smartsplit.backend.service;

import com.smartsplit.backend.dto.GroupDTO;
import com.smartsplit.backend.dto.UserDTO;
import com.smartsplit.backend.exception.ResourceNotFoundException;
import com.smartsplit.backend.model.Expense;
import com.smartsplit.backend.model.Group;
import com.smartsplit.backend.model.User;
import com.smartsplit.backend.repository.ExpenseRepository;
import com.smartsplit.backend.repository.GroupRepository;
import com.smartsplit.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GroupService implements IGroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final ExpenseRepository expenseRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository, ExpenseRepository expenseRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.modelMapper = new ModelMapper();

    }


    @Override
    public GroupDTO createGroup(String groupName, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        Group group = new Group();
        group.setName(groupName);
        group.setAdmin(admin);
        group.getMembers().add(admin); // Add admin as a member too
        Group savedGroup = groupRepository.save(group);

        return convertToGroupDTO(savedGroup);
    }

    @Override
    @Transactional
    public GroupDTO addMemberToGroup(Long groupId, String userEmail) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (group.getMembers().contains(user)) {
            throw new IllegalStateException("User is already a member of the group");
        }

        group.addMember(user);
        Group updatedGroup = groupRepository.save(group);

        return convertToGroupDTO(updatedGroup);
    }


    @Override
    public GroupDTO removeMemberFromGroup(Long groupId, String userEmail) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        group.getMembers().remove(user);
        Group updatedGroup = groupRepository.save(group);
        return convertToGroupDTO(updatedGroup);
    }

    @Override
    public GroupDTO getGroupDetails(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        GroupDTO groupDTO = convertToGroupDTO(group);

        // Include detailed member information
        groupDTO.setMembers(group.getMembers().stream()
                .map(member -> {
                    UserDTO memberDTO = new UserDTO();
                    memberDTO.setId(member.getId());
                    memberDTO.setFirstName(member.getFirstName());
                    memberDTO.setLastName(member.getLastName());
                    memberDTO.setEmail(member.getEmail());
                    // Calculate amount owed if necessary
                    memberDTO.setAmountOwed(calculateAmountOwedForMember(member, group));
                    return memberDTO;
                })
                .collect(Collectors.toList()));

        return groupDTO;
    }

    private Double calculateAmountOwedForMember(User member, Group group) {
        // Fetch all expenses for the group
        List<Expense> groupExpenses = expenseRepository.findByGroup(group);

        double totalOwedByMember = 0.0;
        double totalPaidByMember = 0.0;

        for (Expense expense : groupExpenses) {
            // Check if the member is part of the splits for the expense
            if (expense.getSplits().containsKey(member)) {
                // Add the amount owed by the member for this expense
                totalOwedByMember += expense.getSplits().get(member);
            }

            // Check if the member is the payer of the expense
            if (expense.getPaidBy().equals(member)) {
                // Add the total amount paid by the member for this expense
                totalPaidByMember += expense.getAmount();
            }
        }

        // The net amount owed by the member
        return totalOwedByMember - totalPaidByMember;
    }




    @Override
    public List<GroupDTO> getGroupsForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getGroups().stream()
                .map(group -> {
                    GroupDTO dto = convertToGroupDTO(group);
                    dto.setTotalOwed(calculateTotalOwed(user, group)); // Add total owed
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private Double calculateTotalOwed(User user, Group group) {
        // Mock logic for calculating the total owed
        // Replace with real calculations based on your application's rules
        List<Expense> expenses = group.getExpenses(); // Ensure Group has a method to fetch expenses
        double totalOwed = 0.0;

        for (Expense expense : expenses) {
            Map<User, Double> splits = expense.getSplits();

            // Check if the user is part of the splits for the expense
            if (splits.containsKey(user)) {
                double userShare = splits.get(user); // User's share in the expense
                double paidAmount = expense.getPaidBy().equals(user) ? expense.getAmount() : 0.0; // Amount paid by the user
                totalOwed += userShare - paidAmount; // Accumulate the difference
            }
        }
        return totalOwed;
    }

    @Override
    public void deleteGroup(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        groupRepository.delete(group);
    }

    @Override
    public GroupDTO updateGroupName(Long groupId, String newName) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        group.setName(newName);
        Group updatedGroup = groupRepository.save(group);
        return convertToGroupDTO(updatedGroup);
    }



    private GroupDTO convertToGroupDTO(Group group) {
        GroupDTO groupDTO = modelMapper.map(group, GroupDTO.class);

        // Custom mapping for adminId and memberIds
        groupDTO.setAdminId(group.getAdmin() != null ? group.getAdmin().getId() : null);
        groupDTO.setMembers(group.getMembers().stream()
                .map(member -> {
                    UserDTO memberDTO = modelMapper.map(member, UserDTO.class);
                    memberDTO.setAmountOwed(calculateAmountOwedForMember(member, group)); // Calculate amount owed
                    return memberDTO;
                })
                .collect(Collectors.toList()));

        return groupDTO;
    }

}
