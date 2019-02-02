package com.revolut.backend.entity;

import com.revolut.backend.utils.Args;

import java.math.BigDecimal;
import java.util.Objects;

public class Account {

    private final long id;
    private final long ownerId;
    private final BigDecimal balance;

    public Account(long id, long ownerId, BigDecimal balance) {
        Args.isTrue(id > 0, "id must be greater zero");
        Args.isTrue(ownerId > 0, "id must be greater zero");
        Objects.requireNonNull(balance, "Balance must not be null");
        this.id = id;
        this.ownerId = ownerId;
        this.balance = balance;
    }

    public long getId() {
        return id;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
