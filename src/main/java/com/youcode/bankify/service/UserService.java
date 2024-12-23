package com.youcode.bankify.service;


import com.youcode.bankify.dto.AccountCreationDTO;
import com.youcode.bankify.dto.TransactionResponse;
import com.youcode.bankify.dto.TransferRequest;
import com.youcode.bankify.entity.BankAccount;
import com.youcode.bankify.entity.ScheduledTransfer;
import com.youcode.bankify.entity.Transaction;
import com.youcode.bankify.entity.User;
import com.youcode.bankify.repository.jpa.AccountRepository;
import com.youcode.bankify.repository.jpa.ScheduledTransferRepository;
import com.youcode.bankify.repository.jpa.TransactionRepository;
import com.youcode.bankify.repository.jpa.UserRepository;
import com.youcode.bankify.repository.elasticsearch.TransactionSearchRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    UserRepository userRepository;
    @Autowired
    private ScheduledTransferRepository scheduledTransferRepository;
    @Autowired
    private TransactionSearchRepository transactionSearchRepository;

    public List<BankAccount> getBankAccounts(Long userId, int page , int size){
        Pageable pageable = PageRequest.of(page,size);
        return accountRepository.findByUserId(userId, pageable).getContent();
    }

    public BankAccount createBankAccount(AccountCreationDTO accountCreationDTO, Long userId){
        User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"));
        if(user.getIdentityNumber() == null){
            user.setFirstName(accountCreationDTO.getFirstName());
            user.setLastName(accountCreationDTO.getLastName());
            user.setIdentityNumber(accountCreationDTO.getIdentityNumber());
            user.setDateOfBirth(accountCreationDTO.getDateOfBirth());
            int age = LocalDate.now().getYear() - accountCreationDTO.getDateOfBirth().getYear();
            user.setAge(age);

            if(age < 18){
                throw new RuntimeException("you must be at least 18 years old to be able to create you own account");
            }
            userRepository.save(user);
        }

        BankAccount account = new BankAccount();
        account.setUser(user);
        account.setStatus("ACTIVE");

        String accountNumber;
        do{
            accountNumber = generatedAccountNumber();
        }while(accountRepository.existsByAccountNumber(accountNumber));

        account.setAccountNumber(accountNumber);
        account.setBalance(BigDecimal.valueOf(100));
        BankAccount savedAccount = accountRepository.save(account);


        return savedAccount;
    }

    public List<TransactionResponse> getTransactionHistory(Long userId, int page , int size){
        Pageable pageable  = PageRequest.of(page, size);
        List<Transaction> transactions = transactionRepository.findByUserId(userId,pageable);
        List<TransactionResponse> transactionResponses = transactions.stream().map(transaction -> {
            TransactionResponse dto = new TransactionResponse();
            dto.setAmount(transaction.getAmount());
            dto.setDate(transaction.getDate());

            if(transaction.getBankAccount().getUser().getId().equals(userId)){
                dto.setType("SENT");
                dto.setOtherPartyUsername(transaction.getUser().getUsername());
            }else{
                dto.setType("RECEIVED");
                dto.setOtherPartyUsername(transaction.getBankAccount().getUser().getUsername());
            }
            return dto;
        }).collect(Collectors.toList());
        return transactionResponses;
    }

    public void transferFunds(TransferRequest transferRequest) {
        BigDecimal transactionFee = calculateTransactionFee(transferRequest);

        BigDecimal totalDebitAmount = BigDecimal.valueOf(transferRequest.getAmount()).add(transactionFee);
        BankAccount fromAccount = getAccountWithBalanceCheck(transferRequest.getFromAccount(), totalDebitAmount);
        BankAccount toAccount = accountRepository.findById(transferRequest.getToAccount())
                .orElseThrow(() -> new RuntimeException("To account not found"));

        processTransfer(fromAccount, toAccount, BigDecimal.valueOf(transferRequest.getAmount()), transactionFee);
    }



    public void schedulePermanentTransfer(TransferRequest transferRequest) {
        ScheduledTransfer scheduledTransfer = new ScheduledTransfer();
        scheduledTransfer.setFromAccountId(transferRequest.getFromAccount());
        scheduledTransfer.setToAccountId(transferRequest.getToAccount());
        scheduledTransfer.setAmount(BigDecimal.valueOf(transferRequest.getAmount()));
        scheduledTransfer.setFrequency(transferRequest.getFrequency().toUpperCase());
        LocalDateTime nextExecutionDate = calculateInitialExecutionDate(transferRequest.getFrequency());
        scheduledTransfer.setNextExecutionDate(nextExecutionDate);

        scheduledTransferRepository.save(scheduledTransfer);
    }

    private void processTransfer(BankAccount fromAccount, BankAccount toAccount, BigDecimal transferAmount, BigDecimal transactionFee) {
        fromAccount.setBalance(fromAccount.getBalance().subtract(transferAmount.add(transactionFee)));
        toAccount.setBalance(toAccount.getBalance().add(transferAmount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        String status = transferAmount.compareTo(BigDecimal.valueOf(5000)) > 0 ? "PENDING" : "APPROVED";

        recordTransaction(fromAccount, transferAmount, "DEBIT", status);
        recordTransaction(toAccount, transferAmount, "CREDIT", status);
    }

    private void recordTransaction(BankAccount account, BigDecimal amount, String type, String status) {
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDate(LocalDateTime.now());
        transaction.setBankAccount(account);
        transaction.setUser(account.getUser());
        transaction.setStatus(status);
        transactionRepository.save(transaction);
        transactionSearchRepository.save(transaction);
    }


    private BigDecimal calculateTransactionFee(TransferRequest transferRequest) {
        BigDecimal transferAmount = BigDecimal.valueOf(transferRequest.getAmount());
        switch (transferRequest.getTransactionType().toUpperCase()) {
            case "CLASSIC":
                return transferAmount.multiply(BigDecimal.valueOf(0.01));
            case "INSTANT":
                return transferAmount.multiply(BigDecimal.valueOf(0.02));
            case "PERMANENT":
                schedulePermanentTransfer(transferRequest);
                return BigDecimal.ZERO; // No immediate fee as it's a scheduled transfer
            default:
                throw new RuntimeException("Invalid transaction type");
        }
    }

    private BankAccount getAccountWithBalanceCheck(Long accountId, BigDecimal amountToCheck) {
        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (account.getBalance().compareTo(amountToCheck) < 0) {
            throw new RuntimeException("Insufficient funds, including transaction fee");
        }
        return account;
    }

    private LocalDateTime calculateInitialExecutionDate(String frequency) {
        LocalDateTime currentDate = LocalDateTime.now();
        switch (frequency.toUpperCase()) {
            case "WEEKLY":
                return currentDate.plusWeeks(1);
            case "MONTHLY":
                return currentDate.plusMonths(1);
            case "YEARLY":
                return currentDate.plusYears(1);
            default:
                throw new IllegalArgumentException("Invalid frequency type: " + frequency);
        }
    }


    public User updateProfile(User user){
        return userRepository.save(user);
    }

    private String generatedAccountNumber(){
        return RandomStringUtils.randomNumeric(12);
    }

    public void depositMoney(Long userId , Long accountId , BigDecimal amount){
        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if(!account.getUser().getId().equals(userId)){
            throw new RuntimeException("You are not authorized to deposit to this account");
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }

    public void withdrawMoney(Long userId , Long accountId , BigDecimal amount){
        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if(!account.getUser().getId().equals(userId)){
            throw  new RuntimeException("You are not  authorized to withdraw from this account");
        }

        if(account.getBalance().compareTo(amount) < 0){
            throw new RuntimeException("Insufficient fund for withdrawal");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
    }

//    public List<Transaction> searchTransactions(BigDecimal amount, String type, String status, LocalDateTime startDate, LocalDateTime endDate) {
//        return transactionSearchRepository.searchByCriteria(amount, type, status, startDate, endDate);
//    }





}
