package com.example.mobilebanking.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.mobilebanking.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    List<Transaction> findByFromUser(String fromUser);

    List<Transaction> findByToUser(String toUser);

    @Query(value = "SELECT * FROM transaction WHERE from_user = :username OR to_user = :username ORDER BY created_at DESC LIMIT 5", nativeQuery = true)
    List<Transaction> findRecentTransactionsByUsername(@Param("username") String username);

}
