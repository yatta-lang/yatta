package abzu.runtime;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;

public final class Promise {
  private static final AtomicReferenceFieldUpdater<Promise, Object> UPDATER = AtomicReferenceFieldUpdater.newUpdater(Promise.class, Object.class, "value");

  private volatile Object value;

  public void fulfil(Object o) {
    Object snapshot;
    do {
      snapshot = value;
    } while (!UPDATER.compareAndSet(this, snapshot, o));
    if (snapshot instanceof Callbacks) {
      ((Callbacks) snapshot).run(o);
    }
  }

  public Object map(Function<? super Object, ?> function) {
    Promise result = null;
    Object snapshot;
    Object update;
    do {
      snapshot = value;
      if (!(snapshot instanceof Callbacks) && snapshot != null) return function.apply(snapshot);
      if (result == null) result = new Promise();
      update = new Callbacks(function, result, (Callbacks) snapshot);
    } while (!UPDATER.compareAndSet(this, snapshot, update));
    return result;
  }

  public static Promise all(Object[] os) {
    Promise result = new Promise();
    Object[] data = new Object[os.length];
    AtomicInteger counter = new AtomicInteger(os.length);
    Object o;
    for (int i = 0; i < os.length; i++) {
      o = os[i];
      if (o instanceof Promise) {
        final int idx = i;
        ((Promise) o).map(v -> {
          data[idx] = v;
          if (counter.decrementAndGet() == 0) result.fulfil(data);
          return v;
        });
      } else {
        data[i] = o;
        if (counter.decrementAndGet() == 0) result.fulfil(data);
      }
    }
    return result;
  }

  private static final class Callbacks {
    final Function<? super Object, ?> function;
    final Promise promise;
    final Callbacks next;

    Callbacks(Function<? super Object, ?> function, Promise promise, Callbacks next) {
      this.function = function;
      this.promise = promise;
      this.next = next;
    }

    void run(Object result) {
      Callbacks cursor = this;
      do {
        promise.fulfil(function.apply(result));
        cursor = cursor.next;
      } while (cursor != null);
    }
  }
}
