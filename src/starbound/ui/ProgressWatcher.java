package starbound.ui;

public class ProgressWatcher {

  public interface ProgressListener {
    void starting(String msg);
    void finished(long time);
  }
  
  private long start;

  private final ProgressListener progressListener;
  
  public ProgressWatcher(ProgressListener progressListener) {
    start = System.currentTimeMillis();
    this.progressListener = progressListener;
  }
  
  public void start(String name) {
    progressListener.starting(name);
    this.start = System.currentTimeMillis();
  }

  public void stop() {
    progressListener.finished(System.currentTimeMillis() - start);
  }
}
