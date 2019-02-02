package com.revolut.backend.entity;

import com.revolut.backend.utils.Args;

import java.math.BigDecimal;
import java.util.Objects;

public class Transfer {

    private final long id;
    private final String requestId;
    private final long timestamp;
    private final long srcAccountId;
    private final long dstAccountId;
    private final long userId;
    private final BigDecimal amount;
    private final BigDecimal srcAccountBalanceBefore;
    private final BigDecimal srcAccountBalanceAfter;
    private final BigDecimal dstAccountBalanceBefore;
    private final BigDecimal dstAccountBalanceAfter;

    public Transfer(long id,
                    String requestId,
                    long timestamp,
                    long srcAccountId,
                    long dstAccountId,
                    long userId,
                    BigDecimal amount,
                    BigDecimal srcAccountBalanceBefore,
                    BigDecimal srcAccountBalanceAfter,
                    BigDecimal dstAccountBalanceBefore,
                    BigDecimal dstAccountBalanceAfter) {
        Args.isTrue(id > 0, "id must be greater zero");
        Objects.requireNonNull(requestId, "requestId must not be null");
        Args.isTrue(timestamp > 0, "timestamp must be greater zero");
        Args.isTrue(srcAccountId > 0, "srcAccountId must be greater zero");
        Args.isTrue(dstAccountId > 0, "dstAccountId must be greater zero");
        Args.isTrue(userId > 0, "userId must be greater zero");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(srcAccountBalanceBefore, "srcAccountBalanceBefore must not be null");
        Objects.requireNonNull(srcAccountBalanceAfter, "srcAccountBalanceAfter must not be null");
        Objects.requireNonNull(dstAccountBalanceBefore, "dstAccountBalanceBefore must not be null");
        Objects.requireNonNull(dstAccountBalanceAfter, "dstAccountBalanceAfter must not be null");

        this.id = id;
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.srcAccountId = srcAccountId;
        this.dstAccountId = dstAccountId;
        this.userId = userId;
        this.amount = amount;
        this.srcAccountBalanceBefore = srcAccountBalanceBefore;
        this.srcAccountBalanceAfter = srcAccountBalanceAfter;
        this.dstAccountBalanceBefore = dstAccountBalanceBefore;
        this.dstAccountBalanceAfter = dstAccountBalanceAfter;
    }

    public long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getSrcAccountId() {
        return srcAccountId;
    }

    public long getDstAccountId() {
        return dstAccountId;
    }

    public long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getSrcAccountBalanceBefore() {
        return srcAccountBalanceBefore;
    }

    public BigDecimal getSrcAccountBalanceAfter() {
        return srcAccountBalanceAfter;
    }

    public BigDecimal getDstAccountBalanceBefore() {
        return dstAccountBalanceBefore;
    }

    public BigDecimal getDstAccountBalanceAfter() {
        return dstAccountBalanceAfter;
    }
}
