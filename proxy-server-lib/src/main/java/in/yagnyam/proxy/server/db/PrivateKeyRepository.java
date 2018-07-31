package in.yagnyam.proxy.server.db;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.proxy.server.model.PrivateKeyEntity;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;

/**
 * PrivateKeyEntity Repository for storing and retrieving from Database
 */
@Builder
public class PrivateKeyRepository {

  static {
    ObjectifyService.register(PrivateKeyEntity.class);
  }

  /**
   * Fetch the PrivateKeyEntity from Database
   *
   * @return All Private Keys
   */
  public List<PrivateKeyEntity> getPrivateKeyEntities() {
    return ObjectifyService.run(() -> ofy().load().type(PrivateKeyEntity.class).list());
  }

  /**
   * Fetch all Private Keys from Database
   *
   * @param id PrivateKeyEntity Id
   * @return PrivateKeyEntity with given Id
   */
  public Optional<PrivateKeyEntity> getPrivateKeyEntity(@NonNull String id) {
    return ObjectifyService.run(
        () -> Optional.ofNullable(ofy().load().key(Key.create(PrivateKeyEntity.class, id)).now()));
  }


  /**
   * Save the PrivateKeyEntity to Database
   *
   * @param privateKeyEntity PrivateKeyEntity
   */
  public void savePrivateKeyEntity(@NonNull PrivateKeyEntity privateKeyEntity) {
    ObjectifyService.run(new VoidWork() {
      @Override
      public void vrun() {
        ofy().save().entity(privateKeyEntity).now();
      }
    });
  }


}
