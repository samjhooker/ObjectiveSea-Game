package seng302;


import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Michael Trotter on 3/21/2017.
 */
public class RaceVisionFileReaderTest {

    @Test
    public void readCourseFileTest(){
        Course course = RaceVisionFileReader.importCourse("data/testFiles/testCourse.xml");
        Course expected = createExpectedCourse();

        Assert.assertNotNull(course);
        for (int i = 0; i < expected.getCourseOrder().size(); i++) {
            assertMarksAreEqual(expected.getCourseOrder().get(i), course.getCourseOrder().get(i));
        }
        for (String key : expected.getMarks().keySet()) {
            assertMarksAreEqual(expected.getMarks().get(key), course.getMarks().get(key));
        }
        Assert.assertEquals(expected.getWindDirection(), course.getWindDirection(), 0);
        Assert.assertTrue(course.getMarks().get("Start").isStart());
        Assert.assertTrue(course.getMarks().get("Finish").isFinish());
        Assert.assertTrue(course.getMarks().get("Start") instanceof Gate);
        Assert.assertTrue(course.getMarks().get("Finish") instanceof Gate);
        Assert.assertTrue(course.getMarks().get("Gate") instanceof Gate);
        Assert.assertFalse(course.getMarks().get("Mark") instanceof Gate);

        //Boundary
        Assert.assertEquals(course.getBoundary().size(), 3);
        Assert.assertEquals(course.getBoundary().get(0).getLat(), 32.5, 0);
        Assert.assertEquals(course.getBoundary().get(0).getLon(), -60.1, 0);

        Assert.assertEquals(course.getBoundary().get(1).getLat(), 32.0, 0);
        Assert.assertEquals(course.getBoundary().get(1).getLon(), -60.1, 0);

        Assert.assertEquals(course.getBoundary().get(2).getLat(), 32.0, 0);
        Assert.assertEquals(course.getBoundary().get(2).getLon(), -60.0, 0);

    }

    @Test
    public void nonExistentCourseFileTest(){
        Course course = RaceVisionFileReader.importCourse("I am a fake file");
        Assert.assertNull(course);
    }

    /** This is a clone of the course that testCourse.xml is expected to create */
    private Course createExpectedCourse() {
        Course expected = new Course();
        Gate start = new Gate("Start", 0, 0, 0, 1);
        start.setMarkAsStart();
        Gate finish = new Gate("Finish", 0, 5, 0, 6);
        finish.setMarkAsFinish();
        expected.addNewMark(start);
        expected.addNewMark(finish);
        expected.addNewMark(new CompoundMark("Mark", 2, 2));
        expected.addNewMark(new Gate("Gate", 3, 3, 4, 4));
        expected.addMarkInOrder("Start");
        expected.addMarkInOrder("Mark");
        expected.addMarkInOrder("Gate");
        expected.addMarkInOrder("Finish");
        expected.setWindDirection(200);
        return expected;
    }

    /** Compares two marks for equality */
    private void assertMarksAreEqual(CompoundMark mark1, CompoundMark mark2){
        Assert.assertEquals(mark1.getName(), mark2.getName());
        Assert.assertEquals(mark1.getLat(), mark2.getLat(), 0);
        Assert.assertEquals(mark1.getLon(), mark2.getLon(), 0);
        if (mark1 instanceof Gate){
            Assert.assertTrue(mark2 instanceof  Gate);
            Gate gate1 = (Gate) mark1;
            Gate gate2 = (Gate) mark2;
            Assert.assertEquals(gate1.getEnd1Lat(), gate2.getEnd1Lat(), 0);
            Assert.assertEquals(gate1.getEnd1Lon(), gate2.getEnd1Lon(), 0);
            Assert.assertEquals(gate1.getEnd2Lat(), gate2.getEnd2Lat(), 0);
            Assert.assertEquals(gate1.getEnd2Lon(), gate2.getEnd2Lon(), 0);
        }
    }

}