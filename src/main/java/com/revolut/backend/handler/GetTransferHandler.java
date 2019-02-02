package com.revolut.backend.handler;

import com.revolut.backend.constants.HttpHeaders;
import com.revolut.backend.constants.JsonFields;
import com.revolut.backend.constants.PathParams;
import com.revolut.backend.db.Database;
import com.revolut.backend.db.GetTransferCallback;
import com.revolut.backend.entity.Transfer;
import com.revolut.backend.utils.Reply;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationHandler;

import java.util.concurrent.RejectedExecutionException;

/**
 * Get existing transfer by id specified with {@value PathParams#TRANSFER_ID} path parameter.
 * User id should be specified with {@value HttpHeaders#USER_ID} and should be the transfer owner.
 */
public class GetTransferHandler extends HandlerBase implements ValidatorHolder {

    private final Database database;

    public GetTransferHandler(Database database) {
        this.database = database;
    }

    @Override
    public ValidationHandler getValidator() {
        return HTTPRequestValidationHandler.create()
                .addHeaderParamWithCustomTypeValidator(HttpHeaders.USER_ID,
                        ParameterTypeValidator.createLongTypeValidator(null), true, false)
                .addPathParamWithCustomTypeValidator(PathParams.TRANSFER_ID,
                        ParameterTypeValidator.createLongTypeValidator(null), false);
    }

    @Override
    public void handle(RoutingContext ctx) {
        final Long userId = requestParameters(ctx).headerParameter(HttpHeaders.USER_ID).getLong();
        final Long transferId = requestParameters(ctx).pathParameter(PathParams.TRANSFER_ID).getLong();

        try {
            database.getTransfer(userId, transferId, new GetTransferCallback() {
                @Override
                public void onSuccess(Transfer transfer) {
                    requestLogger(ctx).info("Transfer '{}' obtained successfully", transferId);

                    final JsonObject json = new JsonObject();
                    json.put(JsonFields.TRANSFER_ID, transfer.getId());
                    json.put(JsonFields.SRC_ACC_ID, transfer.getSrcAccountId());
                    json.put(JsonFields.DST_ACC_ID, transfer.getDstAccountId());
                    json.put(JsonFields.USER_ID, transfer.getUserId());
                    json.put(JsonFields.TIMESTAMP, transfer.getTimestamp());
                    json.put(JsonFields.AMOUNT, String.valueOf(transfer.getAmount()));
                    json.put(JsonFields.SRC_ACC_BEFORE, String.valueOf(transfer.getSrcAccountBalanceBefore()));
                    json.put(JsonFields.SRC_ACC_AFTER, String.valueOf(transfer.getSrcAccountBalanceAfter()));
                    json.put(JsonFields.DST_ACC_BEFORE, String.valueOf(transfer.getDstAccountBalanceBefore()));
                    json.put(JsonFields.DST_ACC_AFTER, String.valueOf(transfer.getDstAccountBalanceAfter()));

                    Reply.json(ctx, json);
                }

                @Override
                public void onTransferNotFound() {
                    requestLogger(ctx).warn("Transfer '{}' not found", transferId);
                    Reply.resourceNotFound(ctx, "transfer");
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    requestLogger(ctx).error("Unexpected error occurred on transfer read attempt: '{}'", e.getMessage());
                    requestLogger(ctx).debug("Stacktrace", e);
                    Reply.unexpectedError(ctx, e);
                }
            });
        } catch (RejectedExecutionException e) {
            Reply.tryLater(ctx);
        }
    }
}
