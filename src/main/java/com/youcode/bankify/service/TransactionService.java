package com.youcode.bankify.service;

import com.youcode.bankify.dto.TransactionResponse;
import com.youcode.bankify.entity.Transaction;
import com.youcode.bankify.repository.elasticsearch.TransactionSearchRepository;
import com.youcode.bankify.repository.jpa.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionSearchRepository transactionSearchRepository;
    private final TransactionRepository transactionRepository;


    public List<Transaction> searchTransactions(BigDecimal amount, String type, String status, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionSearchRepository.searchByCriteria(amount, type, status, startDate, endDate);
    }

    public List<TransactionResponse> getTransactionsByAccountId(Long accountId , int page , int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Transaction> transactions = transactionRepository.findByBankAccountId(accountId, pageable);
        return transactions.stream().map(this::mapToTransactionResponse).collect(Collectors.toList());
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        TransactionResponse dto = new TransactionResponse();
        dto.setAmount(transaction.getAmount());
        dto.setDate(transaction.getDate().toString());
        dto.setType(transaction.getType());
        dto.setStatus(transaction.getStatus());
        dto.setOtherPartyUsername(
                transaction.getType().equalsIgnoreCase("DEBIT") ?
                        transaction.getBankAccount().getUser().getUsername() :
                        transaction.getUser().getUsername()
        );
        return dto;
    }
}
