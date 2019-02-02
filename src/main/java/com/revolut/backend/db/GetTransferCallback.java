package com.revolut.backend.db;

import com.revolut.backend.entity.Transfer;

/**
 * Callback for {@link Database#getTransfer(long, long, GetTransferCallback)}}
 */
public interface GetTransferCallback {

    /**
     * Will be called if transfer found and belongs to the requester
     *
     * @param transfer never null
     */
    void onSuccess(Transfer transfer);

    /**
     * Wil be called if transfer not found or does not belong to the requester
     */
    void onTransferNotFound();

    /**
     * Will be called on any unexpected error
     *
     * @param e never null
     */
    void onUnexpectedError(Exception e);

}
