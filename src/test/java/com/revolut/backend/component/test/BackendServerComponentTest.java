package com.revolut.backend.component.test;

import com.revolut.backend.AbstractBackendServerTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BackendServerComponentTest extends AbstractBackendServerTest {

    @Test
    public void createUser_missingNameParameter_return400() {
        final HttpResponse<Buffer> response = sendSync(HttpMethod.POST, "/user");
        assertEquals(400, response.statusCode());
    }

    @Test
    public void createUser_ok_return200() {
        final long userId = createUser("Mike");
        assertTrue(userId > 0);
    }

    @Test
    public void createAccount_missingUserIdHeader_returns400() {
        final HttpResponse<Buffer> response = sendSync(HttpMethod.POST, "/account");
        assertEquals(400, response.statusCode());
    }

    @Test
    public void createAccount_unknownUser_returns404() {
        final HttpResponse<Buffer> response = sendSync(HttpMethod.POST, "/account?userId=999999", 999999L);
        assertEquals(404, response.statusCode());
    }

    @Test
    public void createAccount_invalidUserId_returns400() {
        final HttpResponse<Buffer> response = sendSync(HttpMethod.POST, "/account?userId=abc");
        assertEquals(400, response.statusCode());
    }

    @Test
    public void createAccount_ok_returns200() {
        final long userId = createUser("Mike");
        final long accountId = createAccount(userId);
        assertTrue(accountId > 0);
    }

    @Test
    public void getAccount_missingUserIdHeader_returns400() {
        final HttpResponse<Buffer> response = sendSync(HttpMethod.GET, "/account/1");
        assertEquals(400, response.statusCode());
    }

    @Test
    public void getAccount_invalidAccountId_returns400() {
        final long userId = createUser("Mike");
        final HttpResponse<Buffer> response = sendSync(HttpMethod.GET, "/account/abc", userId);
        assertEquals(400, response.statusCode());
    }

    @Test
    public void getAccount_unknown_returns404() {
        final long userId = createUser("Mike");
        final HttpResponse<Buffer> response = sendSync(HttpMethod.GET, "/account/999999", userId);
        assertEquals(404, response.statusCode());
    }

    @Test
    public void getAccount_notOwner_returns404() {
        final long mikeId = createUser("Mike");
        final long johnId = createUser("John");

        final long johnAccountId = createAccount(johnId);

        final HttpResponse<Buffer> response = sendSync(HttpMethod.GET, "/account/" + johnAccountId, mikeId);
        assertEquals(404, response.statusCode());
    }

    @Test
    public void getAccount_ok_returns200() {
        final long userId = createUser("Mike");

        final long accountId = createAccount(userId);

        final HttpResponse<Buffer> response = sendSync(HttpMethod.GET, "/account/" + accountId, userId);
        assertEquals(200, response.statusCode());
        final JsonObject json = response.bodyAsJsonObject();
        assertEquals(Long.valueOf(accountId), json.getLong("accountId"));
        assertEquals("0", json.getString("balance"));
    }

    @Test
    public void transfer_betweenOneUserAccounts_returns200() {
        final long userId = createUser("Mike");

        //create two accounts
        final BigDecimal account1InitialBalance = BigDecimal.valueOf(21.74);
        final long userAccount1 = createAccount(userId, account1InitialBalance);

        final BigDecimal account2InitialBalance = BigDecimal.valueOf(99.11);
        final long userAccount2 = createAccount(userId, account2InitialBalance);

        //transfer between account
        final BigDecimal amountToTransfer = BigDecimal.valueOf(12.43);
        final String query = transferQuery(userAccount1, userAccount2, amountToTransfer);

        final HttpResponse<Buffer> transferResponse = sendSync(HttpMethod.PATCH, query, userId);
        assertEquals(200, transferResponse.statusCode());

        //verify
        final BigDecimal account1ExpectedBalance = BigDecimal.valueOf(9.31);
        assertEquals(account1ExpectedBalance, getAccountBalance(userId, userAccount1));

        final BigDecimal account2ExpectedBalance = BigDecimal.valueOf(111.54);
        assertEquals(account2ExpectedBalance, getAccountBalance(userId, userAccount2));

        final Long transferId = transferResponse.bodyAsJsonObject().getLong("transferId");
        verifyTransfer(transferId, userId, amountToTransfer, account1InitialBalance,
                account1ExpectedBalance, account2InitialBalance, account2ExpectedBalance);
    }

    @Test
    public void transfer_betweenDifferentUsersAccounts_returns200() {
        //create two users
        final long user1 = createUser("Mike");
        final long user2 = createUser("John");

        //create account per each user
        final BigDecimal user1AccountInitialBalance = BigDecimal.ZERO;
        final long user1AccountId = createAccount(user1, user1AccountInitialBalance);

        final BigDecimal user2AccountInitialBalance = BigDecimal.valueOf(100);
        final long user2AccountId = createAccount(user2, user2AccountInitialBalance);

        //transfer between accounts
        final BigDecimal amountToTransfer = BigDecimal.valueOf(55.73);
        final String query = transferQuery(user2AccountId, user1AccountId, amountToTransfer);

        final HttpResponse<Buffer> transferResponse = sendSync(HttpMethod.PATCH, query, user2);
        assertEquals(200, transferResponse.statusCode());

        //verify
        final BigDecimal user1AccountExpectedBalance = BigDecimal.valueOf(55.73);
        assertEquals(user1AccountExpectedBalance, getAccountBalance(user1, user1AccountId));
        final BigDecimal user2AccountExpectedBalance = BigDecimal.valueOf(44.27);
        assertEquals(user2AccountExpectedBalance, getAccountBalance(user2, user2AccountId));

        final Long transferId = transferResponse.bodyAsJsonObject().getLong("transferId");
        verifyTransfer(transferId, user2, amountToTransfer, user2AccountInitialBalance,
                user2AccountExpectedBalance, user1AccountInitialBalance, user1AccountExpectedBalance);
    }

    @Test
    public void transfer_negativeAmount_returns400() {
        final long user1 = createUser("Mike");
        final long user2 = createUser("John");

        final long user1AccountId = createAccount(user1);
        final long user2AccountId = createAccount(user2);

        final String query = transferQuery(user2AccountId, user1AccountId, BigDecimal.valueOf(-100));

        final HttpResponse<Buffer> transferResponse = sendSync(HttpMethod.PATCH, query, user2);
        assertEquals(400, transferResponse.statusCode());
    }

    @Test
    public void transfer_zeroAmount_returns400() {
        final long user1 = createUser("Mike");
        final long user2 = createUser("John");

        final long user1AccountId = createAccount(user1);
        final long user2AccountId = createAccount(user2);

        final String query = transferQuery(user2AccountId, user1AccountId, BigDecimal.ZERO);

        final HttpResponse<Buffer> transferResponse = sendSync(HttpMethod.PATCH, query, user2);
        assertEquals(400, transferResponse.statusCode());
    }

    @Test
    public void transfer_insufficientFunds_returns409() {
        final long user1 = createUser("Mike");
        final long user2 = createUser("John");

        final long user1Account = createAccount(user1, new BigDecimal(99));
        final long user2Account = createAccount(user2);

        final String query = transferQuery(user1Account, user2Account, new BigDecimal(100));

        final HttpResponse<Buffer> transferResponse = sendSync(HttpMethod.PATCH, query, user1);
        assertEquals(409, transferResponse.statusCode());
    }

    @Test
    public void transfer_betweenOneAccount_returns400() {
        final long user = createUser("Mike");
        final long userAccountId = createAccount(user);

        final String query = transferQuery(userAccountId, userAccountId, BigDecimal.valueOf(100));

        final HttpResponse<Buffer> transferResponse = sendSync(HttpMethod.PATCH, query, user);
        assertEquals(400, transferResponse.statusCode());
    }

    @Test
    public void transfer_userNotAccountOwner_returns404() {
        final long user1 = createUser("Mike");
        final long user2 = createUser("John");

        final long user1Account = createAccount(user1);
        final long user2Account = createAccount(user2);

        final String query = transferQuery(user2Account, user1Account, new BigDecimal(100));

        final HttpResponse<Buffer> transferResponse = sendSync(HttpMethod.PATCH, query, user1);
        assertEquals(404, transferResponse.statusCode());
    }

    @Test
    public void transfer_unknownSrcAccount_returns404() {
        final long user1 = createUser("Mike");
        final long user2 = createUser("John");

        final long user2Account = createAccount(user2);

        final String query = transferQuery(99999, user2Account, new BigDecimal(100));

        final HttpResponse<Buffer> transferResponse = sendSync(HttpMethod.PATCH, query, user1);
        assertEquals(404, transferResponse.statusCode());
    }

    @Test
    public void transfer_unknownDstAccount_returns404() {
        final long user = createUser("Mike");
        final long userAccount = createAccount(user);

        final String query = transferQuery(userAccount, 99999, new BigDecimal(100));

        final HttpResponse<Buffer> transferResponse = sendSync(HttpMethod.PATCH, query, user);
        assertEquals(404, transferResponse.statusCode());
    }

    @Test
    public void transfer_wrongAmount_returns400() {
        final String query = transferQuery(1, 2, "abc");

        final HttpResponse<Buffer> transferResponse = sendSync(HttpMethod.PATCH, query, 1L);
        assertEquals(400, transferResponse.statusCode());
    }

    @Test
    public void getMetrics_return200() {
        final HttpResponse<Buffer> response = sendSync(HttpMethod.GET, "/metrics");
        assertEquals(200, response.statusCode());

        final JsonObject json = response.bodyAsJsonObject();
        assertTrue(json.containsKey("db.queue.size"));
    }

    @Test
    public void getUnknownAccount_return404() {
        final long userId = createUser("Mike");

        final HttpResponse<Buffer> response = sendSync(HttpMethod.GET, "/account/99999", userId);
        assertEquals(404, response.statusCode());
    }

    @Test
    public void unknownEndpoint_return404() {
        final HttpResponse<Buffer> response = sendSync(HttpMethod.GET, "/zzz");
        assertEquals(404, response.statusCode());
    }

}
