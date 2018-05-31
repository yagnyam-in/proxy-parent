package in.yagnyam.proxy.server.db;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cache.AsyncCacheFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.Closeable;
import java.io.IOException;

public class RepositoryTestHelper {

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    private Closeable session;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUpBeforeClass() {
        ObjectifyService.setFactory(new ObjectifyFactory());
    }

    @Before
    public void setUp() {
        this.helper.setUp();
        this.session = ObjectifyService.begin();
    }

    @After
    public void tearDown() throws IOException {
        AsyncCacheFilter.complete();
        this.session.close();
        this.helper.tearDown();
    }

}
