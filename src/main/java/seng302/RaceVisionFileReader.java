package seng302;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * Created on 6/03/17.
 * Collection of methods for reading in data from files. Files must be located in the DEFAULT_FILE_PATH folder
 * TODO: exit program or use some default values on failed read, rather than catching exceptions and failing later
 */

public class RaceVisionFileReader {

    private static final String DEFAULT_FILE_PATH = "/defaultFiles/";
    private static final String DEFAULT_STARTERS_FILE = "starters.txt";
    private static final String DEFAULT_COURSE_FILE = "AC35-course.xml";

    private static Document dom;

    /**
     * Manages importing the course from the correct place
     * If a file path is specified, this will be used, otherwise a default is packaged with the jar.
     * Currently this an XML file at DEFAULT_FILE_PATH/DEFAULT_COURSE_FILE
     * @return a Course object
     */
    public static Course importCourse(String filePath) {
        try {
            if (filePath != null && !filePath.isEmpty()) {
                parseXMLFile(filePath, false);
            } else {
                String resourcePath = DEFAULT_FILE_PATH + DEFAULT_COURSE_FILE;
                parseXMLFile(resourcePath, true);
            }
            return importCourseFromXML();
        }  catch (IOException ioe) {
            System.err.printf("Unable to read %s as a course definition file. " +
                    "Ensure it is correctly formatted.\n", filePath);
            ioe.printStackTrace();
            return null;
        }
    }

    /**
     * Attempts to read the desired XML file into the Document parser
     * @param filePath - the location of the file to be read, must be XML
     * @param isResource specifies whether the file is packaged in the resources folder
     * @throws IOException if the file is not found
     */
    public static void parseXMLFile(String filePath, boolean isResource) throws IOException{
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            if (isResource){
                dom = db.parse(RaceVisionFileReader.class.getResourceAsStream(filePath));
            } else {
                dom = db.parse(filePath);
            }
        } catch(ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch(SAXException se) {
            se.printStackTrace();
        }
    }

