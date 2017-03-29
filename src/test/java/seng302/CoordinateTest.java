package seng302;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Coordinate class
 * Created on 30/03/17.
 */
public class CoordinateTest {

    @Test
    public void gcDistanceTest(){
        Coordinate coord1 = new Coordinate(50, 30);
        Coordinate coord2 = new Coordinate(60, 60);
        assertEquals(1179, (int)Math.round(coord1.greaterCircleDistance(coord2)));
    }

    @Test
    public void headingToCoordinateTest(){
        Coordinate coord1 = new Coordinate(50, 45);
        Coordinate coord2 = new Coordinate(100, 28);
        assertEquals(4, Math.round(coord1.headingToCoordinate(coord2)));
    }

    @Test
    public void headingToSouthCoordinateTest(){
        Coordinate coord1 = new Coordinate(120, 30);
        Coordinate coord2 = new Coordinate(100, 30);
        assertEquals(180, Math.round(coord1.headingToCoordinate(coord2)));
    }

    @Test
    public void headingToSameCoordinateTest(){
        Coordinate coord1 = new Coordinate(50, 30);
        Coordinate coord2 = new Coordinate(50, 30);
        assertEquals(0, Math.round(coord1.headingToCoordinate(coord2)));
    }
}
