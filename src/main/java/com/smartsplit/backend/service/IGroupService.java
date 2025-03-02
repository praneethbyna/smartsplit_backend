package com.smartsplit.backend.service;

import com.smartsplit.backend.dto.GroupDTO;

import java.util.List;


public interface IGroupService {
    GroupDTO createGroup(String groupName, String adminEmail);
    GroupDTO addMemberToGroup(Long groupId, String userEmail);
    GroupDTO removeMemberFromGroup(Long groupId, String userEmail);

    GroupDTO getGroupDetails(Long groupId);

    List<GroupDTO> getGroupsForUser(String userEmail);

    void deleteGroup(Long groupId);

    GroupDTO updateGroupName(Long groupId, String newName);
}
