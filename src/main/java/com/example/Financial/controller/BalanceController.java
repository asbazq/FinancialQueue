package com.example.Financial.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Financial.dto.AccountOperation;
import com.example.Financial.model.Account;
import com.example.Financial.service.BalanceService;


@RestController
@RequestMapping("/account")
// JpaRepository를 사용한다면 데이터베이스 레벨에서 적절한 락을 사용하여 동시성을 관리, thread-safe를 위해 ConcurrentHashMap 을 사용할 필요X
public class BalanceController {

    private final BalanceService balanceService;

    // 생성자 주입
    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("{id}/balance")
    public ResponseEntity<Account> getBalance(@PathVariable Long id) {
        return ResponseEntity.ok(balanceService.getBalance(id));
    }

    @PostMapping("{id}/deposit")
    public ResponseEntity<?> deposit(@PathVariable Long id, @RequestBody Long amount) {
        try {
            balanceService.addOperation(new AccountOperation(id, AccountOperation.OperateType.DEPOSIT, amount));
            return ResponseEntity.ok().build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body("Operation interrupted.");
        }
    }

    @PostMapping("{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable Long id, @RequestBody Long amount) {
        try {
            balanceService.addOperation(new AccountOperation(id, AccountOperation.OperateType.WITHDRAW, amount));
            return ResponseEntity.ok().build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body("Operation interrupted.");
        }
    }
}