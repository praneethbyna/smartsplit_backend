package com.smartsplit.backend.controller;


import com.smartsplit.backend.dto.GroupDTO;
import com.smartsplit.backend.exception.ResourceNotFoundException;
import com.smartsplit.backend.model.Group;
import com.smartsplit.backend.response.ApiResponse;
import com.smartsplit.backend.service.IGroupService;
import com.smartsplit.backend.util.SecurityUtil;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final IGroupService groupService;

    private static final Logger logger = LoggerFactory.getLogger(GroupController.class);

    private String getLoggedInUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createGroup(@RequestParam String groupName){
        String adminEmail = getLoggedInUserEmail();

        try{
            GroupDTO groupDTO = groupService.createGroup(groupName, adminEmail);
            return ResponseEntity.ok(new ApiResponse("Group created successfully", groupDTO));
        }catch(ResourceNotFoundException e){
            throw new ResourceNotFoundException(e.getMessage());
        }

    }


    @PostMapping("/{groupId}/add-member")
    public ResponseEntity<ApiResponse> addMemberToGroup(@PathVariable Long groupId, @RequestParam String userEmail) {
        try{
            GroupDTO groupDTO = groupService.addMemberToGroup(groupId, userEmail);
            return ResponseEntity.ok(new ApiResponse("Member added to group successfully", groupDTO));
        }catch(Exception e){
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @DeleteMapping("/{groupId}/remove-member")
    public ResponseEntity<ApiResponse> removeMemberFromGroup(@PathVariable Long groupId, @RequestParam String userEmail) {
        try{
            GroupDTO groupDTO = groupService.removeMemberFromGroup(groupId, userEmail);
            return ResponseEntity.ok(new ApiResponse("Member removed successfully", groupDTO));
        }catch(ResourceNotFoundException e){
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse> getGroupDetails(@PathVariable Long groupId) {
        logger.info("Fetching details for group with ID: {}", groupId);
        try {
            GroupDTO groupDTO = groupService.getGroupDetails(groupId);
            logger.info("Successfully fetched group details: {}", groupDTO);
            return ResponseEntity.ok(new ApiResponse("Group details fetched successfully", groupDTO));
        } catch (ResourceNotFoundException e) {
            logger.error("Group not found for ID: {}", groupId, e);
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error fetching group details for ID: {}", groupId, e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }


    @GetMapping("/my-groups")
    public ResponseEntity<ApiResponse> getMyGroups() {
        String userEmail = getLoggedInUserEmail();
        try {
            List<GroupDTO> groups = groupService.getGroupsForUser(userEmail);
            return ResponseEntity.ok(new ApiResponse("User groups fetched successfully", groups));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse> deleteGroup(@PathVariable Long groupId) {
        try {
            groupService.deleteGroup(groupId);
            return ResponseEntity.ok(new ApiResponse("Group deleted successfully", null));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @PutMapping("/{groupId}/update-name")
    public ResponseEntity<ApiResponse> updateGroupName(@PathVariable Long groupId, @RequestParam String newName) {
        try {
            GroupDTO updatedGroup = groupService.updateGroupName(groupId, newName);
            return ResponseEntity.ok(new ApiResponse("Group name updated successfully", updatedGroup));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }






}


















