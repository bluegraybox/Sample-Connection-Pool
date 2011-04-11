package com.bluegraybox.sample.connection_pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Simple connection pooling implementation.
 * Has basic tuning parameters&mdash;min and max size, growth increment&mdash;plus
 * optional timeout for idle connections (see notes under {@link #cleanup()}).
 * Uses the {@link ConnectionWrapper} to enforce the connection lifecycle and timeouts.
 * 
 * @author Colin MacDonald
 *
 */
public class ExampleConnectionPool implements ConnectionPool {

	private static final int MIN_SIZE = 10;
	private static final int MAX_SIZE = 200;
	private static final int GROWTH = 10;
	private static final long IDLE_TIMEOUT = 0; // 0 = no timeout
	
	private ArrayList<ConnectionWrapper> pool = new ArrayList<ConnectionWrapper>();
	private int minimumSize;
	private int maximumSize;
	private int growthIncrement;
	private String url, user, password;
	private long timeout = 0;  // No timeout
	private Janitor janitor;
	
	/**
	 * Constructor with all options specified.
	 * @param url Database connection URL
	 * @param user Database username
	 * @param password Database password for user
	 * @param minSize Minimum number of connections in pool. Defaults to 10.
	 * @param maxSize Maximum size of pool. Defaults to 200.
	 * @param incr Number of new connections created when all are in use. Defaults to 10.
	 * @param timeout Connection idle timeout. Defaults to 0 (no timeout).
	 */
	public ExampleConnectionPool(String url,
			String user,
			String password,
			int minSize,
			int maxSize,
			int incr,
			long timeout) throws SQLException {
		if (minSize < 1)
			throw new IllegalArgumentException("Pool size must be positive.");
		if (minSize > maxSize)
			throw new IllegalArgumentException("Minimum size for pool cannot be greater than maximum size.");
		if (incr < 0)
			throw new IllegalArgumentException("Growth increment for pool cannot be negative.");
		if (timeout < 0)
			throw new IllegalArgumentException("Timeout must be zero (no timeout) or positive.");
		this.url = url;
		this.user = user;
		this.password = password;
		this.minimumSize = minSize;
		this.maximumSize = maxSize;
		this.growthIncrement = incr;
		this.timeout = timeout;
		for (int i = 0; i < minimumSize; i++) {
			pool.add(getWrappedConnection());
		}
		if (timeout > 0) {
			long interval = timeout / 5;
			janitor = new Janitor(this, interval);
			janitor.start();
		}
	}

	/**
	 * Constructor using default value for idle timeout.
	 * @param url Database connection URL
	 * @param user Database username
	 * @param password Database password for user
	 * @param minSize Minimum number of connections in pool. Defaults to 10.
	 * @param maxSize Maximum size of pool. Defaults to 200.
	 * @param incr Number of new connections created when all are in use. Defaults to 10.
	 */
	public ExampleConnectionPool(String url, String user, String password, int minSize, int maxSize, int incr) throws SQLException {
		this(url, user, password, minSize, maxSize, incr, IDLE_TIMEOUT);
	}

	/**
	 * Constructor using default values for pool size and increment.
	 * @param url Database connection URL
	 * @param user Database username
	 * @param password Database password for user
	 * @param timeout Connection idle timeout. Defaults to 0 (no timeout).
	 */
	public ExampleConnectionPool(String url, String user, String password, long timeout) throws SQLException {
		this(url, user, password, MIN_SIZE, MAX_SIZE, GROWTH, timeout);
	}

	/**
	 * Constructor using default values for pool size and increment, and idle timeout.
	 * @param url Database connection URL
	 * @param user Database username
	 * @param password Database password for user
	 */
	public ExampleConnectionPool(String url, String user, String password) throws SQLException {
		this(url, user, password, MIN_SIZE, MAX_SIZE, GROWTH);
	}
	
	/**
	 * Get a connection from the pool.
	 * Grow pool if necessary, up to maximum.
	 * @return null if no connection available and pool at maximum size.
	 */
	public synchronized Connection getConnection() throws SQLException {
		ConnectionWrapper conn = null;
		for (ConnectionWrapper pooled: pool) {
			if (pooled.checkOut()) {
				// This connection may have been sitting in the pool for a while;
				// make sure the database hasn't timed it out.
				try {
					pooled.getMetaData();
					conn = pooled;
				}
				catch (SQLException ex) {
					// Connection is stale; get a new one.
					conn = getWrappedConnection();
					// Modifying the pool here is only ok because we break before the next iteration.
					pool.add(conn);
					pool.remove(pooled);
					conn.checkOut();
				}
				break;
			}
		}

		if (conn == null) {
			for (int i = 0; i < growthIncrement && pool.size() < maximumSize; i++) {
				conn = getWrappedConnection();
				pool.add(conn);
			}
			if (conn != null)
				conn.checkOut();
		}
		return conn;
	}

	/**
	 * Get a new SQL Connection, and wrap it.
	 * Protected (not private) so test doubles can override it.
	 */
	protected ConnectionWrapper getWrappedConnection() throws SQLException {
		Connection conn = getSQLConnection();
		ConnectionWrapper wrapper = new ConnectionWrapper(conn);
		return wrapper;
	}

	/**
	 * Get a new SQL Connection.
	 * Protected (not private) so test doubles that override getWrappedConnection can use it,
	 * and don't need access to url, user, and password.
	 */
	protected Connection getSQLConnection() throws SQLException {
		Connection conn = DriverManager.getConnection(url, user, password);
		return conn;
	}

	/**
	 * Release the connection back into the pool.
	 * If the number of available connections is greater than the growth increment, the connection will be closed.
	 * @throws IllegalStateException if the connection is not in the pool; usually because it has exceeded the idle timeout and been removed.
	 * @throws IllegalArgumentException if the connection is not actually a {@link ConnectionWrapper}.
	 */
	public synchronized void releaseConnection(Connection con) throws SQLException {
		if (!(con instanceof ConnectionWrapper))
			throw new IllegalArgumentException("Attempted to release connection that didn't come from this pool.");
		ConnectionWrapper wrapper = (ConnectionWrapper) con;

		if (! pool.contains(wrapper))
			throw new IllegalStateException("Connection not in pool. May have been removed already, possibly due to timeout.");
		wrapper.checkIn();

		// Each wrapper can only be used once, so we need to remove the old one from the pool.
		pool.remove(wrapper);
		if (countAvailable() >= growthIncrement && pool.size() >= minimumSize) {
			// We have plenty of free connections, so close this one.
			// This may throw a SQLException, so do it last!
			wrapper.forceClose();
		}
		else {
			// create a new wrapper for the connection, and add it to the pool
			ConnectionWrapper newWrapper = wrapper.reWrap();
			pool.add(newWrapper);
		}
	}

	/**
	 * Close any connections in use that are older than our timeout.
	 * This doesn't check for stale database connections.
	 */
	synchronized void pruneConnections() {
		long now = System.currentTimeMillis();
		ArrayList<ConnectionWrapper> pruned = new ArrayList<ConnectionWrapper>();
		for (ConnectionWrapper pooled: pool) {
			if (pooled.inUse()) {
				long lastUsed = pooled.getLastUsed();
				long idle = now - lastUsed;
				if (idle > timeout)
					pruned.add(pooled);
			}
		}
		for (ConnectionWrapper dead : pruned) {
			pool.remove(dead);
			dead.checkIn();
			try {
				dead.forceClose();
			} catch (SQLException e) {
				// Closing a timed-out database connection may cause an exception. Don't sweat it.
			}
		}
	}
	
	/**
	 * Clean up the resources used by the pool.
	 * The pool has a thread to check for timed out connections (the inner {@link Janitor Janitor} class).
	 * If not cleaned up, this thread will keep a reference to the pool, preventing it from being garbage collected.
	 */
	public void cleanup() {
		if (janitor != null)
			janitor.cleanup();
		pool.clear();
		// prevent any more connections from being created.
		maximumSize = 0;
	}

	/**
	 * Only used for testing - package scoped.
	 * @return The number of available connections in our pool.
	 */
	int countAvailable() {
		int free = 0;
		for (ConnectionWrapper pooled: pool) {
			if (pooled.available())
				free++;
		}
		return free;
	}

	/**
	 * Only used for testing - package scoped.
	 * @return The number of available connections in our pool.
	 */
	int countInUse() {
		int inUse = 0;
		for (ConnectionWrapper pooled: pool) {
			if (! pooled.available())
				inUse++;
		}
		return inUse;
	}
	
	/**
	 * Only used for testing - package scoped.
	 * @return The size of our connection pool.
	 */
	int size() {
		return pool.size();
	}


	/**
	 * Thread which periodically clears out timed-out connections from the pool.
	 */
	private static class Janitor extends Thread {
		
		private ExampleConnectionPool connPool;
		private long interval;
		
		/**
		 * @param pool The pool we're managing timeouts for.
		 * @param interval The time in milliseconds between checks for timed-out connections.
		 */
		public Janitor(ExampleConnectionPool pool, long interval) {
			this.connPool = pool;
			this.interval = interval;
		}
		
		/**
		 * Allow this thread to exit.
		 * As long as it is running, it keeps a reference to the pool,
		 * keeping it from being garbage collected.
		 */
		public void cleanup() {
			// It isn't necessary to drop references, but it's a convenient way of telling the thread to exit.
			connPool = null;
			this.interrupt();
		}

		public void run() {
			while (true) {
				try {
					sleep(interval);
				}
				catch (InterruptedException ex) {
					// do nothing
				}
				if (connPool == null)
					break;
				connPool.pruneConnections();
			}
		}
	}

}
