package com.revolut.backend.handler;

import com.revolut.backend.constants.HttpHeaders;
import com.revolut.backend.constants.JsonFields;
import com.revolut.backend.constants.PathParams;
import com.revolut.backend.constants.QueryParams;
import com.revolut.backend.db.Database;
import com.revolut.backend.db.TransferCallback;
import com.revolut.backend.utils.Reply;
import com.revolut.backend.utils.Utils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterType;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationHandler;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

/**
 * Transfer money from one account to another. Use path variable {@value PathParams#ACCOUNT_ID} to specify source account
 * and query parameter {@value QueryParams#DST_ACC_ID} for destination. Amount of means to be transferred should be specified
 * with {@value QueryParams#BALANCE} query parameter. Requester id should be specified with {@value HttpHeaders#USER_ID}.
 * User have to be source account owner. {@value JsonFields#TRANSFER_ID} field will be returned in the response json.
 */
public class TransferHandler extends HandlerBase implements ValidatorHolder {

    private static final String TRANSFER_ID_FIELD = "transferId";

    private final Database database;

    public TransferHandler(Database database) {
        Objects.requireNonNull(database, "Database must not be null");
        this.database = database;
    }

    @Override
    public ValidationHandler getValidator() {
        return HTTPRequestValidationHandler.create()
                .addHeaderParamWithCustomTypeValidator(HttpHeaders.USER_ID,
                        ParameterTypeValidator.createLongTypeValidator(null), true, false)
                .addPathParamWithCustomTypeValidator(PathParams.ACCOUNT_ID,
                        ParameterTypeValidator.createLongTypeValidator(null), false)
                .addQueryParamWithCustomTypeValidator(QueryParams.DST_ACC_ID,
                        ParameterTypeValidator.createLongTypeValidator(null), true, false)
                .addQueryParam(QueryParams.AMOUNT, ParameterType.GENERIC_STRING, true);
    }

    @Override
    public void handle(RoutingContext ctx) {
        final RequestParameters requestParams = requestParameters(ctx);

        final Long userId = requestParams.headerParameter(HttpHeaders.USER_ID).getLong();
        final Long srcAccountId = requestParams.pathParameter(PathParams.ACCOUNT_ID).getLong();
        final Long dstAccountId = requestParams.queryParameter(QueryParams.DST_ACC_ID).getLong();
        final String amount = requestParams.queryParameter(QueryParams.AMOUNT).getString();
        final BigDecimal amountDecimal = Utils.parseDecimal(amount);
        final String requestId = requestId(ctx);

        if (srcAccountId.equals(dstAccountId)) {
            Reply.badRequest(ctx, "Source and destination accounts should be different");
            return;
        }

        if (!(amountDecimal.compareTo(BigDecimal.ZERO) > 0)) {
            Reply.badRequest(ctx, "Amount should be greater zero");
            return;
        }

        requestLogger(ctx).info("Transfer '{}' from account '{}' to account '{}' by the user '{}' request",
                amountDecimal, srcAccountId, dstAccountId, userId);

        try {
            database.transfer(requestId, userId, srcAccountId, dstAccountId, amountDecimal, new TransferCallback() {
                @Override
                public void onSuccess(long transferId) {
                    requestLogger(ctx).info("Transfer '{}' complete successfully", transferId);
                    Reply.json(ctx, new JsonObject(Collections.singletonMap(TRANSFER_ID_FIELD, transferId)));
                }

                @Override
                public void onAccountNotFound(long accountId) {
                    requestLogger(ctx).info("Account '{}' not found", accountId);
                    Reply.resourceNotFound(ctx, "account", String.valueOf(accountId));
                }

                @Override
                public void onNotOwner() {
                    requestLogger(ctx).warn("User '{}' is not the account '{}' owner", userId, srcAccountId);

                    //do not expose account existence and answer with 404
                    Reply.resourceNotFound(ctx, "account");
                }

                @Override
                public void onInsufficientFunds() {
                    requestLogger(ctx).info("There is not enough funds for the transfer");
                    Reply.insufficientFunds(ctx);
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    requestLogger(ctx).error("Unexpected error on transfer attempt: {}", e.getMessage());
                    requestLogger(ctx).debug("Stacktrace", e);
                    Reply.unexpectedError(ctx, e);
                }
            });
        } catch (RejectedExecutionException e) {
            Reply.tryLater(ctx);
        }

    }
}
