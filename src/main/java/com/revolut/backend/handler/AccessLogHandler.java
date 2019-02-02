package com.revolut.backend.handler;

import io.vertx.ext.web.RoutingContext;

/**
 * Simply logs request uri and response status
 */
public class AccessLogHandler extends HandlerBase {

    public static AccessLogHandler INSTANCE = new AccessLogHandler();

    private AccessLogHandler() {
    }

    @Override
    public void handle(RoutingContext ctx) {
        requestLogger(ctx).info("Request received: '{}'", ctx.request().uri());
        ctx.addBodyEndHandler(v -> requestLogger(ctx).info("Response code: '{}'", ctx.response().getStatusCode()));
        ctx.next();
    }
}
