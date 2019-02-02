package com.revolut.backend.db;

import java.math.BigDecimal;

/**
 * Callback for {@link Database#transfer(String, long, long, long, BigDecimal, TransferCallback)}}
 */
public interface TransferCallback {

    /**
     * Will be called if transfer succeed
     *
     * @param transferId create transfer id
     */
    void onSuccess(long transferId);

    /**
     * Will be called if one of account not found
     *
     * @param accountId not found account id
     */
    void onAccountNotFound(long accountId);

    /**
     * Will be called if source account does not belong to the requester
     */
    void onNotOwner();

    /**
     * Will be called if source account balance < requested amount
     */
    void onInsufficientFunds();

    /**
     * Will be called on any unexpected error
     *
     * @param e never null
     */
    void onUnexpectedError(Exception e);

}
