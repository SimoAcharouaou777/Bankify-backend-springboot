package com.youcode.bankify.service;


import com.youcode.bankify.dto.RecentApprovalDTO;
import com.youcode.bankify.entity.BankAccount;
import com.youcode.bankify.entity.Invoice;
import com.youcode.bankify.entity.Loan;
import com.youcode.bankify.entity.Transaction;
import com.youcode.bankify.repository.jpa.AccountRepository;
import com.youcode.bankify.repository.jpa.InvoiceRepository;
import com.youcode.bankify.repository.jpa.LoanRepository;
import com.youcode.bankify.repository.jpa.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final InvoiceRepository invoiceRepository;

    public List<BankAccount> getCustomerAccounts(){
        return accountRepository.findAll();
    }

    public void approveTransaction(Long transactionId){
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        transaction.setStatus("APPROVED");
        transactionRepository.save(transaction);
    }

    public void rejectTransaction(Long transactionId){
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        transaction.setStatus("REJECTED");
        transactionRepository.save(transaction);
    }

    public void approveLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        loan.setStatus("APPROVED");
        loanRepository.save(loan);
    }

    public void rejectLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        loan.setStatus("REJECTED");
        loanRepository.save(loan);
    }

    public void approveInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        invoice.setStatus("APPROVED");
        invoiceRepository.save(invoice);
    }

    public void rejectInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        invoice.setStatus("REJECTED");
        invoiceRepository.save(invoice);
    }

    public Map<String,Object> getEmployeeDashboardSummary(Long employeeId) {
        int pendingTransactions = transactionRepository.countByStatus("PENDING");
        int pendingLoans = loanRepository.countByStatus("PENDING");
        int pendingInvoices = invoiceRepository.countByStatus("PENDING");

        Map<String,Object> summary = new HashMap<>();
        summary.put("pendingTransactions", pendingTransactions);
        summary.put("pendingLoans", pendingLoans);
        summary.put("pendingInvoices", pendingInvoices);

        return summary;
    }

    public List<Map<String,Object>> getRecentTransactions() {
        List<Transaction> recentTransactions = transactionRepository.findTop5ByOrderByDateDesc();
        return recentTransactions.stream()
                .map(transaction -> {
                    Map<String ,Object> transactionData = new HashMap<>();
                    transactionData.put("id", transaction.getId());
                    transactionData.put("type", transaction.getType());
                    transactionData.put("amount", transaction.getAmount());
                    transactionData.put("date", transaction.getDate());
                    transactionData.put("status", transaction.getStatus());
                    return transactionData;
                })
                .collect(Collectors.toList());
    }

    public List<Invoice> getAllInvoices(){
        return invoiceRepository.findAll();
    }

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

}
