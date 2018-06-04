package in.yagnyam.proxy.server.model;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RequestEntityTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testConstruction_MissingId() {
        expectedException.expect(NullPointerException.class);
        RequestEntity.builder().requestType("RT").build();
    }


    @Test
    public void testConstruction_MissingType() {
        expectedException.expect(NullPointerException.class);
        RequestEntity.builder().requestId("RID").build();
    }


    @Test
    public void testConstruction_Valid() {
        RequestEntity.builder().requestId("RID").requestType("RT").build();
    }
}
