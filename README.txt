This is a simple connection pooling library, developed just as a code
sample.

This project is managed by Maven. The only third-party libraries required
to build it are junit 4.5 and sqlite-jdbc 3.7.2 (from xerial.org). Maven
will handle these dependencies automatically. "mvn package" will build
a jar file. The more useful Maven goals are:

    mvn test
        Runs the unit tests
    mvn site
        Generates documentation for this project, available at
        target/site/index.html
    mvn cobertura:cobertura
        Runs unit tests and generates a coverage report, available at
        target/site/cobertura/index.html
    mvn javadoc:javadoc
        Generates javadoc for all classes, including private; available
        at target/site/apidocs/index.html
    mvn javadoc:test-javadoc
        Generates javadoc for test classes; available at
        target/site/testapidocs/index.html

Again, this is a fairly simple implementation. A quick review of existing
open-source connection pooling libraries will suggest all sorts of
interesting and useful features which are beyond the scope of this
effort. Beyond the minimum for managing the pool's size, the ones that
I considered essential were: A connection wrapper, to prevent released
connections from being re-used without being checked out again; and an
idle timeout, which reclaims unused connections.

All classes are in the com.bluegraybox.sample.connection_pool
package. Following the Maven convention, the library code is under
src/main/java, and the test code is under src/test/java. The classes are:

    ConnectionPool
        The interface specified in the assignment.
    ExampleConnectionPool
        The pooling implentation, including the Janitor thread class to
        manage stale connections.
    ConnectionWrapper
        The wrapper which enforces the connection's lifecycle and tracks
        the idle timeout.
    ExampleConnectionPoolTest
        Unit tests for the core connection pool functionality.
    ConnectionWrapperTest
        Unit tests for the ConnectionWrapper.
    ConnectionTimeoutTest
        Unit tests for idle timeout functionality.

Please see the javadocs or source code for more information. Enjoy!

