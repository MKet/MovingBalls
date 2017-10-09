/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package movingballsfx;

/**
 *
 * @author Peter Boots
 */
public class BallRunnable implements Runnable {

    private Ball ball;
    private final double criticalXEnter;
    private final double criticalXLeave;
    private BallMonitor monitor;
    boolean WithinCritical = false;

    public BallRunnable(Ball ball, double criticalXEnter, double criticalXLeave, BallMonitor monitor) {
        this.ball = ball;
        this.criticalXEnter = criticalXEnter;
        this.criticalXLeave = criticalXLeave;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (!WithinCritical && ball.getXPos() > criticalXEnter && ball.getXPos() < criticalXLeave) {
                        monitor.Enter();
                        WithinCritical = true;
                    } else if (WithinCritical && ball.getXPos() > criticalXLeave) {
                        WithinCritical = false;
                        monitor.Leave();
                    }
                    ball.move();
                    Thread.sleep(ball.getSpeed());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

            }
        } finally {
            if (WithinCritical)
                try {
                    monitor.Leave();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }
}
