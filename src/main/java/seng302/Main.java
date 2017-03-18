package seng302;

/**
 * Created by cba62 on 15/03/17.
 */
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;


public class Main extends Application {

    private static ArrayList<Boat> boatsInRace;
    private static Race race;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent parent = FXMLLoader.load(getClass().getClassLoader().getResource("main_window.fxml"));
        DisplayUtils displayUtils = new DisplayUtils();
        displayUtils.setScreenSize(0.75);
        primaryStage.setTitle("Sail Fast");
        primaryStage.setScene(new Scene(parent, displayUtils.getWidthHeight().get(0), displayUtils.getWidthHeight().get(1)));
        primaryStage.setMaximized(false);
        primaryStage.setMinHeight(700);
        primaryStage.setMinWidth(1000);

        //primaryStage.setScene(new Scene(root, Color.web("#aae7df")));
        primaryStage.show();
    }


    public static void main( String[] args )
    {
        Config.initializeConfig();
        String name = "America's Cup Race";
        boatsInRace = RaceVisionFileReader.importStarters();
        Course course = RaceVisionFileReader.importCourse();
        race = new Race(name, course, boatsInRace);
        course.initCourseLatLon();
        DisplayUtils.setMaxMinLatLon(course.getMinLat(), course.getMinLon(), course.getMaxLat(), course.getMaxLon());

        launch(args);
    }

    private static void randomizeOrder(ArrayList<Boat> boats){
        int numBoats = boats.size();
        ArrayList<Integer> places = new ArrayList<>();
        for (int i = 1; i <= numBoats; i++){
            places.add(i);
        }
        Collections.shuffle(places, new Random());
        for (int j = 0; j < numBoats; j++) {
            boats.get(j).setFinishingPlace(places.get(j));
        }

    }

    public static Race getRace() {
        return Main.race;
    }
}

