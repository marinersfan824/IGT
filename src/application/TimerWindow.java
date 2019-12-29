package application;
	
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
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
	
	private Label time;
	
	private Timer timer;
	
	@Override
	public void start(Stage primaryStage) {
		try {
			timer = new Timer(this, primaryStage);
			Thread t = new Thread(timer);
			
			minecraft_directory = defaultDirectory();
			
			primaryStage.initStyle(StageStyle.UNDECORATED);
			BorderPane root = new BorderPane();
			Scene scene = new Scene(root,300,80);
			
			root.setStyle("-fx-background-color: #111111;");
			
			root.setOnMousePressed(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	                x = primaryStage.getX() - event.getScreenX();
	                y = primaryStage.getY() - event.getScreenY();
	            }
	        });
			
			root.setOnMouseDragged(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	                primaryStage.setX(event.getScreenX() + x);
	                primaryStage.setY(event.getScreenY() + y);
	            }
	        });
			
			Font minecraftFont = Font.loadFont(getClass().getResourceAsStream("/fonts/minecraft.otf"), 40);
			time = new Label(DEFAULT_TEXT);
			time.setFont(minecraftFont);
			time.setTextFill(Paint.valueOf("#eeeeee"));
			root.setCenter(time);
			
			ContextMenu menu = new ContextMenu();
			
			MenuItem directory = new MenuItem("Change Directory");
			directory.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent ae) {
					File f = getDirectory(primaryStage, minecraft_directory);
					if(f != null)
						minecraft_directory = f;
				}
			});
			menu.getItems().add(directory);
			MenuItem defaultDirectory = new MenuItem("Default Directory");
			defaultDirectory.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent ae) {
					minecraft_directory = defaultDirectory();
				}
			});
			menu.getItems().add(defaultDirectory);
			MenuItem closeOption = new MenuItem("Exit");
			closeOption.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent ae) {
					primaryStage.close();
				}
			});
			menu.getItems().add(closeOption);
			
			
			root.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
				@Override
				public void handle(ContextMenuEvent event) {
					menu.show(primaryStage, primaryStage.getX() + event.getSceneX(), primaryStage.getY() + event.getSceneY());
				}
			});
			
			primaryStage.setResizable(false);
			primaryStage.setAlwaysOnTop(true);
			primaryStage.setScene(scene);
			t.start();
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void stop() {
		timer.stop();
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
		if(systemID.indexOf("win") >= 0)
			return 0;
		if(systemID.indexOf("mac") >= 0)
			return 1;
		if(systemID.indexOf("nix") >= 0 || systemID.indexOf("nux") >= 0 || systemID.indexOf("aix") >= 0)
			return 2;
		return -1;
	}
	
	public File latestWorld(Stage stage) {
		try {
			File saves = new File(minecraft_directory.getAbsolutePath()+"\\saves");
			if(!saves.exists()) {
				updateLabel("Invalid Folder");
				timer.setInvalid(true);
			} else {
				timer.setInvalid(false);
				File[] directories = Arrays.stream(saves.listFiles()).filter(f -> f.isDirectory()).toArray(File[]::new);
				Arrays.sort(directories, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
					}
				});
				return directories[directories.length - 1];
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void updateLabel(String text) {
		time.setText(text);
	}

	public void updateTime(File save) {
		try {
			if(save != null && save.exists()) {
				File statsFolder = new File(save.getAbsolutePath() + "\\stats");
				
				if(statsFolder != null && statsFolder.exists()) {

					File[] playerStats = statsFolder.listFiles();
					
					if(playerStats != null && playerStats.length > 0) {
						File stats = playerStats[0];
						String data = Files.readAllLines(stats.toPath()).get(0);
						Pattern p = Pattern.compile("(inute\":)(\\d+)");
						Matcher m = p.matcher(data);
						if(m.find()) {
							updateLabel(formatTime(Long.parseLong(m.group(2))));
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
		Date d = new Date(time+18000000);
		String result = new SimpleDateFormat("HH:mm:ss.SS").format(d);
		return result;
	}
	
	public String trimTime(long time) {
		return ((time < 10) ? "0" : "")+time;
	}
	
	public static void main(String[] args) {
		{
			File f = new File("bruh.txt");
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(f));
				bw.write("epic");
				bw.flush();
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		launch(args);
	}

}
