package com.youcode.bankify.service;

import com.youcode.bankify.dto.LoanRequestDTO;
import com.youcode.bankify.dto.LoanResponseDTO;
import com.youcode.bankify.entity.Loan;
import com.youcode.bankify.entity.User;
import com.youcode.bankify.repository.jpa.LoanRepository;
import com.youcode.bankify.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;

    public Loan applyForLoan(LoanRequestDTO request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getAge() < 18) {
            throw new RuntimeException("User must be at least 18 years old to apply for a loan");
        }

        Loan loan = new Loan();
        loan.setAmount(request.getAmount());
        loan.setTermInMonths(request.getTermInMonths());
        loan.setStatus("PENDING");
        loan.setApplicationDate(LocalDateTime.now());
        loan.setUser(user);

        return loanRepository.save(loan);
    }

    public List<LoanResponseDTO> getLoans(Long userId) {
        List<Loan> loans = loanRepository.findByUserId(userId);
        return loans.stream().map(loan -> {
            LoanResponseDTO dto = new LoanResponseDTO();
            dto.setId(loan.getId());
            dto.setAmount(loan.getAmount());
            dto.setTermInMonths(loan.getTermInMonths());
            dto.setStatus(loan.getStatus());
            dto.setApplicationDate(loan.getApplicationDate());
            return dto;
        }).collect(Collectors.toList());
    }

    public Loan approveOrRejectLoan(Long loanId, String status) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        loan.setStatus(status);
        return loanRepository.save(loan);
    }
}
