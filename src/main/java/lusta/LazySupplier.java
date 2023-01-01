package lusta;

import java.util.function.Supplier;

public class LazySupplier<T> implements Supplier<T> {

  private final Supplier<T> supplier;

  private T value;

  private boolean supplied;

  public LazySupplier(final Supplier<T> supplier) {
    this.supplier = supplier;
    this.value = null;
    this.supplied = false;
  }

  @Override
  public T get() {
    if (!supplied) {
      try {
        value = supplier.get();
      } finally {
        supplied = true;
      }
    }

    return value;
  }
}
