package com.bluegraybox.sample.connection_pool;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;

/**
 * Abstract parent for {@link Connection} wrapper classes;
 * invokes abstract validation before passing call through to wrapped Connection.
 * <p>Separated from {@link ConnectionWrapper} mostly for tidiness of code coverage tests:
 * These methods are boilerplate, so we don't expect them to be tested thoroughly; but ConnectionWrapper's should be.</p>
 */
public abstract class ConnectionPassThrough implements Connection {

	protected Connection conn;

	public ConnectionPassThrough(Connection conn) {
		this.conn = conn;
	}

	protected abstract void validateState();

	public void clearWarnings() throws SQLException {
		validateState();
		this.conn.clearWarnings();
	}

	public void close() throws SQLException {
		validateState();
		this.conn.close();
	}

	public void commit() throws SQLException {
		validateState();
		this.conn.commit();
	}

	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		validateState();
		return this.conn.createArrayOf(typeName, elements);
	}

	public Blob createBlob() throws SQLException {
		validateState();
		return this.conn.createBlob();
	}

	public Clob createClob() throws SQLException {
		validateState();
		return this.conn.createClob();
	}

	public NClob createNClob() throws SQLException {
		validateState();
		return this.conn.createNClob();
	}

	public SQLXML createSQLXML() throws SQLException {
		validateState();
		return this.conn.createSQLXML();
	}

	public Statement createStatement() throws SQLException {
		validateState();
		return this.conn.createStatement();
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency)
			throws SQLException {
				validateState();
				return this.conn.createStatement(resultSetType, resultSetConcurrency);
			}

	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
				validateState();
				return this.conn.createStatement(resultSetType,
						resultSetConcurrency, resultSetHoldability);
			}

	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		validateState();
		return this.conn.createStruct(typeName, attributes);
	}

	public boolean getAutoCommit() throws SQLException {
		validateState();
		return this.conn.getAutoCommit();
	}

	public String getCatalog() throws SQLException {
		validateState();
		return this.conn.getCatalog();
	}

	public Properties getClientInfo() throws SQLException {
		validateState();
		return this.conn.getClientInfo();
	}

	public String getClientInfo(String name) throws SQLException {
		validateState();
		return this.conn.getClientInfo(name);
	}

	public int getHoldability() throws SQLException {
		validateState();
		return this.conn.getHoldability();
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		validateState();
		return this.conn.getMetaData();
	}

	public int getTransactionIsolation() throws SQLException {
		validateState();
		return this.conn.getTransactionIsolation();
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		validateState();
		return this.conn.getTypeMap();
	}

	public SQLWarning getWarnings() throws SQLException {
		validateState();
		return this.conn.getWarnings();
	}

	public boolean isClosed() throws SQLException {
		validateState();
		return this.conn.isClosed();
	}

	public boolean isReadOnly() throws SQLException {
		validateState();
		return this.conn.isReadOnly();
	}

	public boolean isValid(int timeout) throws SQLException {
		validateState();
		return this.conn.isValid(timeout);
	}

	public String nativeSQL(String sql) throws SQLException {
		validateState();
		return this.conn.nativeSQL(sql);
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		validateState();
		return this.conn.prepareCall(sql);
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
				validateState();
				return this.conn.prepareCall(sql, resultSetType, resultSetConcurrency);
			}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
				validateState();
				return this.conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
			}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		validateState();
		return this.conn.prepareStatement(sql);
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
			throws SQLException {
				validateState();
				return this.conn.prepareStatement(sql, autoGeneratedKeys);
			}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
			throws SQLException {
				validateState();
				return this.conn.prepareStatement(sql, columnIndexes);
			}

	public PreparedStatement prepareStatement(String sql, String[] columnNames)
			throws SQLException {
				validateState();
				return this.conn.prepareStatement(sql, columnNames);
			}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
				validateState();
				return this.conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
			}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
				validateState();
				return this.conn.prepareStatement(sql, resultSetType,
						resultSetConcurrency, resultSetHoldability);
			}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		validateState();
		this.conn.releaseSavepoint(savepoint);
	}

	public void rollback() throws SQLException {
		validateState();
		this.conn.rollback();
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		validateState();
		this.conn.rollback(savepoint);
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		validateState();
		this.conn.setAutoCommit(autoCommit);
	}

	public void setCatalog(String catalog) throws SQLException {
		validateState();
		this.conn.setCatalog(catalog);
	}

	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		validateState();
		this.conn.setClientInfo(properties);
	}

	public void setClientInfo(String name, String value)
			throws SQLClientInfoException {
				validateState();
				this.conn.setClientInfo(name, value);
			}

	public void setHoldability(int holdability) throws SQLException {
		validateState();
		this.conn.setHoldability(holdability);
	}

	public void setReadOnly(boolean readOnly) throws SQLException {
		validateState();
		this.conn.setReadOnly(readOnly);
	}

	public Savepoint setSavepoint() throws SQLException {
		validateState();
		return this.conn.setSavepoint();
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		validateState();
		return this.conn.setSavepoint(name);
	}

	public void setTransactionIsolation(int level) throws SQLException {
		validateState();
		this.conn.setTransactionIsolation(level);
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		validateState();
		this.conn.setTypeMap(map);
	}

	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		validateState();
		return this.conn.isWrapperFor(arg0);
	}

	public <T> T unwrap(Class<T> arg0) throws SQLException {
		validateState();
		return this.conn.unwrap(arg0);
	}

}
