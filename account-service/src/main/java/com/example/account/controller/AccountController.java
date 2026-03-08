package com.example.account.controller;

import com.example.account.entity.Account;
import com.example.account.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Account> create(@RequestBody Account account) {
        System.out.println("account: " + account);
        accountService.create(account);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{number}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Account> getUser(@PathVariable String number) {
        Optional<Account> user = accountService.getAccount(number);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