    /**
     * Decodes an XML file into a Course object
     *
     * Expected file structure:
     * <course>
     *     <marks>
     *         //definitions of each mark (see parseMark)
     *     </marks>
     *     <legs>
     *          <leg>[Start Mark]</leg>
     *          <leg>[A Mark]</leg>
     *          ...
     *          <leg>[Finish Mark]</leg>
     *     </legs>
     * </course>
     *
     * @return a Course object
     */
    public static Course importCourseFromXML() {
        Course course = new Course();

        try {
            Element root = dom.getDocumentElement();
            if (root.getTagName() != XMLTags.Course.COURSE) {
                String message = String.format("The root tag must be <%s>.", XMLTags.Course.COURSE);
                throw new XMLParseException(XMLTags.Course.COURSE, message);
            }

            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    switch (element.getTagName()) {
                        case XMLTags.Course.MARKS:
                            NodeList marks = element.getElementsByTagName(XMLTags.Course.MARK);
                            for (int j = 0; j < marks.getLength(); j++) {
                                course.addNewMark(parseMark((Element) marks.item(j)));
                            }
                            break;
                        case XMLTags.Course.LEGS:
                            NodeList legs = element.getElementsByTagName(XMLTags.Course.LEG);
                            for (int k = 0; k < legs.getLength(); k++) {
                                course.addMarkInOrder(legs.item(k).getTextContent());
                            }
                            break;
                        case XMLTags.Course.WIND:
                            course.setWindDirection(Double.parseDouble(element.getTextContent()));
                            break;
                        case XMLTags.Course.BOUNDARY:
                            NodeList boundaryCoords = element.getElementsByTagName(XMLTags.Course.LATLON);
                            for (int k = 0; k < boundaryCoords.getLength(); k++) {
                                course.addToBoundary(parseBoundaryCoord(boundaryCoords.item(k)));
                            }
                            break;
                    }
                }
            }
        } catch (XMLParseException e) {
            System.err.printf("Error reading course file around tag <%s>.\n", e.getTag());
            e.printStackTrace();
        }

        return course;
    }

    /**
     * Decodes a mark element into a CompoundMark object
     *
     * Expected structure of a mark:
     * <mark>
     *     <name>Name is required</name>
     *     <latlon>
     *         <lat>...</lat>
     *         <lon>...</lon>
     *     </latlon>
     * </mark>
     *
     * If multiple <latlon> tags exist, the Mark will be interpreted as a Gate object
     *
     * To define a mark as the start mark use the start attribute, e.g.
     * <mark start="start">
     *
     * To define a mark as the finish mark use the finish attribute, e.g.
     * <mark finish="finish">
     *
     * @param markElement - an XML <mark> element
     * @return a CompoundMark (potentially Gate) object
     * @throws XMLParseException when an expected tag is missing or unexpectedly formatted
     */
    private static CompoundMark parseMark(Element markElement) throws XMLParseException {
        CompoundMark mark;

        NodeList nameNodes = markElement.getElementsByTagName(XMLTags.Course.NAME);
        if (nameNodes.getLength() < 1) {
            throw new XMLParseException(XMLTags.Course.NAME, "Required tag was not defined.");
        }
        String name = nameNodes.item(0).getTextContent();
        NodeList latlons = markElement.getElementsByTagName(XMLTags.Course.LATLON);
        if (latlons.getLength() < 1) {
            throw new XMLParseException(XMLTags.Course.LATLON, "Required tag was not defined.");
        }
        double lat1 = extractLatitude((Element) latlons.item(0));
        double lon1 = extractLongitude((Element) latlons.item(0));

        if (latlons.getLength() > 1) { //it is a gate or start or finish
            double lat2 = extractLatitude((Element) latlons.item(1));
            double lon2 = extractLongitude((Element) latlons.item(1));

            //Check whether the mark has a start or finish attribute
            NamedNodeMap attr = markElement.getAttributes();
            if (attr.getNamedItem(XMLTags.Course.START) != null) {
                mark = new RaceLine(name, lat1, lon1, lat2, lon2);
                mark.setMarkAsStart();
            } else if (attr.getNamedItem(XMLTags.Course.FINISH) != null){
                mark = new RaceLine(name, lat1, lon1, lat2, lon2);
                mark.setMarkAsFinish();
            } else{
                mark = new Gate(name, lat1, lon1, lat2, lon2);
            }
        } else { //it is just a single-point mark
            mark = new CompoundMark(name, lat1, lon1);
        }

        return mark;
    }

    /**
     * @param latlons A node with lat and lons tags
     * @return a Coordainte object indicating a point on the boundary
     * @throws XMLParseException XMLParseException if no <lat> or <lon> tag exists
     */
    private static Coordinate parseBoundaryCoord(Node latlons) throws XMLParseException{
        double lat = extractLatitude((Element) latlons);
        double lon = extractLongitude((Element) latlons);
        return new Coordinate(lat, lon);
    }

    /**
     * Pulls the latitude from an XML <lat> element and parses it as a double
     * @param latlon
     * @return a double representing a latitude
     * @throws XMLParseException if no <lat> tag exists
     */
    private static double extractLatitude(Element latlon) throws XMLParseException {
        NodeList anyLats = latlon.getElementsByTagName(XMLTags.Course.LAT);
        if (anyLats.getLength() < 1) {
            throw new XMLParseException(XMLTags.Course.LAT, "Required tag was not defined.");
        }
        Node latElement = anyLats.item(0);
        return Double.parseDouble(latElement.getTextContent());
    }

    /**
     * Pulls the longitude from an XML <lon> element and parses it as a double
     * @param latlon
     * @return a double representing a longitude
     * @throws XMLParseException if no <lon> tag exists
     */
    private static double extractLongitude(Element latlon) throws XMLParseException {
        NodeList anyLons = latlon.getElementsByTagName(XMLTags.Course.LON);
        if (anyLons.getLength() < 1) {
            throw new XMLParseException(XMLTags.Course.LON, "Required tag was not defined.");
        }
        Node lonElement = anyLons.item(0);
        return Double.parseDouble(lonElement.getTextContent());
    }

    /**
     * Imports file found at DEFAULT_STARTERS_FILE in DEFAULT_FILE_PATH in resources folder
     *
     * Boats defined as:
     *      BoatName, Speed
     *
     * Speed is expected in knots
     * @return starters - ArrayList of Boat objects defined in file
     */
    public static ArrayList<Boat> importStarters(String filePath){
        ArrayList<Boat> starters = new ArrayList<>();

        try {
            BufferedReader br;
            if (filePath != null && !filePath.isEmpty()) {
                br = new BufferedReader(new FileReader(filePath));
            } else {
                filePath = DEFAULT_FILE_PATH + DEFAULT_STARTERS_FILE;
                br = new BufferedReader(
                        new InputStreamReader(RaceVisionFileReader.class.getResourceAsStream(filePath)));
            }
            ArrayList<Boat> allBoats = new ArrayList<>();

            String line = br.readLine();
            while (line != null){
                StringTokenizer st = new StringTokenizer((line));
                String name = st.nextToken(",");
                String nickName = st.nextToken().trim();
                double speed = Double.parseDouble(st.nextToken());
                allBoats.add(new Boat(name, nickName, speed));
                line = br.readLine();
            }

            Random ran = new Random();
            for (int i = 0; i < Config.NUM_BOATS_IN_RACE; i++){
                starters.add(allBoats.remove(ran.nextInt(allBoats.size())));
            }
            br.close();

        } catch (FileNotFoundException e) {
            System.err.printf("Starters file could not be found at %s\n", filePath);
        } catch (IOException e) {
            System.err.printf("Error reading starters file. Check it is in the correct format.");
        }

        return starters;
    }
}
