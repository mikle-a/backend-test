package com.revolut.backend.unit.test;

import com.revolut.backend.db.CreateAccountCallback;
import com.revolut.backend.db.CreateUserCallback;
import com.revolut.backend.db.GetAccountCallback;
import com.revolut.backend.db.TransferCallback;
import com.revolut.backend.db.impl.H2Database;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class H2DatabaseTest {

    private static H2Database database;
    private static DataSource dataSourceMock;
    private static Connection connectionMock;
    private static Exception exception;

    @BeforeClass
    public static void init() throws SQLException {
        dataSourceMock = mock(DataSource.class);
        connectionMock = mock(Connection.class);
        exception = new Exception();

        when(dataSourceMock.getConnection()).thenReturn(connectionMock);
        when(connectionMock.prepareStatement(anyString())).thenAnswer(a -> {
            throw exception;
        });

        database = new H2Database(9123, Executors.newSingleThreadExecutor(), dataSourceMock);
    }

    @Test(expected = RuntimeException.class)
    public void testSqlExceptionOnInitialization_throwsRuntimeException() throws SQLException {
        when(connectionMock.createStatement()).thenAnswer(a -> {
            throw new SQLException();
        });

        database.init();
    }

    @Test
    public void testExceptionOnCreateUser_callbackCalled() {
        final CreateUserCallback createUserCallback = mock(CreateUserCallback.class);
        database.createUser("Petr", createUserCallback);

        verify(createUserCallback, timeout(1000).times(1)).onUnexpectedError(exception);
    }

    @Test
    public void testExceptionOnCreateAccount_callbackCalled() {
        final CreateAccountCallback createAccountCallback = mock(CreateAccountCallback.class);
        database.createAccount(1L, BigDecimal.ZERO, createAccountCallback);

        verify(createAccountCallback, timeout(1000).times(1)).onUnexpectedError(exception);
    }

    @Test
    public void testExceptionOnGetAccount_callbackCalled() {
        final GetAccountCallback getAccountCallback = mock(GetAccountCallback.class);
        database.getAccount(1L, 1L, getAccountCallback);

        verify(getAccountCallback, timeout(1000).times(1)).onUnexpectedError(exception);
    }

    @Test
    public void testExceptionOnTransfer_callbackCalled() {
        final TransferCallback transferCallback = mock(TransferCallback.class);
        database.transfer("requestId", 1L,1L, 1L, BigDecimal.ONE, transferCallback);

        verify(transferCallback, timeout(1000).times(1)).onUnexpectedError(exception);
    }

}
