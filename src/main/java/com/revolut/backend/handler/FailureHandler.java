package com.revolut.backend.handler;

import com.revolut.backend.utils.Reply;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles all uncaught exceptions. Replies with 400 for {@link ValidationException} and 500 for others
 */
public class FailureHandler implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(FailureHandler.class);

    public static final FailureHandler INSTANCE = new FailureHandler();

    private FailureHandler() {
    }

    @Override
    public void handle(RoutingContext ctx) {
        final Throwable failure = ctx.failure();
        if (failure instanceof ValidationException) {
            logger.warn("Request validation exception: {}", failure.getMessage());
            Reply.badRequest(ctx, failure.getMessage());
        } else {
            logger.error("Unexpected error occurred during handling the request '{}' : '{}'",
                    ctx.request().path(),
                    String.valueOf(ctx.failure()));
            logger.debug("Stacktrace", ctx.failure());
            Reply.unexpectedError(ctx, failure);
        }
    }
}
