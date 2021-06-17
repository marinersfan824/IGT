package application;

import java.io.File;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class TimerWindow extends Application {
    private double x, y;

    private File minecraft_directory;

    private static final String DEFAULT_TEXT = "IGT Timer";

    private Label rtaTime;
    private Label igtTime;

    private Timer rtaTimer;
    private Timer igtTimer;
    private long lastTime = -1;
    
    public static long TIMEZONE_OFFSET;

    @Override
    public void start(Stage primaryStage) {
        try {
            rtaTimer = new Timer(this, primaryStage);
            Thread rtaThread = new Thread(rtaTimer);
            igtTimer = new Timer(this, primaryStage);
            Thread igtThread = new Thread(igtTimer);

            minecraft_directory = defaultDirectory();

            primaryStage.initStyle(StageStyle.UNDECORATED);
            BorderPane root = new BorderPane();
            Scene scene = new Scene(root,300,100);

            root.setStyle("-fx-background-color: #111111;");

            root.setOnMousePressed(event -> {
                x = primaryStage.getX() - event.getScreenX();
                y = primaryStage.getY() - event.getScreenY();
            });

            root.setOnMouseDragged(event -> {
                primaryStage.setX(event.getScreenX() + x);
                primaryStage.setY(event.getScreenY() + y);
            });

            Font minecraftFont = Font.loadFont(getClass().getResourceAsStream("/fonts/minecraft.otf"), 40);
            rtaTime = new Label(DEFAULT_TEXT);
            rtaTime.setFont(minecraftFont);
            rtaTime.setTextFill(Paint.valueOf("#eeeeee"));
            root.setTop(rtaTime);
            igtTime = new Label(DEFAULT_TEXT);
            igtTime.setFont(minecraftFont);
            igtTime.setTextFill(Paint.valueOf("#eeeeee"));
            root.setBottom(igtTime);

            ContextMenu menu = new ContextMenu();

            MenuItem directory = new MenuItem("Change Directory");
            directory.setOnAction(ae -> {
                File f = getDirectory(primaryStage, minecraft_directory);
                if(f != null)
                    minecraft_directory = f;
            });
            menu.getItems().add(directory);
            MenuItem defaultDirectory = new MenuItem("Default Directory");
            defaultDirectory.setOnAction(ae -> minecraft_directory = defaultDirectory());
            menu.getItems().add(defaultDirectory);
            MenuItem closeOption = new MenuItem("Exit");
            closeOption.setOnAction(ae -> primaryStage.close());
            menu.getItems().add(closeOption);


            root.setOnContextMenuRequested(event -> menu.show(primaryStage, primaryStage.getX() + event.getSceneX(), primaryStage.getY() + event.getSceneY()));

            primaryStage.setTitle("IGT Timer");
            primaryStage.getIcons().add(new Image("images/crystal.png"));
            primaryStage.setResizable(false);
            primaryStage.setAlwaysOnTop(true);
            primaryStage.setScene(scene);
            rtaThread.start();
            igtThread.start();
            primaryStage.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        rtaTimer.stop();
        igtTimer.stop();
    }


    public File getDirectory(Stage stage, File previous) {
        try {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(stage);
            return (f != null && f.exists() && f.isDirectory()) ? f : previous;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public File defaultDirectory() {
        try {
            int os = os();
            if(os == 0)
                return new File(System.getProperty("user.home")+"/AppData/Roaming/.minecraft");
            if(os == 1)
                return new File("/Users/"+System.getProperty("user.name")+"/Library/Application Support/minecraft");
            if(os == 2)
                return new File("/home/"+System.getProperty("user.name")+"/.minecraft");
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int os() {
        String systemID = System.getProperty("os.name").toLowerCase();
        if(systemID.contains("win"))
            return 0;
        if(systemID.contains("mac"))
            return 1;
        if(systemID.contains("nix") || systemID.contains("nux") || systemID.contains("aix"))
            return 2;
        return -1;
    }

    public File latestWorld() {
        try {
            File saves = new File(minecraft_directory.getAbsolutePath() + File.separator + "saves");
            if(!saves.exists()) {
                updateLabel("Invalid Folder", "Invalid Folder");
                rtaTimer.setInvalid(true);
                igtTimer.setInvalid(true);
            } else {
                igtTimer.setInvalid(false);
                rtaTimer.setInvalid(false);
                File[] directories = Arrays.stream(Objects.requireNonNull(saves.listFiles())).filter(file -> file.isDirectory()).toArray(File[]::new);

                if (directories.length == 0) {
                    return null;
                }

                File latestDirectory = directories[0];
                long latestDirectoryTime = latestDirectory.lastModified();
                for (int i = 1; i < directories.length; i++) {
                    long curDirectoryTime = directories[i].lastModified();
                    if (curDirectoryTime > latestDirectoryTime) {
                        latestDirectory = directories[i];
                        latestDirectoryTime = curDirectoryTime;
                    }
                }

                return latestDirectory;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateLabel(String rta, String igt) {
        rtaTime.setText(rta);
        igtTime.setText(igt);
    }

    public void updateTime(File save) {
        try {
            if(save != null && save.exists()) {
                File statsFolder = new File(save.getAbsolutePath() + File.separator + "stats");

                if(statsFolder.exists()) {

                    File[] playerStats = statsFolder.listFiles();

                    if(playerStats != null && playerStats.length > 0) {
                        File stats = playerStats[0];
                        String data = Files.readAllLines(stats.toPath()).get(0);
                        //Replace play_time with inute for 1.6-1.16
                        Pattern igt = Pattern.compile("(play_time\":)(\\d+)");
                        Pattern rta = Pattern.compile("(total_world_time\":)(\\d+)");
                        Matcher igtM = igt.matcher(data);
                        Matcher rtaM = rta.matcher(data);
                        if(igtM.find() && rtaM.find()) {
                            long igt2 = Long.parseLong(igtM.group(2));
                            long rta2 = Long.parseLong(rtaM.group(2));
                            if (rta2 != this.lastTime || igt2 != this.lastTime) {
                                this.lastTime = rta2;
                                this.updateLabel(this.formatTime(rta2), this.formatTime(igt2));
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public String formatTime(long time) {
        time *= 50;
        Date d = new Date(time - TIMEZONE_OFFSET);
        return new SimpleDateFormat("HH:mm:ss.SS").format(d);
    }

    public static void main(String[] args) {
        TimeZone time = TimeZone.getDefault();
        TIMEZONE_OFFSET = time.getRawOffset();
        launch(args);
    }

}
