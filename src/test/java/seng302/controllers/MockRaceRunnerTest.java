package seng302.controllers;

import org.junit.Test;
import seng302.data.RaceStatus;
import seng302.models.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by mjt169 on 11/05/17.
 */
public class MockRaceRunnerTest {

    private double DELTA = 1e-6;

    @Test
    public void raceHasEnded() throws Exception {
        MockRaceRunner runner = new MockRaceRunner();
        Race race  = runner.getRace();
        assertFalse(runner.raceHasEnded());
        race.updateRaceStatus(RaceStatus.TERMINATED);
        assertTrue(runner.raceHasEnded());
    }

    @Test
    public void updateLocationTest() {
        Boat boat = new Boat(0, "TestBoat", "testNickname", 10);
        boat.setCurrentSpeed(10);
        Course course = new Course();

        Mark startLine1 = new Mark(0, "Start Line 1", new Coordinate(50, 30));
        Mark startLine2 = new Mark(1, "Start Line 2", new Coordinate(51, 30));
        RaceLine start = new RaceLine(1, "Start Line",startLine1, startLine2);

        Mark mark1 = new Mark(2, "Mark 1", new Coordinate(60, 60));
        CompoundMark mark = new CompoundMark(2, "Mark", mark1);

        course.addNewCompoundMark(start);
        course.addNewCompoundMark(mark);
        course.addMarkInOrder(1);
        course.addMarkInOrder(2);
        course.setStartLine(start);

        boat.setPosition(start.getPosition().getLat(), start.getPosition().getLon());
        ArrayList<Boat> competitors = new ArrayList<>();
        competitors.add(boat);

        Race race = new Race("Mock Race", course, competitors);

        MockRaceRunner runner = new MockRaceRunner(race);

        runner.updateLocation(58.98, course, boat);
        assertEquals(55.65688918, boat.getCurrentLat(), DELTA);
        assertEquals(45.52296393, boat.getCurrentLon(), DELTA);
    }

    @Test
    public void updateTackingLocationTest() {
        Boat boat = new Boat(0, "TestBoat", "testNickname", 10);
        boat.setCurrentSpeed(10);

        Mark startLine1 = new Mark(0, "Start Line 1", new Coordinate(50, 30));
        Mark startLine2 = new Mark(1, "Start Line 2", new Coordinate(51, 30));
        RaceLine start = new RaceLine(1, "Start Line",startLine1, startLine2);
        Mark mark1 = new Mark(2, "Mark 1", new Coordinate(60, 60));
        CompoundMark windwardGate = new CompoundMark(2, "Mark", mark1);

        Course course = new Course();
        course.addNewCompoundMark(start);
        course.addNewCompoundMark(windwardGate);
        course.addMarkInOrder(1);
        course.addMarkInOrder(2);
        course.setWindDirection(30);
        course.setStartLine(start);

        double distanceGained = 58.98 * boat.getVMGofBoat();
        ArrayList<CompoundMark> courseOrder = course.getCourseOrder();
        double windDirection = course.getWindDirection();
        double bearing = course.headingsBetweenMarks(0,1);
        double alphaAngle = Math.abs(bearing - windDirection) % 360;

        boat.setPosition(start.getPosition().getLat(), start.getPosition().getLon());
        ArrayList<Boat> competitors = new ArrayList<>();
        competitors.add(boat);

        Race race = new Race("Mock Race", course, competitors);
        MockRaceRunner runner = new MockRaceRunner(race);

        runner.tackingUpdateLocation(distanceGained, courseOrder, true, alphaAngle, boat);
        assertEquals(60.0, boat.getCurrentLat(), DELTA);
        assertEquals(60.0, boat.getCurrentLon(), DELTA);
    }

    @Test
    public void updateGybingLocationTest() {
        Boat boat = new Boat(0, "TestBoat", "testNickname", 10);
        boat.setCurrentSpeed(10);

        Mark startLine1 = new Mark(0, "Start Line 1", new Coordinate(50, 30));
        Mark startLine2 = new Mark(1, "Start Line 2", new Coordinate(51, 30));
        RaceLine start = new RaceLine(1, "Start Line",startLine1, startLine2);
        Mark mark1 = new Mark(2, "Mark 1", new Coordinate(60, 60));
        CompoundMark windwardGate = new CompoundMark(2, "Mark", mark1);

        Course course = new Course();
        course.addNewCompoundMark(start);
        course.addNewCompoundMark(windwardGate);
        course.addMarkInOrder(1);
        course.addMarkInOrder(2);
        course.setWindDirection(30);
        course.setStartLine(start);

        double distanceGained = 58.98 * boat.getGybeVMGofBoat() * - 1;
        double windDirection = course.getWindDirection();
        double bearing = course.headingsBetweenMarks(0,1);
        double alphaAngle;
        alphaAngle = 180 - Math.abs(bearing - windDirection) % 360;

        boat.setPosition(start.getPosition().getLat(), start.getPosition().getLon());
        ArrayList<Boat> competitors = new ArrayList<>();
        competitors.add(boat);
        Race race = new Race("Mock Race", course, competitors);
        MockRaceRunner runner = new MockRaceRunner(race);

        runner.tackingUpdateLocation(distanceGained, course.getCourseOrder(), false, alphaAngle, boat);
        assertEquals(60.0, boat.getCurrentLat(), DELTA);
        assertEquals(60.0, boat.getCurrentLon(), DELTA);
    }


