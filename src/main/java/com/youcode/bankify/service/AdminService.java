package com.youcode.bankify.service;


import com.youcode.bankify.dto.RegisterRequest;
import com.youcode.bankify.dto.UpdatedUserRequest;
import com.youcode.bankify.entity.BankAccount;
import com.youcode.bankify.entity.Role;
import com.youcode.bankify.entity.User;
import com.youcode.bankify.repository.jpa.AccountRepository;
import com.youcode.bankify.repository.jpa.RoleRepository;
import com.youcode.bankify.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(RegisterRequest registerRequest){
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEnabled(true);
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setDateOfBirth(registerRequest.getDateOfBirth());
        user.setIdentityNumber(registerRequest.getIdentityNumber());

        LocalDate birthDate = registerRequest.getDateOfBirth();
        if(birthDate != null){
            int age = Period.between(birthDate, LocalDate.now()).getYears();
            user.setAge(age);
        }

        Set<Role> roles = registerRequest.getRoles().stream()
                .map(roleName -> roleRepository.findByName(roleName).orElseThrow(() -> new RuntimeException("Role not found ")))
                .collect(Collectors.toSet());
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public User updateUser(Long userId, UpdatedUserRequest userDetails){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(userDetails.getUsername());
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setDateOfBirth(userDetails.getDateOfBirth());
        user.setIdentityNumber(userDetails.getIdentityNumber());
        user.setEnabled(true);

        if(userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()){
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        Set<Role> roles = userDetails.getRoles().stream()
                        .map(roleName -> roleRepository.findByName(roleName)
                                .orElseThrow(() -> new RuntimeException("Role not found" + roleName)))
                                .collect(Collectors.toSet());
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public void deleteUser(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);

    }

    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

    public BankAccount updateAccountStatus(Long accountId , String status){
        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        account.setStatus(status.toUpperCase());
        return accountRepository.save(account);
    }

    public List<BankAccount> getAllBankAccounts(){return accountRepository.findAll();}

    public Map<String, Object> getDashboardSummary() {
        long totalUsers = userRepository.count();
        long activeAccounts = accountRepository.countByStatus("ACTIVE");
        long inactiveAccounts = accountRepository.countByStatus("INACTIVE");

        List<User> recentUsers = userRepository.findTop5ByOrderByIdDesc();
        List<BankAccount> recentAccounts = accountRepository.findTop5ByOrderByIdDesc();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalUsers", totalUsers);
        summary.put("activeAccounts", activeAccounts);
        summary.put("inactiveAccounts", inactiveAccounts);
        summary.put("recentUsers", recentUsers);
        summary.put("recentAccounts", recentAccounts);
        return summary;
    }

}
