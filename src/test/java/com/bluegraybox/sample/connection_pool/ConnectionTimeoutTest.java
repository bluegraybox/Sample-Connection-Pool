package com.bluegraybox.sample.connection_pool;


import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the idle timeout functionality of ConnectionWrapper and ExampleConnectionPool.
 * Uses an in-memory sqlite database for database connections.
 * @see ConnectionWrapper
 * @see ExampleConnectionPool
 */
public class ConnectionTimeoutTest {

	private static final String DB_URL = "jdbc:sqlite::memory:";  // In-memory sqlite database

	/**
	 * Sets up an in-memory database for the tests.
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DriverManager.registerDriver(new org.sqlite.JDBC());
	}

	/**
	 * Test that we can't set an invalid timeout.
	 */
	@Test(expected=IllegalArgumentException.class)
	public final void testBadPoolTimeout() throws SQLException {
		new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 1, 1, -1L);
	}

	/**
	 * Test that the last-used time is tracked correctly.
	 */
	@Test
	public final void testLastUsed() throws SQLException, InterruptedException {
		Connection sqlConn = DriverManager.getConnection(DB_URL, "dummyUser", "dummyPwd");
		ConnectionWrapper conn = new ConnectionWrapper(sqlConn);
		assertTrue(conn.checkOut()); // sets last-used time

		// Test that last-used isn't updated if we're not using the connection.
		long previous = conn.getLastUsed();
		Thread.sleep(50);
		long latest = conn.getLastUsed();
		assertEquals(previous, latest);
		
		// Test that it is when we use the connection.
		conn.getMetaData();
		latest = conn.getLastUsed();
		assertTrue(latest >= (previous + 50));
	}

	/**
	 * Test that getting the last-used time for an inactive (available) connection throws a state exception.  
	 */
	@Test(expected=IllegalStateException.class)
	public final void testAvailableLastUsed() throws SQLException {
		Connection sqlConn = DriverManager.getConnection(DB_URL, "dummyUser", "dummyPwd");
		ConnectionWrapper conn = new ConnectionWrapper(sqlConn);
		conn.getLastUsed();
	}

	/**
	 * Test that getting the last-used time for a closed (used) connection throws a state exception.  
	 */
	@Test(expected=IllegalStateException.class)
	public final void testUsedLastUsed() throws SQLException {
		Connection sqlConn = DriverManager.getConnection(DB_URL, "dummyUser", "dummyPwd");
		ConnectionWrapper conn = new ConnectionWrapper(sqlConn);
		assertTrue(conn.checkOut());
		conn.checkIn();
		conn.getLastUsed();
	}

	/**
	 * Test that the connection times out when idle limit is exceeded, and that it cannot be used afterwards.
	 */
	@Test
	public final void testTimeout() throws SQLException, InterruptedException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 1, 1, 100L);
		Connection conn = pool.getConnection();
		Thread.sleep(50);
		conn.getMetaData(); // make sure this doesn't time out
		try {
			Thread.sleep(200);
			conn.getMetaData();
			fail("Expected IllegalStateException due to timeout.");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	/**
	 * Test that the connection doesn't time out as long as it's kept busy.
	 */
	@Test
	public final void testIdleReset() throws SQLException, InterruptedException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 1, 1, 100L);
		Connection conn = pool.getConnection();
		
		// Use the connection at 50 ms intervals; it shouldn't time out.
		conn.getMetaData();
		Thread.sleep(50);
		conn.getMetaData();
		Thread.sleep(50);
		conn.getMetaData();
		Thread.sleep(50);
		conn.getMetaData();
		Thread.sleep(50);
		conn.getMetaData();
	}

	/**
	 * Test that the connection doesn't time out if timeout is zero.
	 */
	@Test
	public final void testNoTimeout() throws SQLException, InterruptedException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 1, 1, 0L);
		Connection conn = pool.getConnection();
		
		// In theory, we should wait an infinite amount of time for this,
		// or at least Long.MAX_VALUE ms, which amounts to the same thing.
		// In practice, if this is going to time out, it would be because zero is being used as the actual timeout.
		conn.getMetaData();
		Thread.sleep(100);
		conn.getMetaData();
	}

	/**
	 * Test that the idle timer doesn't start before checkout.
	 */
	@Test
	public final void testTimeoutStart() throws SQLException, InterruptedException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 1, 1, 50L);
		// Wait *before* checkout
		Thread.sleep(100);
		Connection conn = pool.getConnection();
		conn.getMetaData();
	}

	/**
	 * Test that the connection times out when idle limit is exceeded, and that it cannot be used afterwards.
	 */
	@Test
	public final void testPoolTimeout() throws SQLException, InterruptedException {
		// Create a connection pool with a 50 ms timeout,
		// then wait 100 ms before using it.
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 1, 1, 50L);
		Connection conn = pool.getConnection(); // starts the idle clock
		assertNotNull(conn.getMetaData()); // don't catch or expect an exception here
		Thread.sleep(100);
		try {
			conn.getMetaData();
			fail("Expected IllegalStateException due to timeout.");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

}
