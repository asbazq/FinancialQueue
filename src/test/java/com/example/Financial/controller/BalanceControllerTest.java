package com.example.Financial.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import com.example.Financial.model.Account;
import com.example.Financial.repository.BalanceRepository;
import com.example.Financial.service.BalanceService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
public class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BalanceController controller;

    @MockBean
    private BalanceService balanceService;

    @MockBean
    private BalanceRepository repository;

    @Test
    void testGetBalance() throws Exception {
        Long id = 1L;
        Account account = new Account(id, 1000L, System.currentTimeMillis(), System.nanoTime());
        given(balanceService.getBalance(id)).willReturn(account);

        mockMvc.perform(get("/account/" + id + "/balance"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":1,\"balance\":1000,\"updateMilli\":"+account.getUpdateMilli()+",\"updateNano\":"+account.getUpdateNano()+"}"));
    }

    @SuppressWarnings("null")
    @Test
    public void testDeposit() throws Exception {
        Long id = 1L;
        Long amount = 500L;
        Account account = new Account(1L, 1500L, System.currentTimeMillis(), System.nanoTime());
        given(balanceService.deposit(id, amount)).willReturn(account);

        mockMvc.perform(post("/account/" + id + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(amount.toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":1,\"balance\":1500,\"updateMilli\":"+account.getUpdateMilli()+",\"updateNano\":"+account.getUpdateNano()+"}"));
    }


    @SuppressWarnings("null")
    @Test
    public void testWithdraw() throws Exception {
        Long id = 1L;
        Long amount = 500L;
        Account account = new Account(1L, 1000L, System.currentTimeMillis(), System.nanoTime());
        given(balanceService.withdraw(id, amount)).willReturn(account);

        mockMvc.perform(post("/account/" + id + "/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(amount.toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":1,\"balance\":1000,\"updateMilli\":"+account.getUpdateMilli()+",\"updateNano\":"+account.getUpdateNano()+"}"));
    }

    @Test
    public void testConcurrentDeposit() throws Exception {
        // 새로운 고정 스레드 풀을 생성하여 동시에 두 개의 요청을 처리할 수 있도록 함
        ExecutorService service = Executors.newFixedThreadPool(2);
        // 두 개의 입금 요청을 스레드 풀에 제출, 각 요청은 서로 다른 스레드에서 동시에 실행
        Future<ResponseEntity<Account>> future1 = service.submit(() -> controller.deposit(1L, 5000L));
        Future<ResponseEntity<Account>> future2 = service.submit(() -> controller.deposit(1L, 5000L));

        // Future.get() 메서드를 사용하여 각 요청의 결과를 가져옴. get() 메서드는 요청이 완료될 때까지 대기
        ResponseEntity<Account> response1 = future1.get();
        ResponseEntity<Account> response2 = future2.get();

        // 하나의 요청만 성공하고 하나의 요청만 실패하는지 확인. 성공은 HTTP 상태 코드 2xx, 실패는 2xx가 아닌 코드
        boolean oneSuccessOneFail = (response1.getStatusCode().is2xxSuccessful() && !response2.getStatusCode().is2xxSuccessful())
                || (!response1.getStatusCode().is2xxSuccessful() && response2.getStatusCode().is2xxSuccessful());
        assertTrue(oneSuccessOneFail);
    }

    @Test
    public void testConcurrentDepositWithdraw() throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(2);
        Long id = 3L;
        Account testAccount = new Account(id, 0L, System.currentTimeMillis(), System.nanoTime());
        when(balanceService.getBalance(id)).thenReturn(testAccount);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(2); // 2개의 스레드가 종료되기를 기다립니다.

        // 입금 스레드
        service.submit(() -> {
            try {
                startGate.await(); // 모든 스레드가 동시에 시작하도록 하기 위해 대기
                testAccount.setBalance(testAccount.getBalance() + 5000L);
                when(balanceService.deposit(id, 5000L)).thenReturn(testAccount);
                controller.deposit(id, 5000L);
            } catch (InterruptedException ignored) {
            } finally {
                endGate.countDown(); // 작업 완료 알림
            }
        });

        // 출금 스레드
        service.submit(() -> {
            try {
                startGate.await(); // 모든 스레드가 동시에 시작하도록 하기 위해 대기
                testAccount.setBalance(testAccount.getBalance() - 3000L);
                when(balanceService.withdraw(id, 3000L)).thenReturn(testAccount);
                controller.withdraw(id, 3000L);
            } catch (InterruptedException ignored) {
            } finally {
                endGate.countDown(); // 작업 완료 알림
            }
        });

        startGate.countDown(); // 모든 스레드가 동시에 시작하도록 함
        endGate.await(); // 모든 스레드가 종료될 때까지 대기

        // Check the final balance
        Account finalAccount = balanceService.getBalance(id);
        assertEquals(2000L, finalAccount.getBalance());
    }

    @Test
    public void testConcurrentWithdraw() throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(2);
        Long id = 2L;
        Account testAccount = new Account(id, 10000L, System.currentTimeMillis(), System.nanoTime());
        when(balanceService.getBalance(id)).thenReturn(testAccount);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(2); // 2개의 스레드가 종료되기를 기다립니다.

        // 출금 스레드1
        service.submit(() -> {
            try {
                startGate.await(); // 모든 스레드가 동시에 시작하도록 하기 위해 대기
                testAccount.setBalance(testAccount.getBalance() - 3000L);
                when(balanceService.withdraw(id, 3000L)).thenReturn(testAccount);
                controller.withdraw(id, 3000L);
            } catch (InterruptedException ignored) {
            } finally {
                endGate.countDown(); // 작업 완료 알림
            }
        });

        // 출금 스레드2
        service.submit(() -> {
            try {
                startGate.await(); // 모든 스레드가 동시에 시작하도록 하기 위해 대기
                testAccount.setBalance(testAccount.getBalance() - 3000L);
                when(balanceService.withdraw(id, 3000L)).thenReturn(testAccount);
                controller.withdraw(id, 3000L);
            } catch (InterruptedException ignored) {
            } finally {
                endGate.countDown(); // 작업 완료 알림
            }
        });

        startGate.countDown(); // 모든 스레드가 동시에 시작하도록 함
        endGate.await(); // 모든 스레드가 종료될 때까지 대기

        // Check the final balance
        Account finalAccount = balanceService.getBalance(id);
        assertEquals(4000L, finalAccount.getBalance());
    }
}
