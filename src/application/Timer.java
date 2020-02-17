package application;

import java.io.File;

import javafx.application.Platform;
import javafx.stage.Stage;

public class Timer implements Runnable {
    boolean running, executing, invalid;
    File directory;
    Stage stage;
    TimerWindow tw;

    public Timer(TimerWindow tw, Stage stage) {
        this.tw = tw;
        this.stage = stage;
        this.running = true;
        this.executing = true;
        this.invalid = false;
    }

    @Override
    public void run() {
        while(running) {
            try {
                if(executing) {
                    Platform.runLater(() -> {
                        directory = tw.latestWorld();
                        if(!invalid) {
                            tw.updateTime(directory);
                        }
                    });

                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }
}
