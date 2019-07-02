package com.revolut.backend;

import io.netty.handler.codec.http.QueryStringEncoder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public abstract class AbstractBackendServerTest {

    private static BackendServer server;
    private static WebClient webClient;

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void setUp() {
        server = new BackendServer(9999);
        server.start();

        webClient = WebClient.create(Vertx.vertx());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }


    protected HttpResponse<Buffer> sendSync(HttpMethod method, String path) {
        return sendSync(method, path, null);
    }

    protected HttpResponse<Buffer> sendSync(HttpMethod method, String path, Long userId) {
        try {
            return sendAsync(method, path, userId).get().result();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    protected CompletableFuture<AsyncResult<HttpResponse<Buffer>>> sendAsync(HttpMethod method, String path, Long userId) {
        final CompletableFuture<AsyncResult<HttpResponse<Buffer>>> f = new CompletableFuture<>();

        final HttpRequest<Buffer> request = webClient.request(method, server.port(), "127.0.0.1", path);

        if (userId != null) {
            request.putHeader("UserId", userId.toString());
        }

        logger.info("Send request:\n" +
                "\tmethod = '{}',\n" +
                "\tpath = '{}',\n" +
                "\theaders = '{}'", method, path, request.headers().entries());

        request.send(f::complete);

        f.whenComplete((r, t) -> {
            if (r != null) {
                logger.info("Response received:\n" +
                        "\tcode = '{}'\n" +
                        "\tbody = '{}'", r.result().statusCode(), r.result().body().toString());
            }
        });

        return f;
    }

    protected long createUser(String name) {
        final QueryStringEncoder query = new QueryStringEncoder("/user");
        query.addParam("userName", name);

        final HttpResponse<Buffer> createUserResponse = sendSync(HttpMethod.POST, query.toString());
        assertEquals(200, createUserResponse.statusCode());
        return createUserResponse.body().toJsonObject().getLong("userId");
    }

    protected long createAccount(long userId) {
        return createAccount(userId, BigDecimal.ZERO);
    }

    protected long createAccount(long userId, BigDecimal balance) {
        final QueryStringEncoder query = new QueryStringEncoder("/account");
        query.addParam("balance", String.valueOf(balance));

        final HttpResponse<Buffer> createAccountResponse = sendSync(HttpMethod.POST, query.toString(), userId);
        assertEquals(200, createAccountResponse.statusCode());
        return createAccountResponse.body().toJsonObject().getLong("accountId");
    }

    protected BigDecimal getAccountBalance(long userId, long accountId) {
        final HttpResponse<Buffer> getAccountJson = sendSync(HttpMethod.GET, "/account/" + accountId, userId);
        assertEquals(200, getAccountJson.statusCode());

        return new BigDecimal(getAccountJson.bodyAsJsonObject().getString("balance"));
    }

    protected void verifyTransfer(long transferId,
                                long userId,
                                BigDecimal amount,
                                BigDecimal srcAccBefore,
                                BigDecimal srcAccAfter,
                                BigDecimal dstAccBefore,
                                BigDecimal dstAccAfter) {

        final HttpResponse<Buffer> response = sendSync(HttpMethod.GET, "/transfer/" + transferId, userId);
        assertEquals(200, response.statusCode());

        final JsonObject json = response.bodyAsJsonObject();
        assertEquals(Long.valueOf(userId), json.getLong("userId"));
        assertEquals(amount, new BigDecimal(json.getString("amount")));
        assertEquals(srcAccBefore, new BigDecimal(json.getString("srcAccountBalanceBefore")));
        assertEquals(srcAccAfter, new BigDecimal(json.getString("srcAccountBalanceAfter")));
        assertEquals(dstAccBefore, new BigDecimal(json.getString("dstAccountBalanceBefore")));
        assertEquals(dstAccAfter, new BigDecimal(json.getString("dstAccountBalanceAfter")));
    }

    protected String transferQuery(long srcAccId, long dstAccId, Object amount) {
        final QueryStringEncoder query = new QueryStringEncoder(String.format("/account/%s/transfer", srcAccId));
        query.addParam("dstAccountId", String.valueOf(dstAccId));
        query.addParam("amount", String.valueOf(amount));
        return query.toString();
    }


}
