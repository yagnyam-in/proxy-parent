package in.yagnyam.proxy.utils;

/**
 * Versions of the standard functional interfaces which throw Throwable.
 * <p>
 * ErrorHandler can convert these into standard functional interfaces.
 */
public interface Throwing {

  /**
   * Versions of the standard functional interfaces which throw a specific exception type.
   */
  interface Specific {

    @FunctionalInterface
    interface Runnable<E extends Throwable> {

      void run() throws E;
    }

    @FunctionalInterface
    interface Supplier<T, E extends Throwable> {

      T get() throws E;
    }

    @FunctionalInterface
    interface Consumer<T, E extends Throwable> {

      void accept(T t) throws E;
    }

    @FunctionalInterface
    interface Function<T, R, E extends Throwable> {

      R apply(T t) throws E;
    }

    @FunctionalInterface
    interface Predicate<T, E extends Throwable> {

      boolean test(T t) throws E;
    }
  }

  @FunctionalInterface
  interface Runnable extends Specific.Runnable<Throwable> {

  }

  @FunctionalInterface
  interface Supplier<T> extends Specific.Supplier<T, Throwable> {

  }

  @FunctionalInterface
  interface Consumer<T> extends Specific.Consumer<T, Throwable> {

  }

  @FunctionalInterface
  interface Function<T, R> extends Specific.Function<T, R, Throwable> {

  }

  @FunctionalInterface
  interface Predicate<T> extends Specific.Predicate<T, Throwable> {

  }
}
