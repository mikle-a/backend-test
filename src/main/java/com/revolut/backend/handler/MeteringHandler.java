package com.revolut.backend.handler;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

import java.util.concurrent.TimeUnit;

/**
 * Writes http metrics to the {@link MetricRegistry}
 */
public class MeteringHandler extends HandlerBase {

    private final MetricRegistry metricRegistry;

    public MeteringHandler(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void handle(RoutingContext ctx) {
        final Timer timer = metricRegistry.timer(routeTimerName(ctx.request()));
        final Timer.Context timerContext = timer.time();
        ctx.addBodyEndHandler(v -> {
            final long nanos = timerContext.stop();
            requestLogger(ctx).info("Request processed in {} ms", TimeUnit.NANOSECONDS.toMillis(nanos));
        });
        ctx.next();
    }

    private String routeTimerName(HttpServerRequest request) {
        return MetricRegistry.name("http", request.path(), request.method().toString());
    }
}
