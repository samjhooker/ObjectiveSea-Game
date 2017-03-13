package seng302;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * Created on 6/03/17.
 * Collection of methods for reading in data from files. Files must be located in the DATA_PATH folder
 * TODO: exit program or use some default values on failed read, rather than catching exceptions and failing later
 */

public class RaceVisionFileReader {

    private static final String DATA_PATH = "data/";
    private static final String STARTERS_FILE = "starters.txt";
    private static final String COURSE_FILE = "course.txt";

    /**
     * Imports file found at COURSE_FILE in DATA_PATH
     *
     * Marks defined as:
     *      MarkName
     *      Latitude Longitude
     *
     * Example file format:
     *      Mark 1
     *      123.4 56.8
     *      Mark 2
     *      345.6 -34.5
     *      Mark Order:
     *      Mark 1
     *      Mark 2
     *      Mark 1
     *
     * @return course - a Course object as specified in the file
     */
    public static Course importCourse() {
        String filePath = DATA_PATH + COURSE_FILE;
        Course course = new Course();

        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            
            String markName = br.readLine();
            while (markName != null){
                /** define each mark */
                String line = br.readLine();
                StringTokenizer st = new StringTokenizer(line);
                double lat = Double.parseDouble(st.nextToken());
                double lon = Double.parseDouble(st.nextToken());
                course.addNewMark(new Mark(markName, lat, lon));
                markName = br.readLine();

                if (markName.equals("Mark Order:")){
                    /** once 'Mark Order' token found, read in order of marks throughout course */
                    markName = null;
                    String mark = br.readLine();
                    while (mark != null) {
                        course.addMarkInOrder(mark);
                        mark = br.readLine();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.printf("Course file could not be found at %s\n", filePath);
        } catch (IOException e) {
            System.err.printf("Error reading course file. Check it is in the correct format.");
        }
        return course;
    }

    /**
     * Imports file found at STARTERS_FILE in DATA_PATH
     *
     * Boats defined as:
     *      BoatName, Speed
     *
     * Speed is expected in knots
     * @return starters - ArrayList of Boat objects defined in file
     */
    public static ArrayList<Boat> importStarters(){
        ArrayList<Boat> starters = new ArrayList<>();
        String filePath = DATA_PATH + STARTERS_FILE;

        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            ArrayList<Boat> allBoats = new ArrayList<>();

            String line = br.readLine();
            while (line != null){
                StringTokenizer st = new StringTokenizer((line));
                String name = st.nextToken(",");
                double speed = Double.parseDouble(st.nextToken());
                allBoats.add(new Boat(name, speed));
                line = br.readLine();
            }

            Random ran = new Random();
            for (int i = 0; i < Config.NUM_BOATS_IN_RACE; i++){
                starters.add(allBoats.remove(ran.nextInt(allBoats.size())));
            }

        } catch (FileNotFoundException e) {
            System.err.printf("Starters file could not be found at %s\n", filePath);
        } catch (IOException e) {
            System.err.printf("Error reading starters file. Check it is in the correct format.");
        }

        return starters;
    }
}
