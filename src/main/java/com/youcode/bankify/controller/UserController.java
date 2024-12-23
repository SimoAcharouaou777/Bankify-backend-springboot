package com.youcode.bankify.controller;

import com.youcode.bankify.dto.*;
import com.youcode.bankify.entity.*;
import com.youcode.bankify.service.InvoiceService;
import com.youcode.bankify.service.LoanService;
import com.youcode.bankify.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final InvoiceService invoiceService;
    private final LoanService loanService;

    @Autowired
    public UserController(UserService userService, InvoiceService invoiceService, LoanService loanService) {
        this.userService = userService;
        this.invoiceService = invoiceService;
        this.loanService = loanService;
    }

    /**
     * Retrieve bank accounts for the authenticated user.
     */
    @GetMapping("/accounts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BankAccount>> getBankAccounts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = userService.getUserIdFromAuthentication(authentication);

        if (userId == null) {
            return ResponseEntity.status(401).body(null);
        }

        List<BankAccount> accounts = userService.getBankAccounts(userId, page, size);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Create a new bank account for the authenticated user.
     */
    @PostMapping("/accounts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createBankAccount(
            @RequestBody AccountCreationDTO accountCreationDTO,
            Authentication authentication) {
        Long userId = userService.getUserIdFromAuthentication(authentication);

        try {
            BankAccount createdAccount = userService.createBankAccount(accountCreationDTO, userId);
            return ResponseEntity.ok(createdAccount);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            error.setTimestamp(java.time.LocalDateTime.now());
            error.setStatus(400);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Retrieve transaction history for the authenticated user.
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = userService.getUserIdFromAuthentication(authentication);
        List<TransactionResponse> transactions = userService.getTransactionHistory(userId, page, size);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Transfer funds between accounts.
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> transferFunds(
            @RequestBody TransferRequest transferRequest,
            Authentication authentication) {
        Long userId = userService.getUserIdFromAuthentication(authentication);

        try {
            // Ensure the 'fromAccount' belongs to the authenticated user
            BankAccount fromAccount = userService.getAccountWithBalanceCheck(
                    transferRequest.getFromAccount(),
                    java.math.BigDecimal.valueOf(transferRequest.getAmount()) /* Adjust as needed */,
                    userId
            );

            if ("PERMANENT".equalsIgnoreCase(transferRequest.getTransactionType())) {
                if (transferRequest.getFrequency() == null || transferRequest.getFrequency().isEmpty()) {
                    ErrorResponse error = new ErrorResponse();
                    error.setMessage("Frequency is required for permanent transfers");
                    error.setTimestamp(java.time.LocalDateTime.now());
                    error.setStatus(400);
                    return ResponseEntity.badRequest().body(error);
                }
                userService.schedulePermanentTransfer(transferRequest, userId);
                return ResponseEntity.ok("Permanent transfer scheduled successfully");
            } else {
                userService.transferFunds(transferRequest, userId);
                return ResponseEntity.ok("Transfer successful");
            }
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            error.setTimestamp(java.time.LocalDateTime.now());
            error.setStatus(400);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update user profile.
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateProfile(
            @RequestBody UserProfileUpdateDTO userProfileUpdateDTO,
            Authentication authentication) {
        Long userId = userService.getUserIdFromAuthentication(authentication);

        try {
            User updatedUser = userService.updateProfile(userId, userProfileUpdateDTO);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            error.setTimestamp(java.time.LocalDateTime.now());
            error.setStatus(400);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Deposit money into an account.
     */
    @PostMapping("/deposit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> depositMoney(
            @RequestBody TransactionRequest transactionRequest,
            Authentication authentication) {
        Long userId = userService.getUserIdFromAuthentication(authentication);

        try {
            userService.depositMoney(userId, transactionRequest.getAccountId(), transactionRequest.getAmount());
            return ResponseEntity.ok("Money deposited successfully");
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            error.setTimestamp(java.time.LocalDateTime.now());
            error.setStatus(400);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Withdraw money from an account.
     */
    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> withdrawMoney(
            @RequestBody TransactionRequest transactionRequest,
            Authentication authentication) {
        Long userId = userService.getUserIdFromAuthentication(authentication);

        try {
            userService.withdrawMoney(userId, transactionRequest.getAccountId(), transactionRequest.getAmount());
            return ResponseEntity.ok("Money withdrawn successfully");
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            error.setTimestamp(java.time.LocalDateTime.now());
            error.setStatus(400);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Create a new invoice for the authenticated user.
     */
    @PostMapping("/invoices")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createInvoice(
            @RequestBody InvoiceRequestDTO invoiceRequest,
            Authentication authentication) {
        Long userId = userService.getUserIdFromAuthentication(authentication);

        try {
            Invoice invoice = invoiceService.createInvoice(invoiceRequest, userId);
            return ResponseEntity.ok(invoice);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            error.setTimestamp(java.time.LocalDateTime.now());
            error.setStatus(400);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Retrieve invoices for the authenticated user.
     */
    @GetMapping("/invoices")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getInvoices(Authentication authentication) {
        Long userId = userService.getUserIdFromAuthentication(authentication);

        try {
            List<InvoiceResponseDTO> invoices = invoiceService.getInvoices(userId);
            return ResponseEntity.ok(invoices);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            error.setTimestamp(java.time.LocalDateTime.now());
            error.setStatus(400);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update the status of an invoice.
     * Restricted to ADMIN and EMPLOYEE roles.
     */
    @PutMapping("/invoices/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<?> updateInvoiceStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            Invoice invoice = invoiceService.updateInvoiceStatus(id, status);
            return ResponseEntity.ok(invoice);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            error.setTimestamp(java.time.LocalDateTime.now());
            error.setStatus(400);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Apply for a loan.
     */
    @PostMapping("/loans")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> applyForLoan(
            @RequestBody LoanRequestDTO loanRequest,
            Authentication authentication) {
        Long userId = userService.getUserIdFromAuthentication(authentication);

        try {
            Loan loan = loanService.applyForLoan(loanRequest, userId);
            return ResponseEntity.ok(loan);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            error.setTimestamp(java.time.LocalDateTime.now());
            error.setStatus(400);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Retrieve loans for the authenticated user.
     */
    @GetMapping("/loans")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getLoans(Authentication authentication) {
        Long userId = userService.getUserIdFromAuthentication(authentication);

        try {
            List<LoanResponseDTO> loans = loanService.getLoans(userId);
            return ResponseEntity.ok(loans);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            error.setTimestamp(java.time.LocalDateTime.now());
            error.setStatus(400);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Approve or reject a loan.
     * Restricted to ADMIN and EMPLOYEE roles.
     */
    @PutMapping("/loans/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<?> approveOrRejectLoan(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            Loan loan = loanService.approveOrRejectLoan(id, status);
            return ResponseEntity.ok(loan);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            error.setTimestamp(java.time.LocalDateTime.now());
            error.setStatus(400);
            return ResponseEntity.badRequest().body(error);
        }
    }
}
