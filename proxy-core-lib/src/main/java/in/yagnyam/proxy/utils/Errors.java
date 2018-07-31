package in.yagnyam.proxy.utils;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * Errors makes it easy to create implementations of the standard functional interfaces (which don't
 * allow checked exceptions).
 *
 * Even for cases where you aren't required to stuff some code into a functional interface, Errors
 * is useful as a concise way to specify how errors will be handled.
 */
public abstract class Errors {

  private static final Rethrowing rethrow = createRethrowing(Errors::asRuntime);
  protected final Consumer<Throwable> handler;

  protected Errors(Consumer<Throwable> error) {
    this.handler = error;
  }

  /**
   * Creates an Errors.Rethrowing which transforms any exceptions it receives into a
   * RuntimeException as specified by the given function, and then throws that RuntimeException.
   *
   * If that function happens to throw an unchecked error itself, that'll work just fine.
   */
  public static Rethrowing createRethrowing(Function<Throwable, RuntimeException> transform) {
    return new Rethrowing(transform);
  }

  /**
   * Creates an Errors.Handling which passes any exceptions it receives to the given handler.
   *
   * The handler is free to throw a RuntimeException if it wants to. If it always throws a
   * RuntimeException, then you should instead create an Errors.Rethrowing using
   * creeateRethrowAs().
   */
  public static Handling createHandling(Consumer<Throwable> handler) {
    return new Handling(handler);
  }

  /**
   * Rethrows any exceptions as runtime exceptions.
   */
  public static Rethrowing rethrow() {
    return rethrow;
  }

  /**
   * Converts the given exception to a RuntimeException, with a minimum of new exceptions to obscure
   * the cause.
   */
  public static RuntimeException asRuntime(Throwable e) {
    if (e instanceof RuntimeException) {
      return (RuntimeException) e;
    } else {
      return new RuntimeException(e);
    }
  }

  /**
   * Passes the given error to be handled by the Errors.
   */
  public void handle(Throwable error) {
    handler.accept(error);
  }

  /**
   * Attempts to run the given runnable.
   */
  public void run(Throwing.Runnable runnable) {
    wrap(runnable).run();
  }

  /**
   * Returns a Runnable whose exceptions are handled by this Errors.
   */
  public Runnable wrap(Throwing.Runnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (Throwable e) {
        handler.accept(e);
      }
    };
  }

  /**
   * Returns a Consumer whose exceptions are handled by this Errors.
   */
  public <T> Consumer<T> wrap(Throwing.Consumer<T> consumer) {
    return val -> {
      try {
        consumer.accept(val);
      } catch (Throwable e) {
        handler.accept(e);
      }
    };
  }

  /**
   * An Errors which is free to rethrow the exception, but it might not.
   *
   * If we want to wrap a method with a return value, since the handler might not throw an
   * exception, we need a default value to return.
   */
  public static class Handling extends Errors {

    protected Handling(Consumer<Throwable> error) {
      super(error);
    }

    /**
     * Attempts to call the given supplier, returns onFailure if there is a failure.
     */
    public <T> T getWithDefault(Throwing.Supplier<T> supplier, T onFailure) {
      return wrapWithDefault(supplier, onFailure).get();
    }

    /**
     * Attempts to call the given supplier, and returns the given value on failure.
     */
    public <T> Supplier<T> wrapWithDefault(Throwing.Supplier<T> supplier, T onFailure) {
      return () -> {
        try {
          return supplier.get();
        } catch (Throwable e) {
          handler.accept(e);
          return onFailure;
        }
      };
    }

    /**
     * Attempts to call the given function, and returns the given value on failure.
     */
    public <T, R> Function<T, R> wrapWithDefault(Throwing.Function<T, R> function, R onFailure) {
      return input -> {
        try {
          return function.apply(input);
        } catch (Throwable e) {
          handler.accept(e);
          return onFailure;
        }
      };
    }

    /**
     * Attempts to call the given function, and returns the given value on failure.
     */
    public <T> Predicate<T> wrapWithDefault(Throwing.Predicate<T> function, boolean onFailure) {
      return input -> {
        try {
          return function.test(input);
        } catch (Throwable e) {
          handler.accept(e);
          return onFailure;
        }
      };
    }
  }

  /**
   * An Errors which is guaranteed to always throw a RuntimeException.
   *
   * If we want to wrap a method with a return value, it's pointless to specify a default value
   * because if the wrapped method fails, a RuntimeException is guaranteed to throw.
   */
  public static class Rethrowing extends Errors {

    private final Function<Throwable, RuntimeException> transform;

    protected Rethrowing(Function<Throwable, RuntimeException> transform) {
      super(error -> {
        throw transform.apply(error);
      });
      this.transform = transform;
    }

    /**
     * Attempts to call the given supplier, throws some kind of RuntimeException on failure.
     */
    public <T> T get(Throwing.Supplier<T> supplier) {
      return wrap(supplier).get();
    }

    /**
     * Attempts to call the given supplier, throws some kind of RuntimeException on failure.
     */
    public <T> Supplier<T> wrap(Throwing.Supplier<T> supplier) {
      return () -> {
        try {
          return supplier.get();
        } catch (Throwable e) {
          throw transform.apply(e);
        }
      };
    }

    /**
     * Attempts to call the given function, throws some kind of RuntimeException on failure.
     */
    public <T, R> Function<T, R> wrap(Throwing.Function<T, R> function) {
      return arg -> {
        try {
          return function.apply(arg);
        } catch (Throwable e) {
          throw transform.apply(e);
        }
      };
    }

    /**
     * Attempts to call the given function, throws some kind of RuntimeException on failure.
     */
    public <T> Predicate<T> wrap(Throwing.Predicate<T> predicate) {
      return arg -> {
        try {
          return predicate.test(arg);
        } catch (Throwable e) {
          throw transform.apply(e); // 1 855 548 2505
        }
      };
    }
  }

}
