package com.example.Financial.dto;

import lombok.Getter;

@Getter
public class AccountOperation {
    private Long accountId;
    private OperateType type;
    private Long amount;

    public AccountOperation(Long accountId, OperateType type, Long amount) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
    }

    public enum OperateType {
        DEPOSIT,
        WITHDRAW
    }
}
