package sjtu.ipads.wtune.systhesis;

import java.util.ArrayList;
import java.util.List;

public abstract class Stage {
  private Stage next = sink();

  public boolean offer(Object obj) {
    return next.feed(obj);
  }

  public abstract boolean feed(Object o);

  public Stage next() {
    return next;
  }

  public void setNext(Stage next) {
    this.next = next != null ? next : sink();
  }

  public static Stage sink() {
    return SINK;
  }

  public static <T> Stage listCollector(List<T> dest) {
    return new ToList<>(dest);
  }

  private static final Stage SINK = new SinkStage();

  private static class SinkStage extends Stage {
    @Override
    public boolean feed(Object o) {
      return true;
    }
  }

  private static class ToList<T> extends Stage {
    private final List<T> list;

    private ToList(List<T> list) {
      this.list = list != null ? list : new ArrayList<>();
    }

    @Override
    public boolean feed(Object o) {
      list.add((T) o);
      return true;
    }
  }
}
