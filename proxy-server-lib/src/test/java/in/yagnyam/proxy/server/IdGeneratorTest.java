package in.yagnyam.proxy.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.googlecode.objectify.ObjectifyService;
import in.yagnyam.proxy.server.db.RepositoryTestHelper;
import in.yagnyam.proxy.server.model.RequestEntity;
import java.util.HashSet;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IdGeneratorTest extends RepositoryTestHelper {

  @BeforeClass
  public static void registerBank() {
    ObjectifyService.register(RequestEntity.class);
  }

  @Test
  public void testGetNextId() {
    Set<Long> allocated = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      long id = IdGenerator.instance().getNextId(RequestEntity.class);
      assertFalse(allocated.contains(id));
      allocated.add(id);
    }
  }


  @Test(expected = IllegalArgumentException.class)
  public void testGetNextIds_InvalidInput() {
    IdGenerator.instance().getNextIds(RequestEntity.class, 0);
  }

  @Test
  public void testGetNextIds() {
    long id = IdGenerator.instance().getNextIds(RequestEntity.class, 10);
    long next = IdGenerator.instance().getNextId(RequestEntity.class);
    assertTrue(next >= id + 10);
  }

  @Test
  public void testInstance() {
    assertNotNull(IdGenerator.instance());
  }
}