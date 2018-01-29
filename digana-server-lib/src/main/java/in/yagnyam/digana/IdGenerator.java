package in.yagnyam.digana;

import lombok.NoArgsConstructor;

import static com.googlecode.objectify.ObjectifyService.factory;

@NoArgsConstructor(staticName = "instance")
public class IdGenerator {

  public <T> long getNextId(Class<T> type) {
    return factory().allocateId(type).getId();
  }
}
