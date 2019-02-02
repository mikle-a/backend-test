package com.revolut.backend.load.test;

import com.revolut.backend.AbstractBackendServerTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BackendServerLoadTest extends AbstractBackendServerTest {

    @Test
    public void transfer_miniLoadTest_shouldRespondWithin50ms() throws ExecutionException, InterruptedException {
        //create two users
        final long user1 = createUser("Mike");
        final long user2 = createUser("John");

        //create account per each user
        final BigDecimal user1AccountInitialBalance = BigDecimal.valueOf(100);
        final long user1AccountId = createAccount(user1, user1AccountInitialBalance);

        final BigDecimal user2AccountInitialBalance = BigDecimal.valueOf(100);
        final long user2AccountId = createAccount(user2, user2AccountInitialBalance);

        //make a lot of transfers between accounts in parallel
        final int attempts = 201;
        final ArrayList<CompletableFuture> futures = new ArrayList<>(attempts * 2);
        for (int i = 0; i < attempts; i++) {
            logger.info("Attempt {}", i);
            futures.add(
                    sendAsync(HttpMethod.PATCH, transferQuery(user2AccountId, user1AccountId, new BigDecimal("0.35")), user2)
            );
            futures.add(
                    sendAsync(HttpMethod.PATCH, transferQuery(user1AccountId, user2AccountId, new BigDecimal("0.17")), user1)
            );
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        //check balances
        assertEquals(new BigDecimal("63.82"), getAccountBalance(user2, user2AccountId));
        assertEquals(new BigDecimal("136.18"), getAccountBalance(user1, user1AccountId));

        //check http metrics
        final HttpResponse<Buffer> response = sendSync(HttpMethod.GET, "/metrics");
        assertEquals(200, response.statusCode());

        final JsonObject metrics = response.body().toJsonObject();
        assertEquals(Long.valueOf(attempts), metrics.getLong("http./account/1/transfer.PATCH.count"));
        final long percentile95ms = TimeUnit.NANOSECONDS.toMillis(metrics.getLong("http./account/1/transfer.PATCH.95percentile"));
        assertTrue("95 percentile should be less 50ms", percentile95ms < 50);
    }

}
