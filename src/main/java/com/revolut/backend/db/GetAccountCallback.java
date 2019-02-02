package com.revolut.backend.db;

import com.revolut.backend.entity.Account;

/**
 * Callback for {@link Database#getAccount(long, long, GetAccountCallback)}}
 */
public interface GetAccountCallback {

    /**
     * Will be called if account found and belongs to the requester
     *
     * @param account never null
     */
    void onSuccess(Account account);

    /**
     * Wil be called if account not found or does not belong to the requester
     */
    void onAccountNotFound();

    /**
     * Will be called on any unexpected error
     *
     * @param e never null
     */
    void onUnexpectedError(Exception e);

}
