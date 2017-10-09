package movingballsfx;

public interface IBallMonitor {
    void Enter() throws InterruptedException;
    void Leave() throws InterruptedException;
}
