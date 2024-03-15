package com.example.Financial.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Getter
@Entity
@Table(name = "account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balance")
    private Long balance;

    // 시간이 변경되지 않도록 불변성을 가져야함
    @Column(name = "update_milli")
    private Long updateMilli;

    @Column(name = "update_nano")
    private Long updateNano;

    public Account(Long id, Long balance, Long updateMilli, Long updateNano) {
        this.id = id;
        this.balance = balance;
        this.updateMilli = updateMilli;
        this.updateNano = updateNano;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
        this.updateMilli = System.currentTimeMillis();
        this.updateNano = System.nanoTime();
    }
}
