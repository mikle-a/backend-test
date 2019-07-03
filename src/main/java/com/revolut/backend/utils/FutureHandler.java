package com.revolut.backend.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.concurrent.CompletableFuture;

public class FutureHandler<T> implements Handler<AsyncResult<T>> {

    private final CompletableFuture<T> f = new CompletableFuture<>();

    @Override
    public void handle(AsyncResult<T> result) {
        if (result.failed()) {
           f.completeExceptionally(result.cause());
        } else {
            f.complete(result.result());
        }
    }

    public CompletableFuture<T> future() {
        return f;
    }
}
