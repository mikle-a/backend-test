package com.revolut.backend.handler;

import com.revolut.backend.utils.Reply;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles endpoint not found event and responds with 404
 */
public class EndpointNotFoundHandler extends HandlerBase {

    @Override
    public void handle(RoutingContext ctx) {
        requestLogger(ctx).warn("Endpoint '{}' not found", ctx.request().uri());
        Reply.endpointNotFound(ctx);
    }
}
