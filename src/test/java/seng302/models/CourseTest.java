package seng302.models;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Observable;
import java.util.Observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by atc60 on 16/03/17.
 */
public class CourseTest {

    private final double DELTA = 1e-6;
    private static Course defaultCourse;

    @BeforeClass
    public static void beforeClass(){
        //Initialise a default course for testing.

        defaultCourse = new Course();

        Mark startLine1 = new Mark(0, "Start Line 1", new Coordinate(32.296577, -64.854304));
        Mark startLine2 = new Mark(1, "Start Line 2", new Coordinate(32.293771, -64.855242));
        RaceLine start = new RaceLine(1, "Start Line", startLine1, startLine2);

        Mark mark1 = new Mark(2, "Mark 1", new Coordinate(32.293039, -64.843983));
        CompoundMark mark = new CompoundMark(2, "Mark", mark1);

        Mark gate1 = new Mark(3, "Gate 1", new Coordinate(32.284680, -64.850045));
        Mark gate2 = new Mark(4, "Gate 2", new Coordinate(32.280164, -64.847591));
        CompoundMark gate = new CompoundMark(3, "Gate", gate1, gate2);

        Mark finishLine1 = new Mark(5, "Finish Line 1", new Coordinate(32.317379, -64.839291));
        Mark finishLine2 = new Mark(6, "Finish Line 2", new Coordinate(32.317257, -64.836260));
        RaceLine finish = new RaceLine(4, "Finish", finishLine1, finishLine2);

        defaultCourse.addNewCompoundMark(start);
        defaultCourse.addNewCompoundMark(mark);
        defaultCourse.addNewCompoundMark(gate);
        defaultCourse.addNewCompoundMark(finish);

        defaultCourse.addMarkInOrder(1);
        defaultCourse.addMarkInOrder(2);
        defaultCourse.addMarkInOrder(3);
        defaultCourse.addMarkInOrder(4);

        defaultCourse.addToBoundary(new Coordinate(32.318879, -64.840291));
        defaultCourse.addToBoundary(new Coordinate(32.328879, -64.840291));
        defaultCourse.addToBoundary(new Coordinate(32.328879, -64.865304));
        defaultCourse.addToBoundary(new Coordinate(32.318879, -64.865304));

    }

    @Test
    public void defaultWindDirectionTest(){
        Course course = new Course();
        assertEquals(0.0, course.getWindDirection(), 0);
    }

    @Test
    public void distanceBetweenMarksTest(){
        assertEquals(0.562050737, defaultCourse.distanceBetweenMarks(0, 1), DELTA); //start and mark;
        assertEquals(0.588289145, defaultCourse.distanceBetweenMarks(1, 2), DELTA); //mark and gate;
        assertEquals(2.054763984, defaultCourse.distanceBetweenMarks(2, 3), DELTA); //gate and finish;
        assertEquals(1.583759843, defaultCourse.distanceBetweenMarks(0, 3), DELTA); //start and finish;
        assertEquals(1.490365837, defaultCourse.distanceBetweenMarks(1, 3), DELTA); //mark and finish;
    }

    @Test
    public void headingsBetweenMarksTest(){
        assertEquals(103.1714893, defaultCourse.headingsBetweenMarks(0, 1), DELTA); //start and mark;
        assertEquals(211.5126065, defaultCourse.headingsBetweenMarks(1, 2), DELTA); //mark and gate;
        assertEquals(17.62453811, defaultCourse.headingsBetweenMarks(2, 3), DELTA); //gate and finish;
        assertEquals(32.96972826, defaultCourse.headingsBetweenMarks(0, 3), DELTA); //start and finish;
        assertEquals(12.19238350, defaultCourse.headingsBetweenMarks(1, 3), DELTA); //mark and finish;
    }

    @Test
    public void initCourseLatLonTest(){
        defaultCourse.initCourseLatLon();
        assertEquals(32.276164, defaultCourse.getMinLat(), DELTA);
        assertEquals(-64.869304, defaultCourse.getMinLon(), DELTA);
        assertEquals(32.332879, defaultCourse.getMaxLat(), DELTA);
        assertEquals(-64.83226, defaultCourse.getMaxLon(), DELTA);
    }

    @Test
    public void outOfBoundsWindDirectionTest(){
        Course course = new Course();
        course.setWindDirection(-45);

        Course course2 = new Course();
        course2.setWindDirection(450);
        assertEquals(315.0, course.getWindDirection(), 0);
        assertEquals(90.0, course2.getWindDirection(), 0);
    }

    @Test
    public void mergeWithOtherCourseTest() throws Exception{
        class TestObserver implements Observer {
           public boolean updated = false;
            @Override
            public void update(Observable o, Object arg) {
                updated = true;
            }
        }
        TestObserver observer = new TestObserver();

        Course startCourse = new Course();
        startCourse.addToBoundary(new Coordinate(1, 1));
        startCourse.addObserver(observer);

        Course otherCourse = new Course();
        otherCourse.addToBoundary(new Coordinate(2,2));

        assertEquals(1, startCourse.getBoundary().get(0).getLat(), 0);
        assertEquals(1, startCourse.getBoundary().get(0).getLon(),0);

        startCourse.mergeWithOtherCourse(otherCourse);

        assertEquals(2, startCourse.getBoundary().get(0).getLat(), 0);
        assertEquals(2, startCourse.getBoundary().get(0).getLon(), 0);
        assertTrue(observer.updated);
    }

    @Test
    public void updateMarkTest(){
        Course course = new Course();

        Mark startLine1 = new Mark(0, "Start Line 1", new Coordinate(32.296577, -64.854304));
        Mark startLine2 = new Mark(1, "Start Line 2", new Coordinate(32.293771, -64.855242));
        RaceLine start = new RaceLine(1, "Start Line", startLine1, startLine2);
        course.addNewCompoundMark(start);

        assertEquals(startLine1.getPosition().getLat(), 32.296577, 0);
        assertEquals(startLine1.getPosition().getLon(), -64.854304, 0);
        course.updateMark(0, 12.0, 128.0);
        assertEquals(startLine1.getPosition().getLat(), 12.0, 0);
        assertEquals(startLine1.getPosition().getLon(), 128.0, 0);
    }

}