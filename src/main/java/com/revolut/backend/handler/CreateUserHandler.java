package com.revolut.backend.handler;

import com.revolut.backend.constants.JsonFields;
import com.revolut.backend.constants.QueryParams;
import com.revolut.backend.db.CreateUserCallback;
import com.revolut.backend.db.Database;
import com.revolut.backend.utils.Reply;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterType;
import io.vertx.ext.web.api.validation.ValidationHandler;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

/**
 * Creates user with name specified with {@link com.revolut.backend.constants.QueryParams#USER_NAME} query parameter
 */
public class CreateUserHandler extends HandlerBase implements ValidatorHolder {

    private final Database database;

    public CreateUserHandler(Database database) {
        Objects.requireNonNull(database, "Database must not be null");
        this.database = database;
    }

    @Override
    public ValidationHandler getValidator() {
        return HTTPRequestValidationHandler.create()
                .addQueryParam(QueryParams.USER_NAME, ParameterType.GENERIC_STRING, true);
    }

    @Override
    public void handle(RoutingContext ctx) {
        final String userName = requestParameters(ctx).queryParameter(QueryParams.USER_NAME).getString();

        requestLogger(ctx).info("Create user {}", userName);

        try {
            database.createUser(userName, new CreateUserCallback() {
                @Override
                public void onSuccess(long userId) {
                    requestLogger(ctx).info("User '{}' created successfully with id '{}'", userName, userId);
                    Reply.json(ctx, new JsonObject(Collections.singletonMap(JsonFields.USER_ID, userId)));
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    requestLogger(ctx).error("Unexpected error on create user '{}' attempt: {}",
                            userName, e.getMessage());
                    requestLogger(ctx).debug("Stacktrace", userName, e);

                    Reply.unexpectedError(ctx, e);
                }
            });
        } catch (RejectedExecutionException e) {
            Reply.tryLater(ctx);
        }
    }
}
