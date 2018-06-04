package in.yagnyam.proxy.server.db;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.proxy.server.ServiceException;
import in.yagnyam.proxy.server.model.RequestEntity;
import lombok.Builder;
import lombok.NonNull;

import java.util.Optional;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Request Repository for storing and retrieving from Database
 */
@Builder
public class RequestRepository {

    static {
        ObjectifyService.register(RequestEntity.class);
    }

    /**
     * Fetch request associated with Id
     *
     * @param requestId Request Id
     * @return Request with given Id
     */
    public Optional<RequestEntity> getRequest(@NonNull String requestId) {
        return ObjectifyService.run(() -> Optional.ofNullable(ofy().load().key(Key.create(RequestEntity.class, requestId)).now()));
    }


    /**
     * Save the RequestEntity to Database
     *
     * @param request RequestEntity
     */
    public void saveRequest(@NonNull RequestEntity request) {
        ObjectifyService.run(new VoidWork() {
            @Override
            public void vrun() {
                ofy().transact(() -> {
                    RequestEntity existing = ofy().load().key(Key.create(RequestEntity.class, request.getRequestId())).now();
                    if (existing == null) {
                        ofy().save().entity(request).now();
                    } else {
                        throw ServiceException.internalServerError("Duplicate Record Found");
                    }
                });
            }
        });
    }


}
