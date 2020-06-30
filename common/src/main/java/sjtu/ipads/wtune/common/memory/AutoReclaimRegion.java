package sjtu.ipads.wtune.common.memory;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class AutoReclaimRegion {
  public final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();
  private final Map<Integer, Resource> association = new ConcurrentHashMap<>();

  public AutoReclaimRegion(ReclaimWorker worker) {
    worker.register(this::reclaim);
  }

  private static int hash(Object obj) {
    return System.identityHashCode(obj);
  }

  public Object get(Object holder) {
    return association.get(hash(holder)).resource;
  }

  public void associate(Object owner, Object resource) {
    final int hash = hash(owner);
    association.computeIfAbsent(hash, ignored -> new Resource(ref(owner), resource));
  }

  public Object associate(Object owner, Supplier<Object> resource) {
    return association.computeIfAbsent(
            hash(owner), ignored -> new Resource(ref(owner), resource.get()))
        .resource;
  }

  private Reference<Object> ref(Object obj) {
    return new OwnerRef(obj, refQueue);
  }

  int i = 0;

  private void reclaim0(Reference<?> ref) {
    System.out.println(i++);
    association.remove(ref.hashCode());
  }

  private boolean reclaim() {
    Reference<?> ref;
    boolean didWork = false;
    while ((ref = refQueue.poll()) != null) {
      reclaim0(ref);
      didWork = true;
    }
    return didWork;
  }

  private static class OwnerRef extends PhantomReference<Object> {
    private final int hashCode;

    public OwnerRef(Object referent, ReferenceQueue<? super Object> q) {
      super(referent, q);
      hashCode = AutoReclaimRegion.hash(referent);
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      OwnerRef ownerRef = (OwnerRef) o;
      return hashCode == ownerRef.hashCode;
    }
  }

  private static class Resource {
    private final Reference<Object> ref;
    private final Object resource;

    private Resource(Reference<Object> ref, Object resource) {
      this.ref = ref;
      this.resource = resource;
    }
  }
}
