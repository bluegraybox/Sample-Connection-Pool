package com.bluegraybox.sample.connection_pool;

import java.sql.Connection;
import java.sql.SQLException;


/**
 * Connection pooling interface.
 */
public interface ConnectionPool {
	public Connection getConnection() throws SQLException;
	public void releaseConnection(Connection con) throws SQLException;
}
