package com.bluegraybox.sample.connection_pool;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the ConnectionWrapper's management of the check-out/check-in lifecycle.
 * Uses an in-memory sqlite database for database connections.
 * @see ConnectionWrapper
 */
public class ConnectionWrapperTest {

	private static final String DB_URL = "jdbc:sqlite::memory:";  // In-memory sqlite database
	private ConnectionWrapper conn;

	/**
	 * Sets up an in-memory database for the tests.
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DriverManager.registerDriver(new org.sqlite.JDBC());
	}

	/**
	 * Creates a new connection wrapper for the tests to use.
	 */
	@Before
	public void setUp() throws Exception {
		Connection sqlConn = DriverManager.getConnection(DB_URL, "dummyUser", "dummyPwd");
		conn = new ConnectionWrapper(sqlConn);
	}

	/**
	 * Test that connection is available before checkout, but not after.
	 */
	@Test
	public final void testCheckOut() throws SQLException {
		assertTrue(conn.available());
		assertTrue(conn.checkOut());
		assertFalse(conn.available());
	}
	
	/**
	 * Test that repeated checkouts fail.
	 */
	@Test
	public final void testRepeatedCheckOut() throws SQLException {
		assertTrue(conn.checkOut());
		assertFalse(conn.checkOut());
	}

	/**
	 * Test that connection remains unavailable when checked in.
	 */
	@Test
	public final void testCheckIn() throws SQLException {
		assertTrue(conn.checkOut());
		conn.checkIn();
		assertFalse(conn.available());
	}
	
	/**
	 * Test that repeated checkins cause an error.
	 */
	@Test(expected=IllegalStateException.class)
	public final void testRepeatedCheckIn() throws SQLException {
		assertTrue(conn.checkOut());
		conn.checkIn();
		conn.checkIn();
	}

	/**
	 * Test that checkout after checkin causes an error.
	 */
	@Test(expected=IllegalStateException.class)
	public final void testCheckOutAfterCheckIn() throws SQLException {
		assertTrue(conn.checkOut());
		conn.checkIn();
		conn.checkOut();
	}

	/**
	 * Test that checkin before checkout causes an error.
	 */
	@Test(expected=IllegalStateException.class)
	public final void testCheckInBeforeCheckOut() throws SQLException {
		conn.checkIn();
	}

	/**
	 * Test that the re-wrapped connection is not the same as the old one
	 */
	@Test
	public final void testRewrap() {
		conn.checkOut();
		conn.checkIn();
		ConnectionWrapper conn2 = conn.reWrap();
		assertNotSame(conn, conn2);
		assertTrue(conn2.checkOut());
	}

	/**
	 * Test that the re-wrapped connection is not the same as the old one
	 */
	@Test(expected=IllegalStateException.class)
	public final void testIllegalRewrap() {
		conn.reWrap();
	}

	/**
	 * Test that the connection is unavailable after release.
	 */
	@Test(expected=IllegalStateException.class)
	public final void testReleaseConnection() throws SQLException {
		conn.getMetaData();
	}
	
	/**
	 * Test that closing the connection does nothing.
	 * @see ConnectionWrapper#close() for explanation.
	 */
	public final void testCloseConnection() throws SQLException {
		assertTrue(conn.checkOut());
		conn.close();
		// With a normal Connection, this would fail, but it shouldn't now.
		conn.getMetaData();
	}

	/**
	 * Test that the wrapped methods report an error if the connection hasn't been checked out, or has already been checked in;
	 * and work correctly when checked out.
	 * Note: Currently, this only tests clearWarnings().
	 * You can add the other 48 methods in the Connection interface if you think it's really important.
	 * It's a lot of work, and these methods are almost too simple to fail.
	 * They're all boilerplate; if one works, they all should.
	 */
	@Test
	public final void testWrappedMethods() throws SQLException {
		try {
			conn.clearWarnings();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		conn.checkOut();
		conn.clearWarnings();
		conn.checkIn();
		try {
			conn.clearWarnings();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

}
