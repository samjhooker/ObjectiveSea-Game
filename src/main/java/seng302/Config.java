package seng302;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Created on 6/03/17.
 * A set of configuration options used throughout the software, read from a config file at startup.
 */
public class Config {

    private static final String DEFAULT_CONFIG_PATH = "/defaultFiles/config.txt";
    public static int NUM_BOATS_IN_RACE;
    public static int TIME_SCALE;

    /**
     * This function finds a config file located at DEFAULT_CONFIG_PATH and sets any properties it finds in the file.
     * Example format for property-value pair: NUMBOATS=6
     *
     * @throws IOException and ends at the first unrecognised token it comes across
     */
    public static void initializeConfig(){
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(Config.class.getResourceAsStream(DEFAULT_CONFIG_PATH)));
            String line = br.readLine();
            while (line != null){
                StringTokenizer st = new StringTokenizer(line);

                String token = st.nextToken("=");
                switch(token) {
                    case "NUMBOATS":
                        NUM_BOATS_IN_RACE = Integer.parseInt(st.nextToken());
                        break;
                    case "TIMESCALE":
                        TIME_SCALE = (int)(Double.parseDouble(st.nextToken()) * 60000); //convert mins to milleseconds
                        break;
                    default:
                        throw new IOException("Invalid Token.");
                }

                line = br.readLine();
            }
        } catch (FileNotFoundException e) {
            System.err.printf("Config file could not be found at %s\n", DEFAULT_CONFIG_PATH);
        } catch (IOException e) {
            System.err.printf("Error reading config file. Check it is in the correct format: %s", e);
        }
    }
}
