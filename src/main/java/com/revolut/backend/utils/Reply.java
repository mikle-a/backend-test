package com.revolut.backend.utils;

import com.revolut.backend.constants.JsonFields;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Objects;

public class Reply {

    private Reply() {
    }

    public static void badRequest(RoutingContext ctx, String msg) {
        json(ctx, HttpResponseStatus.BAD_REQUEST, errorJson(msg));
    }

    public static void resourceNotFound(RoutingContext ctx, String resourceType) {
        json(ctx, HttpResponseStatus.NOT_FOUND, errorJson(resourceType + " not found"));
        ctx.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code());
    }

    public static void resourceNotFound(RoutingContext ctx, String resourceType, String resourceId) {
        json(ctx, HttpResponseStatus.NOT_FOUND,
                errorJson(String.format("%s with id '%s' not found", resourceType, resourceId)));
    }

    public static void insufficientFunds(RoutingContext ctx) {
        json(ctx, HttpResponseStatus.CONFLICT, errorJson("Insufficient funds"));
    }

    public static void endpointNotFound(RoutingContext ctx) {
        json(ctx, HttpResponseStatus.NOT_FOUND, errorJson("Endpoint not found"));
    }

    public static void unexpectedError(RoutingContext ctx, Throwable e) {
        json(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorJson(String.valueOf(e)));
    }

    public static void tryLater(RoutingContext ctx) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        ctx.response().setStatusCode(HttpResponseStatus.TOO_MANY_REQUESTS.code());
        ctx.response().headers().set(HttpHeaderNames.RETRY_AFTER, "30");
    }

    public static void json(RoutingContext ctx, JsonObject json) {
        json(ctx, HttpResponseStatus.OK, json);
    }

    private static void json(RoutingContext ctx, HttpResponseStatus code, JsonObject json) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(json, "json must not be null");

        ctx.response().setStatusCode(code.code());
        ctx.response().headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        ctx.response().end(String.valueOf(json));
    }

    private static JsonObject errorJson(String msg) {
        Objects.requireNonNull(msg, "msg must not be null");
        return new JsonObject().put(JsonFields.ERROR, msg);
    }


}
