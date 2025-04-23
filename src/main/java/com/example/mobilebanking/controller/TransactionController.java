package com.example.mobilebanking.controller;

import com.example.mobilebanking.model.Transaction;
import com.example.mobilebanking.model.User;
import com.example.mobilebanking.dto.TransferRequest;
import com.example.mobilebanking.repository.TransactionRepository;
import com.example.mobilebanking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @GetMapping("/from/{username}")
    public ResponseEntity<List<Transaction>> getSentTransactions(@PathVariable String username) {
        List<Transaction> transactions = transactionRepository.findByFromUser(username);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/to/{username}")
    public ResponseEntity<List<Transaction>> getReceivedTransactions(@PathVariable String username) {
        List<Transaction> transactions = transactionRepository.findByToUser(username);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody TransferRequest request) {
        Optional<User> senderOpt = userRepository.findByUsername(request.getFromUser());
        Optional<User> receiverOpt = userRepository.findByUsername(request.getToUser());

        if (senderOpt.isEmpty() || receiverOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Sender or receiver not found");
        }

        User sender = senderOpt.get();
        User receiver = receiverOpt.get();
        BigDecimal amount = request.getAmount();

        if (sender.getBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body("Insufficient balance");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        Transaction transaction = new Transaction();
        transaction.setFromUser(request.getFromUser());
        transaction.setToUser(request.getToUser());
        transaction.setAmount(amount);
        transaction.setType("transfer");
        transaction.setCreatedAt(LocalDateTime.now());

        userRepository.save(sender);
        userRepository.save(receiver);
        transactionRepository.save(transaction);

        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/summary/{username}")
    public ResponseEntity<Map<String, BigDecimal>> getTransactionSummary(@PathVariable String username) {
        List<Transaction> incomeList = transactionRepository.findByToUser(username);
        List<Transaction> expenseList = transactionRepository.findByFromUser(username);

        BigDecimal income = incomeList.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenses = expenseList.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("income", income);
        result.put("expenses", expenses);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable String username) {
        List<Transaction> sent = transactionRepository.findByFromUser(username);
        List<Transaction> received = transactionRepository.findByToUser(username);

        List<Transaction> combined = new ArrayList<>();
        combined.addAll(sent);
        combined.addAll(received);

        combined.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // limit to last 5
        List<Transaction> latest5 = combined.stream().limit(5).toList();

        return ResponseEntity.ok(latest5);
    }
}