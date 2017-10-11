/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package movingballsfx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 *
 * @author Nico Kuijpers
 */
public class MovingBallsFX extends Application {
    
    private Thread threadDraw;
    private final int ballAmount = 10;
    private final double WriterReaderSplit = 0.5;
    private Ball[] ballArray = new Ball[ballAmount];
    private Thread[] threadArray = new Thread[ballAmount];
    private CheckBox[] checkBoxArray = new CheckBox[ballAmount];
    private Circle[] circleArray = new Circle[ballAmount];
    private int minX = 100;
    private int maxX = 700;
    private int maxY = 40*ballAmount;
    private int radius = 10;
    private int minCsX = (maxX + minX) / 2 - 100;
    private int maxCsX = (maxX + minX) / 2 + 100;
    private Rectangle criticalSection;
    private BallMonitor readerMonitor;
    private BallMonitor writerMonitor;

    @Override
    public void start(Stage primaryStage) {
       
        // Create the scene
        Group root = new Group();
        Scene scene = new Scene(root, maxX, maxY);
        ReaderWriterMonitor monitor = new ReaderWriterMonitor();

        readerMonitor = new BallMonitor() {
            @Override
            public void Enter() throws InterruptedException {
                monitor.readerEnter();
            }

            @Override
            public void Leave() throws InterruptedException {
                monitor.readerLeave();
            }
        };

        writerMonitor = new BallMonitor() {
            @Override
            public void Enter() throws InterruptedException {
                monitor.writeEnter();
            }

            @Override
            public void Leave() throws InterruptedException {
                monitor.writerLeave();
            }
        };

        // Check boxes
        for (int i = 0; i < checkBoxArray.length; i++) {
            String text;
            if (i < Math.floor(ballAmount * WriterReaderSplit)) {
                // Check box for reader
                text = "Reader"+(i+1);
            }
            else {
                // Check box for writer
                text = "Writer"+(i-(int)Math.floor(ballAmount * WriterReaderSplit)+1);
            }
            final int index = i;
            checkBoxArray[i] = new CheckBox(text);
            checkBoxArray[i].selectedProperty().addListener((obs, oldVal, newVal ) -> checkBoxMouseClicked(index));
            checkBoxArray[i].setLayoutX(radius);
            checkBoxArray[i].setLayoutY((i*4 + 1)*radius);
            root.getChildren().add(checkBoxArray[i]);
        }
        
        // Indicate entire section
        Rectangle entireSection = new Rectangle(minX,0,maxX-minX,maxY);
        entireSection.setFill(Color.LIGHTYELLOW);
        root.getChildren().add(entireSection);
        
        // Indicate critical section
        criticalSection = new Rectangle(minCsX,0,maxCsX-minCsX,maxY);
        criticalSection.setFill(Color.LIGHTGREEN);
        root.getChildren().add(criticalSection);
        
        // Define circles for each ball
        for (int i = 0; i < circleArray.length; i++) {
            CheckBox cb = checkBoxArray[i];
            int y = (int) cb.getLayoutY() + radius;
            if (i < Math.floor(ballAmount * WriterReaderSplit))
                // Reader
                circleArray[i] = new Circle(minX, y, radius, Color.RED);
            else
                // Writer
                circleArray[i] = new Circle(minX, y, radius, Color.BLUE);
            circleArray[i].setVisible(false);
            root.getChildren().add(circleArray[i]);
        }
        
        // Define title and assign the scene for main window
        primaryStage.setTitle("Moving Balls");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Start thread to draw each 20 ms
        threadDraw = new Thread(new DrawRunnable());
        threadDraw.start();
    }
    
    private void checkBoxMouseClicked(int index) {
        CheckBox cb = checkBoxArray[index];
        int y = (int) cb.getLayoutY() + radius;
        if (cb.isSelected() && index < Math.floor(ballAmount * WriterReaderSplit)) {
            // Reader selected: new red ball
            Ball b = new Ball(minX, maxX, minCsX, maxCsX, y, Color.RED);
            ballArray[index] = b;
            Thread t = new Thread(new BallRunnable(b, criticalSection.getX(),criticalSection.getX() + criticalSection.getWidth(), readerMonitor));
            threadArray[index] = t;
            circleArray[index].setVisible(true);
            t.start();
        } else if (cb.isSelected() && index >= Math.floor(ballAmount * WriterReaderSplit)) {
            // Writer selected: new blue ball
            Ball b = new Ball(minX, maxX, minCsX, maxCsX, y, Color.BLUE);
            ballArray[index] = b;
            Thread t = new Thread(new BallRunnable(b,criticalSection.getX(),criticalSection.getX() + criticalSection.getWidth(), writerMonitor));
            threadArray[index] = t;
            circleArray[index].setVisible(true);
            t.start();
        } else {
            // Reader or writer deselected: remove ball
            threadArray[index].interrupt();
            threadArray[index] = null;
            ballArray[index] = null;
            circleArray[index].setVisible(false);
            circleArray[index].setCenterX(minX);
        }
    }
    
    private void updateCircles() {
        for (int i = 0; i < ballArray.length; i++) {
            Ball b = ballArray[i];
            Circle c = circleArray[i];
            if (b != null) {
                c.setCenterX(b.getXPos());
                c.setCenterY(b.getYPos());
            }
        }
    }

    @Override
    public void stop() {
        threadDraw.interrupt();
        for (int i = 0; i < threadArray.length; i++) {
            if (threadArray[i] != null) {
                threadArray[i].interrupt();
            }
        }
    }
  
    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    // Update circles each 20 ms
    private class DrawRunnable implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(20);
                    Platform.runLater(() -> updateCircles());
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
    }
}
