package seng302.utilities;

import java.time.*;
import java.util.DoubleSummaryStatistics;
import java.util.TimeZone;

/**
 * A utility class for converting between various time units to aid in clean and readable code in other places.
 */
public class TimeUtils {

    private static final double NANOSECONDS_IN_SECOND = 1e9f;
    private static final double SECONDS_IN_MINUTE = 60;
    private static final double MINUTES_IN_HOUR = 60;

    private static boolean incorrectTimeZone = true;
    private static String foundId = new String();

    public static double convertSecondsToHours(double seconds){
        return seconds / (SECONDS_IN_MINUTE * MINUTES_IN_HOUR);
    }


    /**
     * Takes the UTC offset given by the regatta xml file and returns the local time of the race
     * @param UTCOffset The UTC Offset from the Data Stream
     * @return String containing the correct time for the given time zone
     */
    public static String setTimeZone(double UTCOffset) {
        String utcFormat = "";
        try {
            utcFormat = formatUTCOffset(UTCOffset);
            if(utcFormat.equals("")) {
                throw new Exception("Incorrect TimeZone in XML file. TimeZone reset to default.");
            }
        } catch (Exception e){
            utcFormat = "+00:00";
            System.out.print(e.getMessage());
        } finally {
            Instant instant = Instant.now();
            ZoneId zone = ZoneId.of(utcFormat);
            ZonedDateTime zonedDateTime = instant.atZone(zone);
            int hours = zonedDateTime.getHour();
            int minutes = zonedDateTime.getMinute();
            int seconds = zonedDateTime.getSecond();
            return String.format("%02d:%02d:%02d UTC%s", hours, minutes, seconds, utcFormat);
        }
    }

    /**
     * Takes the UTC offset and returns it in acceptable ZoneId format
     * @param UTCOffset
     * @return utcFormat - String of the formatted UTC offset
     */
    private static String formatUTCOffset(double UTCOffset) {
        String utcFormat = "";
        String positiveRounded = String.format("%02d", (int)UTCOffset);
        String negativeRounded = String.format("%03d", (int)UTCOffset);
        if ((UTCOffset >= 0) && (UTCOffset < 10)) {
            if ((UTCOffset % 1) == 0) {
                utcFormat = "+" + positiveRounded + ":00";
            } else {
                utcFormat = "+" + positiveRounded + ":30";
            }
        } else if ((UTCOffset > 10) && (UTCOffset <= 14)) {
            if ((UTCOffset % 1) == 0) {
                utcFormat = "+" + positiveRounded + ":00";
            } else {
                utcFormat = "+" + positiveRounded + ":30";
            }
        } else if ((UTCOffset < 0) && (UTCOffset > -10)) {
            if ((UTCOffset % 1) == 0) {
                utcFormat = negativeRounded + ":00";
            } else {
                utcFormat = negativeRounded + ":30";
            }
        } else if ((UTCOffset <= -10) && (UTCOffset >= -12)) {
            if ((UTCOffset % 1) == 0) {
                utcFormat = negativeRounded + ":00";
            } else {
                utcFormat = negativeRounded + ":30";
            }
        }
        return utcFormat;
    }

    public static double convertNanosecondsToSeconds(double nanoseconds){
        return nanoseconds / NANOSECONDS_IN_SECOND;
    }

    public static double convertHoursToSeconds(double hours){
        return hours * (SECONDS_IN_MINUTE * MINUTES_IN_HOUR);
    }

    public static double convertMinutesToSeconds(double seconds){
        return seconds * SECONDS_IN_MINUTE;
    }

    public static Double convertMmPerSecondToKnots(Integer mmPerSecond){
        Double kilometersInNauticalMile = 1.852;
        Double kilometersPerSecond = mmPerSecond / 1e6;
        Double kilometersPerHour = kilometersPerSecond * SECONDS_IN_MINUTE * MINUTES_IN_HOUR;
        Double knots = kilometersPerHour / kilometersInNauticalMile;
        return knots;
    }

    public static String getFormatUTCOffset(double UTCOffset) {
        return formatUTCOffset(UTCOffset);
    }
}