    @Test
    public void passedMarkTest() {
        Boat boat = new Boat(0, "TestBoat", "testNickname", 10);
        boat.setCurrentSpeed(10);

        Course course = new Course();

        Mark startLine1 = new Mark(0, "Start Line 1", new Coordinate(50, 30));
        Mark startLine2 = new Mark(1, "Start Line 2", new Coordinate(50.02, 30.02));
        RaceLine start = new RaceLine(1, "Start Line",startLine1, startLine2);

        Mark mark1 = new Mark(2, "Mark 1", new Coordinate(50, 30.5));
        CompoundMark mark = new CompoundMark(2, "Mark", mark1);

        Mark finishLine1 = new Mark(3, "Finish Line 1", new Coordinate(50.5, 30.5));
        Mark finishLine2 = new Mark(4, "Finish Line 2", new Coordinate(50.52, 30.52));
        RaceLine finish = new RaceLine(3, "Finish", finishLine1, finishLine2);

        course.addNewCompoundMark(start);
        course.addNewCompoundMark(mark);
        course.addNewCompoundMark(finish);
        course.addMarkInOrder(1);
        course.addMarkInOrder(2);
        course.addMarkInOrder(3);
        course.setStartLine(start);

        boat.setPosition(50, 30);
        ArrayList<Boat> competitors = new ArrayList<>();
        competitors.add(boat);

        Race race = new Race("Mock Race", course, competitors);
        MockRaceRunner runner = new MockRaceRunner(race);

        runner.updateLocation(2, course, boat);

        assertEquals(50.02421042, boat.getCurrentLat(), DELTA);
        assertEquals(30.50047471, boat.getCurrentLon(), DELTA);
        assertEquals( 1, boat.getLastRoundedMarkIndex());
    }

    @Test
    public void setStartingPositions() throws Exception {
        Boat boat0 = new Boat(0, "TestBoat0", "testNickname", 10);
        Boat boat1 = new Boat(1, "TestBoat1", "testNickname", 10);
        Boat boat2 = new Boat(2, "TestBoat2", "testNickname", 10);

        Course course = new Course();

        Mark startLine1 = new Mark(0, "Start Line 1", new Coordinate(50, 30));
        Mark startLine2 = new Mark(1, "Start Line 2", new Coordinate(53, 30));
        RaceLine start = new RaceLine(1, "Start Line",startLine1, startLine2);

        Mark finishLine1 = new Mark(3, "Finish Line 1", new Coordinate(50.5, 30.5));
        Mark finishLine2 = new Mark(4, "Finish Line 2", new Coordinate(50.52, 30.52));
        RaceLine finish = new RaceLine(2, "Finish", finishLine1, finishLine2);

        course.addNewCompoundMark(start);
        course.addNewCompoundMark(finish);
        course.addMarkInOrder(1);
        course.addMarkInOrder(2);
        course.setStartLine(start);

        ArrayList<Boat> competitors = new ArrayList<>();
        competitors.add(boat0);
        competitors.add(boat1);
        competitors.add(boat2);

        Race race = new Race("Mock Race", course, competitors);
        MockRaceRunner runner = new MockRaceRunner(race);
        //setStartingPositions will be called during initialization

        assertEquals(51, boat0.getCurrentLat(), DELTA);
        assertEquals(30, boat0.getCurrentLon(), DELTA);
        assertEquals(52, boat1.getCurrentLat(), DELTA);
        assertEquals(30, boat1.getCurrentLon(), DELTA);
        assertEquals(53, boat2.getCurrentLat(), DELTA);
        assertEquals(30, boat2.getCurrentLon(), DELTA);
    }


    @Test
    public void finishedRaceTest() {
        Boat boat = new Boat(0, "TestBoat0", "testNickname", 10);

        Course course = new Course();

        Mark startLine1 = new Mark(0, "Start Line 1", new Coordinate(51.55, 30.11));
        Mark startLine2 = new Mark(1, "Start Line 2", new Coordinate(51.60, 30.16));
        RaceLine start = new RaceLine(1, "Start Line", startLine1, startLine2);

        Mark finishLine1 = new Mark(2, "Finish Line 1", new Coordinate(51.56, 30.12));
        Mark finishLine2 = new Mark(3, "Finish Line 2", new Coordinate(51.61, 30.18));
        RaceLine finish = new RaceLine(2, "Finish Line", finishLine1, finishLine2);

        course.addNewCompoundMark(start);
        course.addNewCompoundMark(finish);
        course.addMarkInOrder(1);
        course.addMarkInOrder(2);
        course.setStartLine(start);
        course.setFinishLine(finish);

        boat.setPosition(51.55, 30.11);
        List<Boat> competitors = new ArrayList<>();
        competitors.add(boat);

        Race race = new Race("Mock Race", course, competitors);
        MockRaceRunner runner = new MockRaceRunner(race);

        runner.updateLocation(20, course, boat);

        assertTrue(boat.isFinished());
        assertEquals(51.585, boat.getCurrentLat(), DELTA);
        assertEquals(30.15, boat.getCurrentLon(), DELTA);
    }
}