package com.revolut.backend.handler;

import com.codahale.metrics.*;
import com.revolut.backend.utils.Reply;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.Objects;

/**
 * Expose metrics of types gauge, counter, timer, meter
 */
public class ExposeMetricsHandler implements Handler<RoutingContext> {

    private final MetricRegistry metricRegistry;

    public ExposeMetricsHandler(MetricRegistry metricRegistry) {
        Objects.requireNonNull(metricRegistry, "metricRegistry must not be null");
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void handle(RoutingContext ctx) {
        final JsonObject json = new JsonObject();

        addGauges(json, metricRegistry.getGauges());
        addCounters(json, metricRegistry.getCounters());
        addTimers(json, metricRegistry.getTimers());
        addMetered(json, metricRegistry.getMeters());

        Reply.json(ctx, json);
    }

    private static void addGauges(JsonObject json, Map<String, Gauge> metrics) {
        for (Map.Entry<String, Gauge> entry : metrics.entrySet()) {
            json.put(entry.getKey(), entry.getValue().getValue().toString());
        }
    }

    private static void addCounters(JsonObject json, Map<String, Counter> metrics) {
        for (Map.Entry<String, Counter> entry : metrics.entrySet()) {
            json.put(entry.getKey(), entry.getValue().getCount());
        }
    }

    private static void addMetered(JsonObject json, Map<String, ? extends Metered> metrics) {
        for (Map.Entry<String, ? extends Metered> entry : metrics.entrySet()) {
            addMetered(json, entry.getKey(), entry.getValue());
        }
    }

    private static void addTimers(JsonObject json, Map<String, Timer> metrics) {
        for (Map.Entry<String, Timer> entry : metrics.entrySet()) {
            final Timer timer = entry.getValue();
            addMetered(json, entry.getKey(), timer);

            final Snapshot snapshot = timer.getSnapshot();
            json.put(entry.getKey() + ".999percentile", snapshot.get999thPercentile());
            json.put(entry.getKey() + ".99percentile", snapshot.get99thPercentile());
            json.put(entry.getKey() + ".95percentile", snapshot.get95thPercentile());
            json.put(entry.getKey() + ".75percentile", snapshot.get75thPercentile());
            json.put(entry.getKey() + ".max", snapshot.getMax());
            json.put(entry.getKey() + ".min", snapshot.getMin());
            json.put(entry.getKey() + ".mean", snapshot.getMean());
        }
    }

    private static void addMetered(JsonObject json, String name, Metered metered) {
        json.put(name + ".count", metered.getCount());
        json.put(name + ".15min.rate", metered.getFifteenMinuteRate());
        json.put(name + ".5min.rate", metered.getFiveMinuteRate());
        json.put(name + ".1min.rate", metered.getOneMinuteRate());
        json.put(name + ".mean.rate", metered.getMeanRate());
    }
}

