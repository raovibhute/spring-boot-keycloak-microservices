package com.example.account.service;

import com.example.account.entity.Account;
import com.example.account.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account create(Account user) {
        return accountRepository.save(user);
    }

    public Optional<Account> getAccount(String number) {
        return Optional.of(accountRepository.findByAccountNumber(number).orElseThrow());
    }
}
