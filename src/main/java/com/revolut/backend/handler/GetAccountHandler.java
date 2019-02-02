package com.revolut.backend.handler;

import com.revolut.backend.constants.HttpHeaders;
import com.revolut.backend.constants.JsonFields;
import com.revolut.backend.constants.PathParams;
import com.revolut.backend.db.Database;
import com.revolut.backend.db.GetAccountCallback;
import com.revolut.backend.entity.Account;
import com.revolut.backend.utils.Reply;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationHandler;

import java.util.concurrent.RejectedExecutionException;

/**
 * Get user account by id specified with {@value PathParams#ACCOUNT_ID}.
 * User id should be specified with {@value HttpHeaders#USER_ID} and should be the account owner.
 */
public class GetAccountHandler extends HandlerBase implements ValidatorHolder {

    private final Database database;

    public GetAccountHandler(Database database) {
        this.database = database;
    }

    @Override
    public ValidationHandler getValidator() {
        return HTTPRequestValidationHandler.create()
                .addHeaderParamWithCustomTypeValidator(HttpHeaders.USER_ID,
                        ParameterTypeValidator.createLongTypeValidator(null), true, false)
                .addPathParamWithCustomTypeValidator(PathParams.ACCOUNT_ID,
                        ParameterTypeValidator.createLongTypeValidator(null), false);
    }

    @Override
    public void handle(RoutingContext ctx) {
        final RequestParameters requestParams = requestParameters(ctx);
        final Long userId = requestParams.headerParameter(HttpHeaders.USER_ID).getLong();
        final Long accountId = requestParams.pathParameter(PathParams.ACCOUNT_ID).getLong();

        requestLogger(ctx).info("Get user '{}' account with id '{}'", userId, accountId);

        try {
            database.getAccount(userId, accountId, new GetAccountCallback() {
                @Override
                public void onSuccess(Account account) {
                    requestLogger(ctx).info("Account '{}' obtained successfully", accountId);

                    final JsonObject json = new JsonObject();
                    json.put(JsonFields.ACCOUNT_ID, account.getId());
                    json.put(JsonFields.BALANCE, account.getBalance().toString());

                    Reply.json(ctx, json);
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    requestLogger(ctx).error("Unexpected error occurred on account read attempt" +
                            " for the user '{}' : '{}'", userId, e.getMessage());
                    requestLogger(ctx).debug("Stacktrace", e);
                    Reply.unexpectedError(ctx, e);
                }

                @Override
                public void onAccountNotFound() {
                    requestLogger(ctx).warn("Account '{}' not found", accountId);
                    Reply.resourceNotFound(ctx, "account");
                }
            });
        } catch (RejectedExecutionException e) {
            Reply.tryLater(ctx);
        }
    }
}
