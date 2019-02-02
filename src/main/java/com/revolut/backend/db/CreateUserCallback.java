package com.revolut.backend.db;

/**
 * Callback for {@link Database#createUser(String, CreateUserCallback)}}
 */
public interface CreateUserCallback {

    /**
     * Will be called if user creation succeed
     *
     * @param userId created user id
     */
    void onSuccess(long userId);

    /**
     * Will be called on any unexpected error
     *
     * @param e never null
     */
    void onUnexpectedError(Exception e);

}
