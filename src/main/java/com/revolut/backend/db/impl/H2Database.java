package com.revolut.backend.db.impl;

import com.revolut.backend.db.*;
import com.revolut.backend.entity.Account;
import com.revolut.backend.entity.Transfer;
import com.revolut.backend.utils.Args;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Database implementation based on in memory H2
 */
public class H2Database implements Database {

    private enum ErrorCode {
        PARENT_MISSING("23506");

        private final String sqlState;

        ErrorCode(String sqlState) {
            this.sqlState = sqlState;
        }

        public boolean is(SQLException e) {
            return this.sqlState.equals(e.getSQLState());
        }
    }

    private static Logger logger = LoggerFactory.getLogger(H2Database.class);

    private final Server server;
    private final DataSource dataSource;
    private final ExecutorService executorService;

    /**
     * Construct new instance
     *
     * @param executorService will be used to process all blocking jdbc calls. Configure the queue properly to
     *                        avoid the application to be over overwhelmed
     * @throws RuntimeException on h2 server startup error
     */
    public H2Database(int port, ExecutorService executorService, DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        Objects.requireNonNull(executorService, "executorService must not be null");
        Args.isTrue(port > 0, "port must  greater 0");
        try {
            this.server = Server.createTcpServer("-tcpPort", String.valueOf(port), "-tcpAllowOthers");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        this.dataSource = dataSource;
        this.executorService = executorService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        try {
            server.start();

            executeStatement("DROP ALL OBJECTS;");

            executeStatement("CREATE SCHEMA IF NOT EXISTS revolut;");
            executeStatement("SET SCHEMA REVOLUT;");

            executeStatement("CREATE TEMP TABLE users (" +
                    "id IDENTITY PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL)" +
                    ";");

            executeStatement("CREATE TEMP TABLE accounts (" +
                    "id IDENTITY PRIMARY KEY, " +
                    "owner_id BIGINT NOT NULL, " +
                    "balance DECIMAL NOT NULL, " +
                    "FOREIGN KEY (owner_id) REFERENCES users(id)" +
                    ");");

            executeStatement("CREATE TEMP TABLE transfers (" +
                    "id IDENTITY PRIMARY KEY, " +
                    "ts TIMESTAMP WITH TIME ZONE, " +
                    "src_acc_id BIGINT NOT NULL, " +
                    "dst_acc_id BIGINT NOT NULL, " +
                    "amount DECIMAL NOT NULL, " +
                    "user_id BIGINT NOT NULL, " +
                    "request_id VARCHAR(255) NOT NULL, " +
                    "src_acc_before DECIMAL NOT NULL, " +
                    "src_acc_after DECIMAL NOT NULL, " +
                    "dst_acc_before DECIMAL NOT NULL, " +
                    "dst_acc_after DECIMAL NOT NULL, " +
                    "FOREIGN KEY (src_acc_id) REFERENCES accounts(id), " +
                    "FOREIGN KEY (dst_acc_id) REFERENCES accounts(id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ");");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        server.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createUser(String userName, CreateUserCallback callback) {
        Objects.requireNonNull(userName, "userName must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        executorService.submit(() -> {
            try (Connection connection = getConnection();
                 final PreparedStatement ps = connection.prepareStatement("INSERT INTO users (name) VALUES (?)")) {

                ps.setString(1, userName);
                ps.execute();

                final Long userId = getLastId(connection);

                if (userId != null) {
                    connection.commit();
                    callback.onSuccess(userId);
                } else {
                    connection.rollback();
                    callback.onUnexpectedError(new SQLException("Couldn't obtain user id"));
                }

            } catch (Exception e) {
                callback.onUnexpectedError(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createAccount(long userId, BigDecimal balance, CreateAccountCallback callback) {
        Args.isTrue(userId > 0, "userId must be greater zero");
        Objects.requireNonNull(balance, "balance must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        executorService.submit(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "INSERT INTO accounts (owner_id, balance) VALUES (?, ?)")) {

                ps.setLong(1, userId);
                ps.setBigDecimal(2, balance);

                try {
                    ps.execute();
                } catch (SQLException e) {
                    if (ErrorCode.PARENT_MISSING.is(e)) {
                        callback.onUserNotFound();
                        return;
                    }
                    throw e;
                }

                final Long accountId = getLastId(connection);

                if (accountId != null) {
                    connection.commit();
                    callback.onSuccess(accountId);
                } else {
                    connection.rollback();
                    callback.onUnexpectedError(new SQLException("Couldn't obtain account id"));
                }

            } catch (Exception e) {
                callback.onUnexpectedError(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getAccount(long userId, long accountId, GetAccountCallback callback) {
        Args.isTrue(userId > 0, "userId must be greater zero");
        Args.isTrue(accountId > 0, "accountId must be greater zero");

        executorService.submit(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT id, owner_id, balance FROM accounts WHERE id = ? AND owner_id = ?")) {
                ps.setLong(1, accountId);
                ps.setLong(2, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        final long accId = rs.getLong(1);
                        final long ownerId = rs.getLong(2);
                        final BigDecimal balance = rs.getBigDecimal(3);

                        callback.onSuccess(new Account(accId, ownerId, balance));
                    } else {
                        callback.onAccountNotFound();
                    }
                }

            } catch (Exception e) {
                callback.onUnexpectedError(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transfer(String requestId, long userId, long srcAccountId, long dstAccountId, BigDecimal amount, TransferCallback callback) {
        Objects.requireNonNull(requestId, "request id must not be null");
        Args.isTrue(userId > 0, "userId must be greater zero");
        Args.isTrue(srcAccountId > 0, "accountSrc must be greater zero");
        Args.isTrue(dstAccountId > 0, "accountDst must be greater zero");
        Objects.requireNonNull(amount, "amount must not be null");
        Args.isTrue(amount.compareTo(BigDecimal.ZERO) > 0, "amount must be positive");
        Objects.requireNonNull(callback, "callback must not be null");

        executorService.submit(() -> {
            try (Connection connection = getConnection()) {

                Account srcAccount;
                Account dstAccount;

                //lock accounts in the same order
                if (srcAccountId > dstAccountId) {
                    dstAccount = lockAccount(connection, dstAccountId);
                    srcAccount = lockAccount(connection, srcAccountId);
                } else {
                    srcAccount = lockAccount(connection, srcAccountId);
                    dstAccount = lockAccount(connection, dstAccountId);
                }

                //check src account exists
                if (srcAccount == null) {
                    connection.rollback();
                    callback.onAccountNotFound(srcAccountId);
                    return;
                }

                //check if user is src account owner
                if (userId != srcAccount.getOwnerId()) {
                    connection.rollback();
                    callback.onNotOwner();
                    return;
                }

                //check dst account exists
                if (dstAccount == null) {
                    connection.rollback();
                    callback.onAccountNotFound(dstAccountId);
                    return;
                }

                //check if balance is sufficient
                if (srcAccount.getBalance().compareTo(amount) >= 0) {

                    //update accounts
                    final BigDecimal srcAccountNewBalance = srcAccount.getBalance().subtract(amount);
                    setBalance(connection, srcAccountId, srcAccountNewBalance);

                    final BigDecimal dstAccountNewBalance = dstAccount.getBalance().add(amount);
                    setBalance(connection, dstAccountId, dstAccountNewBalance);

                    //write transfer
                    try (PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO transfers (ts, src_acc_id, dst_acc_id, amount, user_id, request_id, " +
                                    "src_acc_before, src_acc_after, dst_acc_before, dst_acc_after) " +
                                    "VALUES (CURRENT_TIMESTAMP(),?,?,?,?,?,?,?,?,?)")) {
                        ps.setLong(1, srcAccountId);
                        ps.setLong(2, dstAccountId);
                        ps.setBigDecimal(3, amount);
                        ps.setLong(4, userId);
                        ps.setString(5, requestId);
                        ps.setBigDecimal(6, srcAccount.getBalance());
                        ps.setBigDecimal(7, srcAccountNewBalance);
                        ps.setBigDecimal(8, dstAccount.getBalance());
                        ps.setBigDecimal(9, dstAccountNewBalance);

                        ps.execute();
                    }

                    final Long transferId = getLastId(connection);

                    if (transferId != null) {
                        connection.commit();
                        callback.onSuccess(transferId);
                    } else {
                        connection.rollback();
                        callback.onUnexpectedError(new SQLException("Couldn't obtain transfer id"));
                    }

                } else {
                    connection.rollback();
                    callback.onInsufficientFunds();
                }

            } catch (Exception e) {
                callback.onUnexpectedError(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getTransfer(long userId, long transferId, GetTransferCallback callback) {
        Args.isTrue(transferId > 0, "transferId must be greater zero");
        Objects.requireNonNull(callback, "request id must not be null");

        executorService.submit(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT id,ts,src_acc_id,dst_acc_id,amount,user_id,request_id,src_acc_before," +
                                 "src_acc_after,dst_acc_before,dst_acc_after " +
                                 "FROM transfers WHERE id = ? AND user_id = ?")) {

                ps.setLong(1, transferId);
                ps.setLong(2, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        final long id = rs.getLong(1);
                        final long ts = rs.getTimestamp(2).getTime();
                        final long srcAccId = rs.getLong(3);
                        final long dstAccId = rs.getLong(4);
                        final BigDecimal amount = rs.getBigDecimal(5);
                        final long transferUserId = rs.getLong(6);
                        final String requestId = rs.getString(7);
                        final BigDecimal srcAccBefore = rs.getBigDecimal(8);
                        final BigDecimal srcAccAfter = rs.getBigDecimal(9);
                        final BigDecimal dstAccBefore = rs.getBigDecimal(10);
                        final BigDecimal dstAccAfter = rs.getBigDecimal(11);

                        callback.onSuccess(new Transfer(id, requestId, ts, srcAccId,
                                dstAccId, transferUserId, amount, srcAccBefore, srcAccAfter, dstAccBefore, dstAccAfter));
                    } else {
                        callback.onTransferNotFound();
                    }
                }

            } catch (Exception e) {
                callback.onUnexpectedError(e);
            }
        });
    }

    private Account lockAccount(Connection connection, long accountId) throws SQLException {
        logger.debug("Acquire lock for the account '{}'", accountId);

        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM accounts WHERE id = ? FOR UPDATE ")) {
            ps.setLong(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final long id = rs.getLong(1);
                    final long ownerId = rs.getLong(2);
                    final BigDecimal balance = rs.getBigDecimal(3);

                    return new Account(id, ownerId, balance);
                } else {
                    return null;
                }
            }
        }
    }

    private void setBalance(Connection connection, long accountId, BigDecimal balance) throws SQLException {
        logger.debug("Set account '{}' balance = '{}'", accountId, balance);

        try (PreparedStatement ps = connection.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?")) {
            ps.setBigDecimal(1, balance);
            ps.setLong(2, accountId);

            ps.execute();
        }
    }

    private void executeStatement(String sql) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.createStatement().execute(sql);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        }
    }

    private Connection getConnection() throws SQLException {
        final Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        connection.setSchema("REVOLUT");
        return connection;
    }

    /**
     * Since H2 does not support "RETURNING id" and "getGeneratedKeys()", there is no other way
     * to obtain auto-generated id, but to call special function SCOPE_IDENTITY
     *
     * @param connection
     * @return last id if present, could be null
     * @throws SQLException
     */
    private static Long getLastId(Connection connection) throws SQLException {
        try (ResultSet rs = connection.prepareStatement("CALL SCOPE_IDENTITY()").executeQuery();) {
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                return null;
            }
        }
    }

}

