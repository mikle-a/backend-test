package com.revolut.backend.handler;

import io.vertx.ext.web.api.validation.ValidationHandler;

/**
 * Interface to indicate that class contains validator inside. It is an effort to keep
 * validating and handling code together.
 */
public interface ValidatorHolder {
    ValidationHandler getValidator();
}
