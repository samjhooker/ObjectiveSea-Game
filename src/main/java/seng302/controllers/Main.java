package seng302.controllers;

/**
 * Main class. Sets up server and client and starts GUI.
 */
import javafx.application.Application;
import javafx.application.Preloader;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;
import javafx.stage.WindowEvent;
import javafx.event.EventHandler;
import javafx.application.Platform;
import seng302.data.ConnectionManager;
import seng302.data.DataStreamReader;
import seng302.models.ServerOptions;
import seng302.utilities.Config;
import seng302.models.Race;

import java.io.IOException;


public class Main extends Application {

    private static Scene scene;
    private static Client client;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Config.initializeConfig();
        //setupServer();
        setupClient();

        Parent parent = FXMLLoader.load(getClass().getClassLoader().getResource("main_window.fxml"));
        primaryStage.setTitle("Race Vision");
        primaryStage.getIcons().add(new Image("graphics/icon.png"));
        scene = new Scene(parent);
        primaryStage.setScene(scene);
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setHeight(primaryScreenBounds.getHeight());
        primaryStage.setWidth(primaryScreenBounds.getWidth());
        notifyPreloader(new Preloader.StateChangeNotification(Preloader.StateChangeNotification.Type.BEFORE_START));
        primaryStage.show();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                Platform.exit();
                System.exit(0);
            }
        });

        UserInputController userInputController = new UserInputController(scene, Client.getRace());
        client.setUserInputController(userInputController);
        userInputController.addObserver(client);
    }

    public static void main( String[] args ) {
        if (args.length >= 1) {
            launchWithArguments(args);
        } else {
            launch(args);
        }
    }

    /**
     * Parse command line arguments and use them to launch the application
     * @param args arguments that application was started with
     */
    private static void launchWithArguments(String[] args) {
        if (args[0].equals("-s")){
            try {
                Config.initializeConfig();
                ServerOptions serverOptions = new ServerOptions();
                serverOptions.setSpeedScale(Config.MOCK_SPEED_SCALE);
                setupServer(serverOptions);
                System.out.println("Headless server started.");
            } catch (Exception e) {
                System.out.println("Failed to start headless server.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Initializes the client on it's own thread.
     */
    private static void setupClient() {
        client = new Client();
        Thread clientThread = new Thread(client);
        clientThread.setName("Client");
        clientThread.start();
    }

    /**
     * Creates a Server object, puts it in it's own thread and starts the thread
     */
    private static void setupServer(ServerOptions serverOptions) throws IOException {
        Server server = new Server(serverOptions);
        Thread serverThread = new Thread(server);
        serverThread.setName("Server");
        serverThread.start();
    }

    public static Client getClient() {
        return client;
    }
}

