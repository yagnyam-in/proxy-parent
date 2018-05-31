package in.yagnyam.proxy.server;

import lombok.NoArgsConstructor;

import static com.googlecode.objectify.ObjectifyService.factory;

@NoArgsConstructor(staticName = "instance")
public class IdGenerator {

    public <T> long getNextId(Class<T> type) {
        return factory().allocateId(type).getId();
    }

    public <T> long getNextIds(Class<T> type, long count) {
        if (count <=0 ) {
            throw new IllegalArgumentException("invalid count");
        }
        return factory().allocateIds(type, count).iterator().next().getId();
    }

}
