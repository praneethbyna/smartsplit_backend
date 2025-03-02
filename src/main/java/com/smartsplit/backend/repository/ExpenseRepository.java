package com.smartsplit.backend.repository;

import com.smartsplit.backend.model.Expense;
import com.smartsplit.backend.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroup(Group group);

    @Query("SELECT e FROM Expense e " +
            "LEFT JOIN FETCH e.splits " +
            "WHERE e.group.id = :groupId")
    List<Expense> findByGroupWithSplits(@Param("groupId") Long groupId);


}
