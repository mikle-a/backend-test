package com.revolut.backend.unit.test;

import com.revolut.backend.constants.HttpHeaders;
import com.revolut.backend.constants.PathParams;
import com.revolut.backend.constants.QueryParams;
import com.revolut.backend.db.*;
import com.revolut.backend.handler.*;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.RequestParameters;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.RejectedExecutionException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class UnexpectedErrorsTest {

    private static Database databaseMock;

    private RoutingContext ctxMock;
    private HttpServerRequest httpRequestMock;
    private HttpServerResponse httpResponseMock;
    private RequestParameters requestParametersMock;
    private MultiMap headersMock;

    @BeforeClass
    public static void init(){
        databaseMock = mock(Database.class);
    }

    @Before
    public void beforeEachTest(){
        ctxMock = mock(RoutingContext.class);

        httpRequestMock = mock(HttpServerRequest.class);
        when(ctxMock.request()).thenReturn(httpRequestMock);

        httpResponseMock = mock(HttpServerResponse.class);
        when(ctxMock.response()).thenReturn(httpResponseMock);

        requestParametersMock = mock(RequestParameters.class);
        when(ctxMock.get("parsedParameters")).thenReturn(requestParametersMock);

        headersMock = mock(MultiMap.class);
        when(httpResponseMock.headers()).thenReturn(headersMock);
    }

    @Test
    public void testCreateUserHandler_onRejectedExecutionException(){
        doThrow(new RejectedExecutionException()).when(databaseMock).createUser(any(), any());
        when(requestParametersMock.queryParameter(QueryParams.USER_NAME))
                .thenReturn(RequestParameter.create("p"));

        new CreateUserHandler(databaseMock).handle(ctxMock);

        verify(httpResponseMock, times(1)).setStatusCode(429);
    }

    @Test
    public void testCreateUserHandler_onUnexpectedError(){
        doAnswer(a -> {
            final CreateUserCallback createUserCallback = (CreateUserCallback) a.getArguments()[1];
            createUserCallback.onUnexpectedError(new Exception());
            return null;
        }).when(databaseMock).createUser(any(), any());

        when(requestParametersMock.queryParameter(QueryParams.USER_NAME))
                .thenReturn(RequestParameter.create("p"));

        new CreateUserHandler(databaseMock).handle(ctxMock);

        verify(httpResponseMock, times(1)).setStatusCode(500);
    }

    @Test
    public void testCreateAccountHandler_onRejectedExecutionException(){
        doThrow(new RejectedExecutionException()).when(databaseMock).createAccount(anyLong(), any(), any());
        when(requestParametersMock.headerParameter(HttpHeaders.USER_ID))
                .thenReturn(RequestParameter.create(1L));

        new CreateAccountHandler(databaseMock).handle(ctxMock);

        verify(httpResponseMock, times(1)).setStatusCode(429);
    }

    @Test
    public void testCreateAccountHandler_onUnexpectedError(){
        doAnswer(a -> {
            final CreateAccountCallback createAccountCallback = (CreateAccountCallback) a.getArguments()[2];
            createAccountCallback.onUnexpectedError(new Exception());
            return null;
        }).when(databaseMock).createAccount(anyLong(), any(), any());

        when(requestParametersMock.headerParameter(HttpHeaders.USER_ID))
                .thenReturn(RequestParameter.create(1L));

        new CreateAccountHandler(databaseMock).handle(ctxMock);

        verify(httpResponseMock, times(1)).setStatusCode(500);
    }

    @Test
    public void testGetAccountHandler_onRejectedExecutionException(){
        doThrow(new RejectedExecutionException()).when(databaseMock).getAccount(anyLong(), anyLong(), any());
        when(requestParametersMock.headerParameter(HttpHeaders.USER_ID))
                .thenReturn(RequestParameter.create(1L));
        when(requestParametersMock.pathParameter(PathParams.ACCOUNT_ID))
                .thenReturn(RequestParameter.create(1L));

        new GetAccountHandler(databaseMock).handle(ctxMock);

        verify(httpResponseMock, times(1)).setStatusCode(429);
    }

    @Test
    public void testGetAccountHandler_onUnexpectedError(){
        doAnswer(a -> {
            final GetAccountCallback getAccountCallback = (GetAccountCallback) a.getArguments()[2];
            getAccountCallback.onUnexpectedError(new Exception());
            return null;
        }).when(databaseMock).getAccount(anyLong(), anyLong(), any());

        when(requestParametersMock.headerParameter(HttpHeaders.USER_ID))
                .thenReturn(RequestParameter.create(1L));
        when(requestParametersMock.pathParameter(PathParams.ACCOUNT_ID))
                .thenReturn(RequestParameter.create(1L));

        new GetAccountHandler(databaseMock).handle(ctxMock);

        verify(httpResponseMock, times(1)).setStatusCode(500);
    }

    @Test
    public void testTransferHandler_onRejectedExecutionException(){
        doThrow(new RejectedExecutionException()).when(databaseMock)
                .transfer(anyString(), anyLong(), anyLong(), anyLong(), any(), any());

        when(requestParametersMock.headerParameter(HttpHeaders.USER_ID))
                .thenReturn(RequestParameter.create(1L));

        when(requestParametersMock.pathParameter(PathParams.ACCOUNT_ID))
                .thenReturn(RequestParameter.create(1L));

        when(requestParametersMock.queryParameter(QueryParams.DST_ACC_ID))
                .thenReturn(RequestParameter.create(2L));

        when(requestParametersMock.queryParameter(QueryParams.AMOUNT))
                .thenReturn(RequestParameter.create("1"));

        new TransferHandler(databaseMock).handle(ctxMock);

        verify(httpResponseMock, times(1)).setStatusCode(429);
    }

    @Test
    public void testTransferHandler_onUnexpectedError(){
        doAnswer(a -> {
            final TransferCallback transferCallback = (TransferCallback) a.getArguments()[5];
            transferCallback.onUnexpectedError(new Exception());
            return null;
        }).when(databaseMock).transfer(anyString(), anyLong(), anyLong(), anyLong(), any(), any());

        when(requestParametersMock.headerParameter(HttpHeaders.USER_ID))
                .thenReturn(RequestParameter.create(1L));

        when(requestParametersMock.pathParameter(PathParams.ACCOUNT_ID))
                .thenReturn(RequestParameter.create(1L));

        when(requestParametersMock.queryParameter(QueryParams.DST_ACC_ID))
                .thenReturn(RequestParameter.create(2L));

        when(requestParametersMock.queryParameter(QueryParams.AMOUNT))
                .thenReturn(RequestParameter.create("1"));

        new TransferHandler(databaseMock).handle(ctxMock);

        verify(httpResponseMock, times(1)).setStatusCode(500);
    }

    @Test
    public void testGetTransferHandler_onRejectedExecutionException(){
        doThrow(new RejectedExecutionException()).when(databaseMock).getTransfer(anyLong(), anyLong(), any());

        when(requestParametersMock.headerParameter(HttpHeaders.USER_ID))
                .thenReturn(RequestParameter.create(1L));

        when(requestParametersMock.pathParameter(PathParams.TRANSFER_ID))
                .thenReturn(RequestParameter.create(1L));

        new GetTransferHandler(databaseMock).handle(ctxMock);

        verify(httpResponseMock, times(1)).setStatusCode(429);
    }

    @Test
    public void testGetTransferHandler_onUnexpectedError(){
        doAnswer(a -> {
            final GetTransferCallback getTransferCallback = (GetTransferCallback) a.getArguments()[2];
            getTransferCallback.onUnexpectedError(new Exception());
            return null;
        }).when(databaseMock).getTransfer(anyLong(), anyLong(), any());

        when(requestParametersMock.headerParameter(HttpHeaders.USER_ID))
                .thenReturn(RequestParameter.create(1L));

        when(requestParametersMock.pathParameter(PathParams.TRANSFER_ID))
                .thenReturn(RequestParameter.create(1L));

        new GetTransferHandler(databaseMock).handle(ctxMock);

        verify(httpResponseMock, times(1)).setStatusCode(500);
    }




}
