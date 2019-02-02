package com.revolut.backend.handler;

import com.revolut.backend.constants.HttpHeaders;
import com.revolut.backend.constants.JsonFields;
import com.revolut.backend.constants.QueryParams;
import com.revolut.backend.db.CreateAccountCallback;
import com.revolut.backend.db.Database;
import com.revolut.backend.utils.Reply;
import com.revolut.backend.utils.Utils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameter;
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
 * Creates account for the user. Initial balance could be optionally specified
 * with {@link com.revolut.backend.constants.QueryParams#BALANCE} query parameter
 */
public class CreateAccountHandler extends HandlerBase implements ValidatorHolder {

    private final Database database;

    public CreateAccountHandler(Database database) {
        Objects.requireNonNull(database, "Database must not be null");
        this.database = database;
    }

    @Override
    public ValidationHandler getValidator() {
        return HTTPRequestValidationHandler.create()
                .addQueryParam(QueryParams.BALANCE, ParameterType.GENERIC_STRING, false)
                .addHeaderParamWithCustomTypeValidator(HttpHeaders.USER_ID,
                        ParameterTypeValidator.createLongTypeValidator(null), true, false);
    }

    @Override
    public void handle(RoutingContext ctx) {
        final RequestParameters requestParams = requestParameters(ctx);
        final Long userId = requestParams.headerParameter(HttpHeaders.USER_ID).getLong();
        final RequestParameter balance = requestParams.queryParameter(QueryParams.BALANCE);
        final BigDecimal balanceDecimal = balance == null ? BigDecimal.ZERO : Utils.parseDecimal(balance.getString());

        requestLogger(ctx).info("Create account for the user '{}'", userId);

        try {
            database.createAccount(userId, balanceDecimal, new CreateAccountCallback() {
                @Override
                public void onSuccess(long accountId) {
                    requestLogger(ctx).info("Account '{}' successfully created for the user '{}'", accountId, userId);
                    Reply.json(ctx, new JsonObject(Collections.singletonMap(JsonFields.ACCOUNT_ID, accountId)));
                }

                @Override
                public void onUserNotFound() {
                    requestLogger(ctx).warn("User '{}' not found", userId);
                    Reply.resourceNotFound(ctx, "user");
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    requestLogger(ctx).error("Unexpected error occurred on account create attempt" +
                            " for the user '{}' : '{}'", userId, e.getMessage());
                    requestLogger(ctx).debug("Stacktrace", e);
                    Reply.unexpectedError(ctx, e);
                }
            });
        } catch (RejectedExecutionException e) {
            Reply.tryLater(ctx);
        }

    }
}
