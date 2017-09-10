package seng302.controllers;

/**
 * Main class. Sets up server and client and starts GUI.
 */
import javafx.application.Application;
import javafx.application.Preloader;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;
import javafx.application.Platform;
import seng302.data.registration.ServerFullException;
import seng302.models.ClientOptions;
import seng302.models.ServerOptions;
import seng302.utilities.ConnectionUtils;
import seng302.utilities.DisplaySwitcher;
import seng302.utilities.NoConnectionToServerException;
import java.io.IOException;
import java.net.BindException;


public class Main extends Application {
    private static Client client;
    private Server server;
    private Stage primaryStage;
    private DisplaySwitcher displaySwitcher;


    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Objective Sea");
        this.primaryStage.getIcons().add(new Image("graphics/icon.png"));
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        this.primaryStage.setHeight(primaryScreenBounds.getHeight());
        this.primaryStage.setWidth(primaryScreenBounds.getWidth());
        displaySwitcher = new DisplaySwitcher(this, primaryStage);
        loadMainMenu();
        notifyPreloader(new Preloader.StateChangeNotification(Preloader.StateChangeNotification.Type.BEFORE_START));
        this.primaryStage.show();
        this.primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main( String[] args ) {
        if (args.length >= 1) {
            Main main = new Main();
            main.launchWithArguments(args);
        } else {
            launch(args);
        }
    }

    /**
     * Parse command line arguments and use them to launch the application
     * @param args arguments that application was started with
     */
    private void launchWithArguments(String[] args) {
        if (args[0].equals("server")){
            try {
                ServerOptions serverOptions = new ServerOptions();
                serverOptions.setNumRacesToRun(-1);
                for (int i = 1; i < args.length; i+=2) {
                    switch(args[i]) {
                        case "-p":
                            serverOptions.setPort(Integer.parseInt(args[i + 1]));
                            break;
                        case "-n":
                            serverOptions.setMinParticipants(Integer.parseInt(args[i + 1]));
                            break;
                        case "-m":
                            serverOptions.setRaceXML(args[i + 1]);
                            break;
                        case "-s":
                            serverOptions.setSpeedScale(Double.parseDouble(args[i + 1]));
                            break;
                        case "-r":
                            serverOptions.setNumRacesToRun(Integer.parseInt(args[i + 1]));
                            break;
                        default:
                            throw new IllegalArgumentException(String.format("Unknown argument \"%s\"", args[i]));
                    }
                }
                setupServer(serverOptions);
                System.out.println("Headless server started.");
            } catch (IllegalArgumentException iae) {
                System.out.print("Invalid server arguments. ");
                System.out.println(iae.getMessage());
                Platform.exit();
            } catch (IOException e) {
                System.out.println("Failed to start headless server.");
                e.printStackTrace();
                Platform.exit();
            }
        }
    }

    /**
     * Creates a Server object, puts it in it's own thread and starts the thread
     */
    private void setupServer(ServerOptions serverOptions) throws IOException {
        server = new Server(serverOptions);
        ConnectionUtils.setServer(server);
        Thread serverThread = new Thread(server);
        serverThread.setName("Server");
        serverThread.start();
    }

    public static Client getClient() {
        return client;
    }

    private void loadMainMenu() {
        displaySwitcher.loadMainMenu();
    }

    /**
     * Loads the visualiser and attaches a KeyInputController to the client and the JavaFX scene
     * @param options ClientOptions for the RaceView
     */
    public void loadRaceView(ClientOptions options) {
        displaySwitcher.loadRaceView(options);
        if (options.isParticipant()) {
            KeyInputController keyInputController = new KeyInputController(DisplaySwitcher.getScene(), Client.getRace());
            TouchInputController touchInputController = new TouchInputController(DisplaySwitcher.getScene(), Client.getRace());
            client.setInputControllers(keyInputController, touchInputController);
            keyInputController.addObserver(client);
            touchInputController.addObserver(client);
        }
    }

    /**
     * initilises a hosted race with the provided parameters
     * @param course course name
     * @param port port number to host
     * @param isTutorial is hosted game a tutorial
     * @param clientOptions client options
     * @return whether starting hosted race was successful or not
     * @throws Exception uncaught error
     */
    public boolean startHostedRace(String course, Integer port, Boolean isTutorial, ClientOptions clientOptions) throws Exception{
        ServerOptions serverOptions = new ServerOptions();
        serverOptions.setPort(port);
        serverOptions.setRaceXML(course);
        serverOptions.setTutorial(isTutorial);
        try{
            setupServer(serverOptions);
        } catch(BindException e){
            showPortInUseError(port);
            return false;
        }
        startClient(clientOptions);
        return true;
    }

    /**
     * starts the client at the desired ip and port number
     * ensures that the client connects
     * throws error if connection fails
     * @param options ClientOptions to initialize with
     * @return whether the connection was successful or times out
     */
    public boolean startClient(ClientOptions options){
        try {
            client = new Client(options);
            ConnectionUtils.setClient(client);
            Thread clientThread = new Thread(client);
            clientThread.setName("Client");
            clientThread.start();
        } catch (NoConnectionToServerException e) {
            showServerConnectionError(e);
            return false;
        } catch (ServerFullException e) {
            showServerJoinError(options.isParticipant());
            return false;
        }
        return true;
    }

    /**
     * Shows a popup informing user that they were unable to connect to the server
     */
    private static void showServerConnectionError(NoConnectionToServerException err){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add("style/menuStyle.css");
        dialogPane.getStyleClass().add("myDialog");
        alert.setTitle("Cannot Connect to Server");
        alert.setHeaderText("Cannot Connect to Server");
        if(err.isLocalError()){
            alert.setContentText("No connection to local server.\n\n" +
                    "Please ensure that the Port number \n" +
                    "you have entered is correct.");
        }else{
            alert.setContentText("This server may not be running.\n\n" +
                    "Please ensure that the IP and Port numbers \n" +
                    "you have entered are correct.");
        }

        alert.showAndWait();
    }

    /**
     * Shows a popup informing user that they were not allowed to join the server
     * If they attempted to join as a participant, suggests they try joining as a spectator
     * @param isParticipant whether or not an attempt was made to participate in the race
     */
    private void showServerJoinError(boolean isParticipant) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add("style/menuStyle.css");
        dialogPane.getStyleClass().add("myDialog");
        alert.setTitle("Failed to Join Server");
        alert.setHeaderText("Failed to Join Server");
        String message = "There was not a free slot for you to join the server.\n\n";
        if (isParticipant) message += "You may be able to join as a spectator instead.";
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * shows a popup saying that port is in use.
     * @param port port number to display
     */
    private void showPortInUseError(int port) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add("style/menuStyle.css");
        dialogPane.getStyleClass().add("myDialog");
        alert.setTitle("Failed to Host this Race");
        alert.setHeaderText("Failed to Host this Race");
        String message = "You already have a game running\n" +
                "Please close that game to continue.";
        alert.setContentText(message);
        alert.showAndWait();
    }
}

