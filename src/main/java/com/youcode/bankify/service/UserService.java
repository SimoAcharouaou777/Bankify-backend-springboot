package com.youcode.bankify.service;

import com.youcode.bankify.dto.AccountCreationDTO;
import com.youcode.bankify.dto.TransactionResponse;
import com.youcode.bankify.dto.TransferRequest;
import com.youcode.bankify.dto.UserProfileUpdateDTO;
import com.youcode.bankify.entity.BankAccount;
import com.youcode.bankify.entity.ScheduledTransfer;
import com.youcode.bankify.entity.Transaction;
import com.youcode.bankify.entity.User;
import com.youcode.bankify.repository.jpa.AccountRepository;
import com.youcode.bankify.repository.jpa.ScheduledTransferRepository;
import com.youcode.bankify.repository.jpa.TransactionRepository;
import com.youcode.bankify.repository.jpa.UserRepository;
import com.youcode.bankify.repository.elasticsearch.TransactionSearchRepository;
import com.youcode.bankify.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ScheduledTransferRepository scheduledTransferRepository;
    private final TransactionSearchRepository transactionSearchRepository;
    private final JwtUtil jwtUtil;



    /**
     * Extracts the user ID from the Authentication object.
     */
    public Long getUserIdFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    /**
     * Retrieve bank accounts for the user.
     */
    public List<BankAccount> getBankAccounts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return accountRepository.findByUserId(userId, pageable).getContent();
    }

    /**
     * Create a new bank account for the user.
     */
    public BankAccount createBankAccount(AccountCreationDTO accountCreationDTO, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIdentityNumber() == null) {
            user.setFirstName(accountCreationDTO.getFirstName());
            user.setLastName(accountCreationDTO.getLastName());
            user.setIdentityNumber(accountCreationDTO.getIdentityNumber());
            user.setDateOfBirth(accountCreationDTO.getDateOfBirth());
            int age = calculateAge(accountCreationDTO.getDateOfBirth());
            user.setAge(age);

            if (age < 18) {
                throw new RuntimeException("You must be at least 18 years old to create an account.");
            }
            userRepository.save(user);
        }

        BankAccount account = new BankAccount();
        account.setUser(user);
        account.setStatus("ACTIVE");

        String accountNumber;
        do {
            accountNumber = generateAccountNumber();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        account.setAccountNumber(accountNumber);
        account.setBalance(BigDecimal.valueOf(100));
        return accountRepository.save(account);
    }

    /**
     * Retrieve transaction history for the user.
     */
    public List<TransactionResponse> getTransactionHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByDateDesc(userId, pageable);
        return transactions.stream().map(this::mapToTransactionResponse).collect(Collectors.toList());
    }

    /**
     * Handle fund transfers based on the transaction type.
     */
        public void handleTransfer(TransferRequest transferRequest, Authentication authentication) {
        String transactionType = transferRequest.getTransactionType().toUpperCase();
        switch (transactionType) {
            case "CLASSIC":
            case "INSTANT":
                transferFunds(transferRequest, authentication);
                break;
            case "PERMANENT":
                Long userId = getUserIdFromAuthentication(authentication);
                schedulePermanentTransfer(transferRequest, userId);
                break;
            default:
                throw new RuntimeException("Invalid transaction type");
        }
    }

    /**
     * Transfer funds between accounts.
     */
    public void transferFunds(TransferRequest transferRequest, Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long userId = user.getId();

        BigDecimal transactionFee = calculateTransactionFee(transferRequest.getTransactionType(), transferRequest.getAmount());
        BigDecimal totalDebitAmount = BigDecimal.valueOf(transferRequest.getAmount()).add(transactionFee);
        BankAccount fromAccount = getAccountWithBalanceCheck(transferRequest.getFromAccount(), totalDebitAmount, userId);
        BankAccount toAccount = accountRepository.findByAccountNumber(transferRequest.getToAccountNumber())
                .orElseThrow(() -> new RuntimeException("To account not found"));

        processTransfer(fromAccount, toAccount, BigDecimal.valueOf(transferRequest.getAmount()), transactionFee);
    }

    public void transferFundsForScheduledJob(TransferRequest transferRequest) {
        BigDecimal transactionFee = calculateTransactionFee(
                transferRequest.getTransactionType(),
                transferRequest.getAmount()
        );
        BigDecimal totalDebitAmount = BigDecimal.valueOf(transferRequest.getAmount()).add(transactionFee);

        BankAccount fromAccount = accountRepository.findById(transferRequest.getFromAccount())
                .orElseThrow(() -> new RuntimeException("From account not found"));

        if(fromAccount.getBalance().compareTo(totalDebitAmount) < 0){
            throw new RuntimeException("Insufficient funds for scheduled transfer");
        }

        BankAccount toAccount = accountRepository.findByAccountNumber(transferRequest.getToAccountNumber())
                .orElseThrow(() -> new RuntimeException("To account not found"));

        processTransfer(fromAccount, toAccount, BigDecimal.valueOf(transferRequest.getAmount()), transactionFee);
    }


    public void schedulePermanentTransfer(TransferRequest transferRequest, Long userId) {
        BankAccount fromAccount = accountRepository.findById(transferRequest.getFromAccount())
                .orElseThrow(() -> new RuntimeException("From account not found"));
        if (!fromAccount.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to transfer from this account");
        }

        BankAccount recipientAccount = accountRepository.findByAccountNumber(transferRequest.getToAccountNumber())
                .orElseThrow(() -> new RuntimeException("Recipient account not found"));

        ScheduledTransfer scheduledTransfer = new ScheduledTransfer();
        scheduledTransfer.setFromAccountId(transferRequest.getFromAccount());
        scheduledTransfer.setToAccountId(recipientAccount.getId());
        scheduledTransfer.setAmount(BigDecimal.valueOf(transferRequest.getAmount()));
        scheduledTransfer.setFrequency(transferRequest.getFrequency().toUpperCase());
        scheduledTransfer.setNextExecutionDate(OffsetDateTime.of(calculateInitialExecutionDate(transferRequest.getFrequency()), OffsetDateTime.now().getOffset()));

        scheduledTransferRepository.save(scheduledTransfer);
    }

    /**
     * Deposit money into an account.
     */
    public void depositMoney(Long userId, Long accountId, BigDecimal amount) {
        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to deposit to this account");
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        recordTransaction(account, amount, "CREDIT", "APPROVED");
    }

    /**
     * Withdraw money from an account.
     */
    public void withdrawMoney(Long userId, Long accountId, BigDecimal amount) {
        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to withdraw from this account");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds for withdrawal");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        recordTransaction(account, amount, "DEBIT", "APPROVED");
    }

    /**
     * Update user profile.
     */
    public User updateProfile(Long userId, UserProfileUpdateDTO profileUpdateDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update allowed fields
        user.setFirstName(profileUpdateDTO.getFirstName());
        user.setLastName(profileUpdateDTO.getLastName());
        user.setDateOfBirth(profileUpdateDTO.getDateOfBirth());
        user.setAge(profileUpdateDTO.getAge());
        user.setIdentityNumber(profileUpdateDTO.getIdentityNumber());

        return userRepository.save(user);
    }

    /**
     * Calculate age based on date of birth.
     */
    private int calculateAge(LocalDate dateOfBirth) {
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }

    /**
     * Generate a unique account number.
     */
    private String generateAccountNumber() {
        return RandomStringUtils.randomNumeric(12);
    }

    /**
     * Calculate transaction fee based on type.
     */
    private BigDecimal calculateTransactionFee(String transactionType, double amount) {
        BigDecimal transferAmount = BigDecimal.valueOf(amount);
        switch (transactionType.toUpperCase()) {
            case "CLASSIC":
                return transferAmount.multiply(BigDecimal.valueOf(0.01));
            case "INSTANT":
                return transferAmount.multiply(BigDecimal.valueOf(0.02));
            default:
                throw new RuntimeException("Invalid transaction type for fee calculation");
        }
    }

    /**
     * Retrieve account and check for sufficient balance and ownership.
     */
    public BankAccount getAccountWithBalanceCheck(Long accountId, BigDecimal amountToCheck, Long userId) {
        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to transfer from this account");
        }
        if (account.getBalance().compareTo(amountToCheck) < 0) {
            throw new RuntimeException("Insufficient funds, including transaction fee");
        }
        return account;
    }

    /**
     * Process the fund transfer between accounts.
     */
    private void processTransfer(BankAccount fromAccount, BankAccount toAccount, BigDecimal transferAmount, BigDecimal transactionFee) {
        fromAccount.setBalance(fromAccount.getBalance().subtract(transferAmount.add(transactionFee)));
        toAccount.setBalance(toAccount.getBalance().add(transferAmount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        String status = transferAmount.compareTo(BigDecimal.valueOf(5000)) > 0 ? "PENDING" : "APPROVED";

        recordTransaction(fromAccount, transferAmount, "DEBIT", status);
        recordTransaction(toAccount, transferAmount, "CREDIT", status);
    }

    /**
     * Record a transaction in the database and Elasticsearch.
     */
    private void recordTransaction(BankAccount account, BigDecimal amount, String type, String status) {
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDate(OffsetDateTime.now());
        transaction.setBankAccount(account);
        transaction.setUser(account.getUser());
        transaction.setStatus(status);
        transactionRepository.save(transaction);
        transactionSearchRepository.save(transaction);
    }

    /**
     * Calculate the initial execution date for scheduled transfers.
     */
    private LocalDateTime calculateInitialExecutionDate(String frequency) {
        LocalDateTime currentDate = LocalDateTime.now();
        switch (frequency.toUpperCase()) {
            case "DAILY":
                return currentDate.plusDays(1);
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

    /**
     * Map Transaction entity to TransactionResponse DTO.
     */
    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        TransactionResponse dto = new TransactionResponse();
        dto.setAmount(transaction.getAmount());
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        dto.setDate(transaction.getDate().format(formatter));
        dto.setType(transaction.getType());
        dto.setStatus(transaction.getStatus());
        dto.setOtherPartyUsername(
                transaction.getType().equalsIgnoreCase("DEBIT") ?
                        transaction.getBankAccount().getUser().getUsername() :
                        transaction.getUser().getUsername()
        );
        return dto;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void verifyAccountOwnerShip(Long accountId , Long userId) {
        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if(!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to the account");
        }
    }

    public Map<String, Object> getDashboardSummary(Long userId) {
        BigDecimal totalBalance = accountRepository.findByUserId(userId).stream()
                .map(BankAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeAccounts = accountRepository.findByUserId(userId).stream()
                .filter(account -> "ACTIVE".equalsIgnoreCase(account.getStatus()))
                .count();

        long pendingTransactions = transactionRepository.findByUserId(userId).stream()
                .filter(transaction -> "PENDING".equalsIgnoreCase(transaction.getStatus()))
                .count();

        List<Transaction> recentTransactions = transactionRepository.findByUserIdOrderByDateDesc(userId, PageRequest.of(0,4));

        Map<String, Object> dashboardSummary = new HashMap<>();
        dashboardSummary.put("totalBalance", totalBalance);
        dashboardSummary.put("activeAccounts", activeAccounts);
        dashboardSummary.put("pendingTransactions", pendingTransactions);
        dashboardSummary.put("recentTransactions", recentTransactions);

        return dashboardSummary;
    }
}
