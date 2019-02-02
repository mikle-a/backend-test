package com.revolut.backend.db;

import java.math.BigDecimal;

/**
 * Callback for {@link Database#createAccount(long, BigDecimal, CreateAccountCallback)}
 */
public interface CreateAccountCallback {

    /**
     * Will be called if account creation succeed
     *
     * @param accountId created account id
     */
    void onSuccess(long accountId);

    /**
     * Will be called if user not found
     */
    void onUserNotFound();

    /**
     * Will be called on any unexpected error
     *
     * @param e never null
     */
    void onUnexpectedError(Exception e);
}
