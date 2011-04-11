package com.bluegraybox.sample.connection_pool;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps a Connection to keep track of state information needed by an {@link ExampleConnectionPool}.
 * <p>The lifecycle for a ConnectionWrapper is: Available -> checkOut() -> In Use -> checkIn() -> Used.
 * The {@link Connection} methods can only be invoked while it is In Use.
 * Each wrapper can only be used once. To get a new wrapper for the same SQL connection, use {@link #reWrap()}.</p>
 * <p>The boilerplate methods to wrap the Connection are defined in {@link ConnectionPassThrough}, just for tidiness.</p>
 */
class ConnectionWrapper extends ConnectionPassThrough {

	private static enum State { AVAILABLE, IN_USE, USED }

	private State state = State.AVAILABLE;
	private long lastUsed = 0;

	/**
	 * @param conn An actual SQL database connection.
	 * @see ExampleConnectionPool
	 */
	public ConnectionWrapper(Connection conn) {
		super(conn);
	}

	/**
	 * Returns the last time this connection was used, or when it was checked out.
	 * @return System time in milliseconds.
	 * @throws IllegalStateException if the connection is not in use.
	 */
	public long getLastUsed() {
		if (! state.equals(State.IN_USE))
			throw new IllegalStateException("Connection not in use");
		return lastUsed;
	}

	/**
	 * A connection is available if it has not been checked out yet.
	 */
	public boolean available() {
		return state.equals(State.AVAILABLE);
	}

	/**
	 * A connection is in use if it has been checked out, but not checked in yet.
	 */
	public boolean inUse() {
		return state.equals(State.IN_USE);
	}
	
	/**
	 * Mark the connection as in use.
	 * Note that each connection can only be checked out once.
	 * @return false if the connection is already in use.
	 * @throws IllegalStateException if the connection has already been checked in.
	 */
	public synchronized boolean checkOut() {
		if (state.equals(State.IN_USE))
			return false;
		if (state.equals(State.USED))
			throw new IllegalStateException("Connection has already been checked in, and cannot be re-used.");
		state = State.IN_USE;
		lastUsed = System.currentTimeMillis();
		return true;
	}

	/**
	 * Mark the connection as no longer in use.
	 * @throws IllegalStateException if the connection was not in use.
	 */
	public void checkIn() {
		if (state.equals(State.USED))
			throw new IllegalStateException("Connection already checked in");
		if (! state.equals(State.IN_USE))
			throw new IllegalStateException("Connection not checked out");
		state = State.USED;
	}

	/**
	 * Create a new ConnectionWrapper for our SQL connection.
	 * @throws IllegalStateException if the connection has not been checked in.
	 */
	public ConnectionWrapper reWrap() {
		if (! state.equals(State.USED))
			throw new IllegalStateException("Connection not checked in");
		return new ConnectionWrapper(this.conn);
	}

	/**
	 * Checks that the connection is in use, and updates its idle time.
	 * Protected, not private, so we can extend this class for testing.
	 * @throws IllegalStateException if the connection has not been checked out, or has already been checked in.
	 */
	protected void validateState() {
		if (! state.equals(State.IN_USE))
			throw new IllegalStateException("Connection not in use");
		lastUsed = System.currentTimeMillis();
	}

	/**
	 * Because the {@link ConnectionPool} is managing the opening and closing of connections,
	 * the normal Connection.close() does nothing.
	 * The connection pool uses the {@link #forceClose()} method instead.
	 */
	public void close() throws SQLException {
		/* FIXME: Arguably, this should throw a runtime exception, since it should never be called.
		 * If you're converting code that used to create and close its own connections, you should
		 * replace all the close() calls with releaseConnection(). But you don't want ugly surprises
		 * (runtime production failures) if you miss anything.
		 */
	}

	/**
	 * Actually close our database connection (unlike {@link #close()}).
	 * This is used internally by the {@link ConnectionPool} implementation, and is not part of the public interface.
	 * Unlike other Connection methods, this does not validate the state of the connection.
	 */
	protected void forceClose() throws SQLException {
		this.conn.close();
	}

}
