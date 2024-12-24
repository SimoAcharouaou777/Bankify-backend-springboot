package com.youcode.bankify.controller;


import com.youcode.bankify.dto.RegisterRequest;
import com.youcode.bankify.dto.UpdatedUserRequest;
import com.youcode.bankify.entity.BankAccount;
import com.youcode.bankify.entity.User;
import com.youcode.bankify.service.AdminService;
import com.youcode.bankify.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private UserService userService;


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<User> createUser( @RequestBody RegisterRequest registerRequest){
        User createdUser = adminService.createUser(registerRequest);
        return ResponseEntity.ok(createdUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser( @PathVariable Long id , @RequestBody UpdatedUserRequest userDetails){
        User updatedUser = adminService.updateUser(id,userDetails);
        return ResponseEntity.ok(updatedUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser( @PathVariable Long id){
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(){
        List<User> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/accounts/{accountId}/status")
    public ResponseEntity<BankAccount> updateAccountStatus(@PathVariable Long accountId, @RequestParam String status){
        BankAccount updatedAccount = adminService.updateAccountStatus(accountId, status);
        return ResponseEntity.ok(updatedAccount);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccount>> getAllBankAccounts(){
        List<BankAccount> bankAccounts = adminService.getAllBankAccounts();
        return ResponseEntity.ok(bankAccounts);
    }




}
