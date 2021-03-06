package com.revolut.backend.utils;

import io.vertx.ext.web.api.validation.ValidationException;

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class Utils {

    /**
     * Vetx api validation does not support BigDecimal type validation out of the box and there is no method
     * to obtain raw value from the {@link io.vertx.ext.web.api.RequestParameter}
     *
     * @param amountString amount as a string
     * @return parsed amount, never null
     * @throws ValidationException is amountString not a valid representation of a {@code BigDecimal}
     */
    public static BigDecimal parseDecimal(String amountString) throws ValidationException {
        try {
            return new BigDecimal(amountString);
        } catch (NumberFormatException e) {
            throw ValidationException.ValidationExceptionFactory
                    .generateNotMatchValidationException("Value is not a valid amount");
        }
    }

    public static <T> T await(Consumer<FutureHandler<T>> func) {
        final FutureHandler<T> h = new FutureHandler<>();
        func.accept(h);
        try {
            return h.future().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
