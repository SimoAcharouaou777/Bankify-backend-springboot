package com.youcode.bankify.controller;


import com.youcode.bankify.dto.ErrorResponse;
import com.youcode.bankify.dto.RecentApprovalDTO;
import com.youcode.bankify.entity.BankAccount;
import com.youcode.bankify.entity.Invoice;
import com.youcode.bankify.entity.Loan;
import com.youcode.bankify.entity.Transaction;
import com.youcode.bankify.service.EmployeeService;
import com.youcode.bankify.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/employee")
public class EmployeeController {


    private final EmployeeService employeeService;
    private final UserService userService;



    @GetMapping("/accounts")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<BankAccount>> viewCustomerAccount(Authentication authentication){
        userService.getUserIdFromAuthentication(authentication);
        List<BankAccount> accounts = employeeService.getCustomerAccounts();
        return ResponseEntity.ok(accounts);

    }

    @PostMapping("/transactions/{transactionId}/approve")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> approveTransaction(@PathVariable Long transactionId, Authentication authentication){
        userService.getUserIdFromAuthentication(authentication);
        try{
            employeeService.approveTransaction(transactionId);
            return ResponseEntity.ok(Map.of("message", "Transaction approved successfully"));
        }catch (RuntimeException e){
            return handleError(e);
        }
    }

    @PostMapping("/transactions/{transactionId}/reject")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> rejectTransaction(@PathVariable Long transactionId, Authentication authentication){
        userService.getUserIdFromAuthentication(authentication);
        try {
            employeeService.rejectTransaction(transactionId);
            return ResponseEntity.ok(Map.of("message", "Transaction rejected successfully"));
        }catch (RuntimeException e){
            return handleError(e);
        }
    }

    @PostMapping("/loans/{loanId}/approve")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> approveLoan(@PathVariable Long loanId , Authentication authentication){
        userService.getUserIdFromAuthentication(authentication);
        try {
            employeeService.approveLoan(loanId);
            return ResponseEntity.ok(Map.of("message", "Loan approved successfully"));
        }catch (RuntimeException e){
            return handleError(e);
        }
    }

    @PostMapping("/loans/{loanId}/reject")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> rejectLoan(@PathVariable Long loanId ,  Authentication authentication){
        userService.getUserIdFromAuthentication(authentication);
        try {
            employeeService.rejectLoan(loanId);
            return ResponseEntity.ok(Map.of("message", "Loan rejected successfully"));
        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    @PostMapping("/invoices/{invoiceId}/approve")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> approveInvoice(@PathVariable Long invoiceId, Authentication authentication) {
        userService.getUserIdFromAuthentication(authentication);
        try {
            employeeService.approveInvoice(invoiceId);
            return ResponseEntity.ok(Map.of("message", "Invoice approved successfully"));
        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    @PostMapping("/invoices/{invoiceId}/reject")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> rejectInvoice(@PathVariable Long invoiceId, Authentication authentication) {
        userService.getUserIdFromAuthentication(authentication);
        try {
            employeeService.rejectInvoice(invoiceId);
            return ResponseEntity.ok(Map.of("message", "Invoice rejected successfully"));
        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    @GetMapping("/dashboard-data")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Map<String, Object>> getDashboardSummary(Authentication authentication) {
        Long employeeId = userService.getUserIdFromAuthentication(authentication);
        Map<String, Object> summary = employeeService.getEmployeeDashboardSummary(employeeId);
        return ResponseEntity.ok(summary);
    }

    private ResponseEntity<?> handleError(RuntimeException e) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage(e.getMessage());
        error.setTimestamp(java.time.LocalDateTime.now());
        error.setStatus(400);
        return ResponseEntity.badRequest().body(error);
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<Invoice>> getAllInvoices(Authentication authentication) {
        userService.getUserIdFromAuthentication(authentication);
        List<Invoice> invoices = employeeService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<Transaction>> getAllTransactions(Authentication authentication) {
        userService.getUserIdFromAuthentication(authentication);
        List<Transaction> transactions = employeeService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/loans")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<Loan>> getAllLoans(Authentication authentication) {
        userService.getUserIdFromAuthentication(authentication);
        List<Loan> loans = employeeService.getAllLoans();
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/recent-transactions")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Map<String , Object>> getRecentTransactions(Authentication authentication) {
        userService.getUserIdFromAuthentication(authentication);
        List<Map<String, Object>> recentTransactions = employeeService.getRecentTransactions();
        return ResponseEntity.ok(Map.of("recentTransactions", recentTransactions));
    }
}
