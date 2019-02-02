package com.revolut.backend.db;

import java.math.BigDecimal;

/**
 * Essentially, database is a concurrent safe storage responsible for storing data and transfering means
 * between accounts. It is designed in an asynchronous manner in order to avoid blocking event-loop threads.
 * All methods are allowed to throw {@link java.util.concurrent.RejectedExecutionException} whenever
 * execution queue is full.
 */
public interface Database {

    /**
     * Create user
     *
     * @param userName non null user name
     * @param callback non null callback to be executed after completion
     * @throws java.util.concurrent.RejectedExecutionException whenever execution queue is full
     */
    void createUser(String userName, CreateUserCallback callback);

    /**
     * Create account for an existing user
     *
     * @param userId   id of existing user
     * @param balance  non null initial balance
     * @param callback non null callback to be executed after completion
     * @throws java.util.concurrent.RejectedExecutionException whenever execution queue is full
     */
    void createAccount(long userId, BigDecimal balance, CreateAccountCallback callback);

    /**
     * Transfer means from one existing account to another
     *
     * @param requestId    non null request id to be logged in the transfer
     * @param userId       requester id
     * @param srcAccountId source account id, should be owned by userId
     * @param dstAccountId destination account id
     * @param amount       non null amount to be transferred
     * @param callback     non null callback to be executed after completion
     * @throws java.util.concurrent.RejectedExecutionException whenever execution queue is full
     */
    void transfer(String requestId, long userId, long srcAccountId, long dstAccountId, BigDecimal amount, TransferCallback callback);

    /**
     * Get existing account
     *
     * @param userId    requester id
     * @param accountId existing account id, owned by userId
     * @param callback  non null callback to be executed after completion
     * @throws java.util.concurrent.RejectedExecutionException whenever execution queue is full
     */
    void getAccount(long userId, long accountId, GetAccountCallback callback);

    /**
     * Get existing transfer
     *
     * @param userId     requester id, should be the transfer owner
     * @param transferId transfer id
     * @param callback   non null callback to be called after completion
     */
    void getTransfer(long userId, long transferId, GetTransferCallback callback);

    /**
     * Initialization method to allow implementations do some stuff on startup
     */
    void init();

    /**
     * To shutdown gracefully
     */
    void stop();

}
