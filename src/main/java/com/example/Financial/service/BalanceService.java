package com.example.Financial.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Financial.dto.AccountOperation;
import com.example.Financial.model.Account;
import com.example.Financial.repository.BalanceRepository;

@Service
public class BalanceService {
    private final BalanceRepository balanceRepository;
    private final BlockingQueue<AccountOperation> operationsQueue = new ArrayBlockingQueue<>(5);

    public BalanceService(BalanceRepository balanceRepository) {
        this.balanceRepository = balanceRepository;
        // BalanceService의 생성자에서 호출되어, 서비스가 생성될 때 자동으로 큐에서 작업을 처리할 소비자 스레드가 시작
        startConsumerThread();
    }

    public void startConsumerThread() {
        new Thread(() -> {
            try {
                while (true) {
                    // 큐에서 작업을 기다리며 가져옴
                    AccountOperation operation = operationsQueue.take();
                    // 가져온 작업을 처리
                    processOperation(operation);
                }
            } catch (InterruptedException e) {
                 // 스레드 인터럽트 상태를 설정하여 스레드가 종료될 수 있게 함
                Thread.currentThread().interrupt();
            }
        // 스레드 시작
        }).start();
    }

    // 스레드가 별도로 생성되어 실행되고 있으므로, 스프링의 @Transactional이 제대로 작동 X
    // @Transactional
    // public void processOperation(AccountOperation operation) {
    //     Account account = balanceRepository.findById(operation.getAccountId()).orElseThrow();
    //     switch (operation.getType()) {
    //         case DEPOSIT:
    //             account.setBalance(account.getBalance() + operation.getAmount());
    //             break;
    //         case WITHDRAW:
    //             account.setBalance(account.getBalance() - operation.getAmount());
    //             break;
    //     }
    //     balanceRepository.save(account);
    // }

    // 별도의 트랜잭션 관리 메소드 추가
    @Transactional
    public void updateAccountBalance(Long accountId, Long amount, AccountOperation.OperateType type) {
        Account account = balanceRepository.findById(accountId).orElseThrow();
        if (type == AccountOperation.OperateType.DEPOSIT) {
            account.setBalance(account.getBalance() + amount);
        } else if (type == AccountOperation.OperateType.WITHDRAW) {
            account.setBalance(account.getBalance() - amount);
        }
        balanceRepository.save(account);
    }

    // 수정된 processOperation 메소드
    public void processOperation(AccountOperation operation) {
        updateAccountBalance(operation.getAccountId(), operation.getAmount(), operation.getType());
    }


    // 큐에 작업을 추가
    public void addOperation(AccountOperation operation) throws InterruptedException {
        operationsQueue.put(operation);
    }

    @Transactional(readOnly = true)
    public Account getBalance(Long id) {
        return balanceRepository.findById(id).orElseThrow();
    }
}
