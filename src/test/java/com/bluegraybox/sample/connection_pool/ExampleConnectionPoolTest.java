package com.bluegraybox.sample.connection_pool;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests ExampleConnectionPool: input validation, connection lifecycle, and pool resizing.
 * Uses an in-memory sqlite database to test our connection pool.
 */
public class ExampleConnectionPoolTest {

	private static final String DB_URL = "jdbc:sqlite::memory:";  // In-memory sqlite database

	/**
	 * Sets up an in-memory database for the tests.
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DriverManager.registerDriver(new org.sqlite.JDBC());
	}

	/**
	 * Test that we can create a connection pool; and get, use and release a connection.
	 * Test each of the constructors.
	 */
	@Test
	public final void testGetConnection() throws SQLException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 1, 1, 0L);
		Connection conn = pool.getConnection();
		assertNotNull(conn.getMetaData());
		pool.releaseConnection(conn);

		pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 1, 1);
		conn = pool.getConnection();
		assertNotNull(conn.getMetaData());
		pool.releaseConnection(conn);

		pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 0L);
		conn = pool.getConnection();
		assertNotNull(conn.getMetaData());
		pool.releaseConnection(conn);

		pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd");
		conn = pool.getConnection();
		assertNotNull(conn.getMetaData());
		pool.releaseConnection(conn);
	}

	/**
	 * Test that the constructor throws an exception if min size is less than one.
	 */
	@Test(expected=IllegalArgumentException.class)
	public final void testBadMinParam() throws SQLException {
		new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 0, 1, 1);
	}

	/**
	 * Test that the constructor throws an exception if max size is less than the min size.
	 */
	@Test(expected=IllegalArgumentException.class)
	public final void testBadMaxParam() throws SQLException {
		new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 2, 1, 1);
	}

	/**
	 * Test that the constructor throws an exception if the increment is less than zero.
	 */
	@Test(expected=IllegalArgumentException.class)
	public final void testBadIncrParam() throws SQLException {
		new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 2, -1);
	}

	/**
	 * Test that the constructor throws an exception if the timeout is less than zero.
	 */
	@Test(expected=IllegalArgumentException.class)
	public final void testBadTimeoutParam() throws SQLException {
		new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 2, 1, -1L);
	}

	/**
	 * Test that the pool won't create more connections than allowed.
	 */
	@Test
	public final void testTooManyConnections() throws SQLException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 1, 1);
		pool.getConnection();
		// Our second request should return null.
		assertNull(pool.getConnection());
	}

	/**
	 * Test that the connection is unavailable after release.
	 */
	@Test(expected=IllegalStateException.class)
	public final void testReleaseConnection() throws SQLException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 1, 1);
		Connection conn = pool.getConnection();
		pool.releaseConnection(conn);
		conn.getMetaData();
	}

	/**
	 * Test that trying to release an unwrapped connection causes an error.
	 */
	@Test(expected=IllegalArgumentException.class)
	public final void testReleaseUnwrappedConnection() throws SQLException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 1, 1);
		Connection sqlConn = DriverManager.getConnection(DB_URL, "dummyUser", "dummyPwd");
		pool.releaseConnection(sqlConn);
	}

	/**
	 * Test that trying to release an unpooled connection causes an error.
	 */
	@Test(expected=IllegalStateException.class)
	public final void testReleaseUnpooledConnection() throws SQLException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 1, 1);
		Connection sqlConn = DriverManager.getConnection(DB_URL, "dummyUser", "dummyPwd");
		Connection conn = new ConnectionWrapper(sqlConn);
		pool.releaseConnection(conn);
	}

	/**
	 * Test that the pool grows in expected increments and obeys limits.
	 */
	@Test
	public final void testPoolGrowth() throws SQLException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 1, 6, 3);
		
		pool.getConnection();
		assertEquals(1, pool.countInUse());
		assertEquals(0, pool.countAvailable());
		assertEquals(1, pool.size());
		
		// Pool should expand by 3, from 1 to 4.
		pool.getConnection();
		assertEquals(2, pool.countInUse());
		assertEquals(2, pool.countAvailable());
		assertEquals(4, pool.size());

		pool.getConnection();
		assertEquals(3, pool.countInUse());
		assertEquals(1, pool.countAvailable());
		assertEquals(4, pool.size());
		
		pool.getConnection();
		assertEquals(4, pool.countInUse());
		assertEquals(0, pool.countAvailable());
		assertEquals(4, pool.size());
		
		// Pool would expand by 3, but it stops at max size.
		pool.getConnection();
		assertEquals(5, pool.countInUse());
		assertEquals(1, pool.countAvailable());
		assertEquals(6, pool.size());
	}

	/**
	 * Test that the pool re-uses released connections correctly.
	 */
	@Test
	public final void testConnectionReuse() throws SQLException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 4, 9, 3);

		Connection conn1 = pool.getConnection();
		Connection conn2 = pool.getConnection();
		Connection conn3 = pool.getConnection();
		Connection conn4 = pool.getConnection();
		assertEquals(4, pool.countInUse());
		assertEquals(0, pool.countAvailable());
		assertEquals(4, pool.size());

		pool.releaseConnection(conn1);
		pool.releaseConnection(conn2);
		pool.releaseConnection(conn3);
		pool.releaseConnection(conn4);
		assertEquals(0, pool.countInUse());
		assertEquals(4, pool.countAvailable());
		assertEquals(4, pool.size());
	}

	/**
	 * Test that the pool closes surplus connections correctly.
	 */
	@Test
	public final void testSurplusConnections() throws SQLException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 4, 9, 3);
		
		Connection conn1 = pool.getConnection();
		Connection conn2 = pool.getConnection();
		Connection conn3 = pool.getConnection();
		Connection conn4 = pool.getConnection();
		Connection conn5 = pool.getConnection();
		// Pool has grown to 7.
		pool.releaseConnection(conn5);
		// Pool should not close connection because we only have 3 (the increment) free.
		assertEquals(4, pool.countInUse());
		assertEquals(3, pool.countAvailable());
		assertEquals(7, pool.size());
		
		conn5 = pool.getConnection();
		Connection conn6 = pool.getConnection();
		Connection conn7 = pool.getConnection();
		Connection conn8 = pool.getConnection();
		// Pool has grown to 9.
		
		pool.releaseConnection(conn5);
		assertEquals(7, pool.countInUse());
		assertEquals(2, pool.countAvailable());
		assertEquals(9, pool.size());
		
		pool.releaseConnection(conn6);
		assertEquals(6, pool.countInUse());
		assertEquals(3, pool.countAvailable());
		assertEquals(9, pool.size());
		
		pool.releaseConnection(conn7);
		// This would leave 4 connections free, so the pool closes one.
		assertEquals(5, pool.countInUse());
		assertEquals(3, pool.countAvailable());
		assertEquals(8, pool.size());
		
		pool.releaseConnection(conn8);
		// The pool closes one more
		assertEquals(4, pool.countInUse());
		assertEquals(3, pool.countAvailable());
		assertEquals(7, pool.size());

		pool.releaseConnection(conn4);
		// And closes one more
		assertEquals(3, pool.countInUse());
		assertEquals(3, pool.countAvailable());
		assertEquals(6, pool.size());

		pool.releaseConnection(conn3);
		// And closes one more
		assertEquals(2, pool.countInUse());
		assertEquals(3, pool.countAvailable());
		assertEquals(5, pool.size());

		pool.releaseConnection(conn2);
		// And closes one more
		assertEquals(1, pool.countInUse());
		assertEquals(3, pool.countAvailable());
		assertEquals(4, pool.size());

		pool.releaseConnection(conn1);
		// But doesn't close any more, now that pool is at minimum size.
		assertEquals(0, pool.countInUse());
		assertEquals(4, pool.countAvailable());
		assertEquals(4, pool.size());
	}

	/**
	 * Test pool's handling of stale connections.
	 */
	@Test
	public final void testStaleConnection() throws SQLException, InterruptedException {
		/**
		 * Extends ConnectionWrapper to allow us to force a SQLException.
		 */
		class TestConnectionWrapper extends ConnectionWrapper {
			public TestConnectionWrapper(Connection conn) {
				super(conn);
			}
			
			public DatabaseMetaData getMetaData() throws SQLException {
				throw new SQLException("Faked exception for testing");
			}
			
			public void close() throws SQLException {
				throw new SQLException("Faked exception for testing");
			}
		}

		/**
		 * Extends ExampleConnectionPool to use TestConnectionWrappers.
		 */
		class TestConnectionPool extends ExampleConnectionPool {
			
			public TestConnectionPool(String url, String user, String password, long timeout) throws SQLException {
				super(url, user, password, timeout);
			}

			protected ConnectionWrapper getWrappedConnection() throws SQLException {
				Connection conn = getSQLConnection();
				ConnectionWrapper wrapper = new TestConnectionWrapper(conn);
				return wrapper;
			}
		}

		ExampleConnectionPool pool = new TestConnectionPool(DB_URL, "dummyUser", "dummyPwd", 100L);
		pool.getConnection();
		Thread.sleep(200);  // Exercise the pool Janitor
	}

	/**
	 * Test that we can't get any more connections from the pool after it has been cleaned up.
	 * Tests the case of a pool with no timeout (and thus no Janitor thread).
	 */
	@Test
	public final void testCleanup() throws SQLException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 4, 9, 3);
		
		pool.cleanup();
		assertEquals(0, pool.countInUse());
		assertEquals(0, pool.countAvailable());
		assertEquals(0, pool.size());
		assertNull(pool.getConnection());
	}

	/**
	 * Test that we can't get any more connections from the pool after it has been cleaned up.
	 * Tests the case of a pool with a timeout (and thus a Janitor thread).
	 */
	@Test
	public final void testCleanupJanitor() throws SQLException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 4, 9, 3, 1000L);
		
		pool.cleanup();
		assertEquals(0, pool.countInUse());
		assertEquals(0, pool.countAvailable());
		assertEquals(0, pool.size());
		assertNull(pool.getConnection());
	}

	/**
	 * Test that releasing a connection after the pool has been cleaned up causes an IllegalStateException.
	 */
	@Test(expected=IllegalStateException.class)
	public final void testPostCleanupRelease() throws SQLException {
		ExampleConnectionPool pool = new ExampleConnectionPool(DB_URL, "dummyUser", "dummyPwd", 4, 9, 3, 1000L);
		Connection conn = pool.getConnection();
		pool.cleanup();
		pool.releaseConnection(conn);
	}
}
