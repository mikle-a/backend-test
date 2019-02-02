package com.revolut.backend.handler;

import com.revolut.backend.constants.HttpHeaders;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Some base functionality for handlers - requestId, request specific logger, access to parsed parameters
 */
public abstract class HandlerBase implements Handler<RoutingContext> {

    private static String REQUEST_ID = "requestId";
    private static String PARSED_PARAMS = "parsedParameters";
    private static String REQUEST_LOGGER = "requestLogger";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Get or create request specific logger responsible to print requestId in each logged message
     *
     * @param ctx routing context
     * @return request specific logger
     */
    protected RequestLogger requestLogger(RoutingContext ctx) {
        final String requestLoggerKey = requestLoggerKey(logger);

        RequestLogger requestLogger = ctx.get(requestLoggerKey);
        if (requestLogger == null) {
            final String requestId = requestId(ctx);

            ctx.put(requestLoggerKey, requestLogger = new RequestLogger() {
                @Override
                public void debug(String msg, Object... args) {
                    logger.debug(msgWithRequestId(requestId, msg), args);
                }

                @Override
                public void info(String msg, Object... args) {
                    logger.info(msgWithRequestId(requestId, msg), args);
                }

                @Override
                public void warn(String msg, Object... args) {
                    logger.warn(msgWithRequestId(requestId, msg), args);
                }

                @Override
                public void error(String msg, Object... args) {
                    logger.error(msgWithRequestId(requestId, msg), args);
                }
            });
        }

        return requestLogger;
    }

    /**
     * Get params parsed by vertx-validation. To get any {@link io.vertx.ext.web.api.validation.ValidationHandler}
     * should be registered before this handler
     *
     * @param ctx routing context
     * @return parsed params
     */
    protected RequestParameters requestParameters(RoutingContext ctx) {
        return ctx.get(PARSED_PARAMS);
    }

    /**
     * Request id is intended to simplify logging (happening on different threads) and troubleshooting.
     * Obtained from the {@value HttpHeaders#REQUEST_ID} header or generated as UUID.
     *
     * @param ctx routing context
     * @return request id
     */
    protected String requestId(RoutingContext ctx) {
        String requestId = ctx.get(REQUEST_ID);

        if (requestId == null) {
            ctx.put(REQUEST_ID, requestId = getOrCreateRequestId(ctx));
        }

        return requestId;
    }

    private String requestLoggerKey(Logger logger) {
        return logger.getName() + REQUEST_LOGGER;
    }

    private String getOrCreateRequestId(RoutingContext ctx) {
        final String requestId = ctx.request().getHeader(HttpHeaders.REQUEST_ID);
        return requestId == null ? UUID.randomUUID().toString() : requestId;
    }

    private static String msgWithRequestId(String requestId, String msg) {
        return "[R:" + requestId + "] " + msg;
    }

    interface RequestLogger {
        void debug(String msg, Object... args);

        void info(String msg, Object... args);

        void warn(String msg, Object... args);

        void error(String msg, Object... args);
    }

}
