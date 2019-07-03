package com.revolut.backend;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.revolut.backend.db.Database;
import com.revolut.backend.db.impl.H2Database;
import com.revolut.backend.handler.*;
import com.revolut.backend.utils.Args;
import com.revolut.backend.utils.Utils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.ValidationHandler;
import org.apache.commons.cli.*;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.revolut.backend.constants.PathParams.ACCOUNT_ID;
import static com.revolut.backend.constants.PathParams.TRANSFER_ID;

/**
 * Server provides RESTful API for money transfers between accounts.
 * Vertx is used for handling http and internal storage is backed with H2.
 */
public class BackendServer {

    private static final Logger logger = LoggerFactory.getLogger(BackendServer.class);

    private final Database database;
    private final ThreadPoolExecutor dbExecutor;
    private final Vertx vertx;
    private final HttpServer httpServer;
    private final int port;
    private final MetricRegistry metricRegistry;

    public BackendServer(int port) {
        Args.isTrue(port > 0, "port must greater 0");
        this.metricRegistry = new MetricRegistry();
        this.dbExecutor = dbExecutorService(10, 50);
        this.database = new H2Database(9123, dbExecutor,
                JdbcConnectionPool.create("jdbc:h2:mem:db", "user", "pass"));
        this.vertx = Vertx.vertx();
        this.httpServer = vertx.createHttpServer();
        this.port = port;
    }

    public BackendServer start() {
        database.init();
        initMetrics();
        initHttpServer();
        return this;
    }

    public void stop() {
        Utils.<Void>await(h -> httpServer.close(h));
        database.stop();
    }

    public int port() {
        return port;
    }

    private void initHttpServer() {
        final Router router = Router.router(vertx);
        initRoute(router.route(HttpMethod.POST, "/user"), new CreateUserHandler(database));
        initRoute(router.route(HttpMethod.GET, String.format("/account/:%s", ACCOUNT_ID)), new GetAccountHandler(database));
        initRoute(router.route(HttpMethod.POST, "/account"), new CreateAccountHandler(database));
        initRoute(router.route(HttpMethod.GET, String.format("/transfer/:%s", TRANSFER_ID)), new GetTransferHandler(database));
        initRoute(router.route(HttpMethod.PATCH, String.format("/account/:%s/transfer", ACCOUNT_ID)), new TransferHandler(database));
        initRoute(router.route(HttpMethod.GET, "/metrics"), new ExposeMetricsHandler(metricRegistry));
        initRoute(router.route(), new EndpointNotFoundHandler());

        Utils.<HttpServer>await(h -> httpServer.requestHandler(router).listen(port, h));

        logger.info("HTTP server is ready to accept traffic on port {}", port);
    }

    private void initMetrics() {
        metricRegistry.register("db.queue.size", (Gauge<Integer>) () -> dbExecutor.getQueue().size());
        metricRegistry.register("threads", new ThreadStatesGaugeSet());
        metricRegistry.register("memory", new MemoryUsageGaugeSet());
        metricRegistry.register("jvm", new JvmAttributeGaugeSet());
        metricRegistry.register("gc", new GarbageCollectorMetricSet());
    }

    private void initRoute(Route route, Handler<RoutingContext> endpointHandler) {
        route.handler(new MeteringHandler(metricRegistry));
        route.handler(AccessLogHandler.INSTANCE);

        if (endpointHandler instanceof ValidatorHolder) {
            final ValidationHandler validator = ((ValidatorHolder) endpointHandler).getValidator();
            if (validator != null) {
                route.handler(validator);
            }
        }

        route.handler(endpointHandler);
        route.failureHandler(FailureHandler.INSTANCE);
    }

    private static ThreadPoolExecutor dbExecutorService(int threads, int queueSize) {
        Args.isTrue(threads > 0, "Threads count should be greater 0");
        Args.isTrue(queueSize > 0, "Queue size should be greater 0");

        return new ThreadPoolExecutor(threads, threads,
                5000L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize, true),
                new ThreadFactoryBuilder().setNameFormat("db-worker-%d").build(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    public static void main(String[] args) {
        final Options options = new Options()
                .addOption(new Option("p", "port", true, "http port"));

        try {
            final CommandLine parse = new BasicParser().parse(options, args);
            final String portString = parse.getOptionValue("p");

            try {
                final int port = portString != null ? Integer.parseInt(portString) : 8080;

                final BackendServer server = new BackendServer(port).start();
                Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "shutdown-hook"));
            } catch (NumberFormatException e) {
                System.err.println("Invalid port value: " + portString);
            }

        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("server", options);
        }
    }

}
