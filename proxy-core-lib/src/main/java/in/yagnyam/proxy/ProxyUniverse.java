package in.yagnyam.proxy;

public class ProxyUniverse {

  public static final String PRODUCTION = "production";

  private ProxyUniverse() {
  }

  public static boolean isProduction(String universe) {
    return PRODUCTION.equals(universe);
  }

}
