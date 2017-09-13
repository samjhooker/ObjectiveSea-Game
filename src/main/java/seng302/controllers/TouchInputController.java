package seng302.controllers;

import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import seng302.data.BoatAction;
import seng302.models.*;
import seng302.utilities.DisplayUtils;
import seng302.utilities.MathUtils;
import seng302.utilities.PolarReader;
import seng302.views.DisplayTouchController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Observable;
import java.util.Set;

import static java.lang.Math.abs;
import static java.lang.Math.multiplyExact;

/**
 * handles user key presses.
 */
public class TouchInputController extends Observable {

    private int commandInt;
    private int clientID;
    private Race race;
    private final Set<EventType<TouchEvent>> consumedTouchEvents = new HashSet<>(Arrays.asList(TouchEvent.TOUCH_MOVED, TouchEvent.TOUCH_PRESSED));
    private DisplayTouchController displayTouchController;
    private CanvasCoordinate swipeStart;
    private Boat playersBoat;
    private double timeElapsed;
    private double touchTime;
    private boolean multipleFingers;
    private PolarTable polarTable;
    private CanvasCoordinate previousCoordinate;
    private Pane touchPane;
    private Group root;

    /**
     * Sets up user key press handler.
     *
     * @param boat
     */
    public TouchInputController(Race race, Boat boat) {
        this.race = race;
        this.polarTable = new PolarTable(PolarReader.getPolarsForAC35Yachts(), race.getCourse());
        this.playersBoat = boat;
    }

    private void touchEventListener() {
        // Pane touchPane = (Pane) scene.lookup("#touchPane");

        touchPane.addEventFilter(TouchEvent.ANY, touch -> {
            if (touch.getEventType() == TouchEvent.TOUCH_PRESSED) {
                touchTime = System.currentTimeMillis();
            }
            checkTouchMoved(touch);
            if (consumedTouchEvents.contains(touch.getEventType())) {
                touch.consume();
            }
        });

        touchPane.setOnScrollStarted(swipe -> {
            swipeStart = new CanvasCoordinate(swipe.getScreenX(), swipe.getScreenY());
            multipleFingers = false;
            if (swipe.getTouchCount() != 1) {
                multipleFingers = true;
            }
            timeElapsed = System.currentTimeMillis();
        });

        touchPane.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent swipe) {
                CanvasCoordinate currentCoordinate = new CanvasCoordinate(swipe.getX(), swipe.getY());
                if (previousCoordinate == null) {
                    previousCoordinate = currentCoordinate;
                }
                if (swipe.getTouchCount() > 0) {
                    displayTouchController.displaySwipe(currentCoordinate, previousCoordinate);
                    previousCoordinate = currentCoordinate;
                }

            }
        });

        touchPane.setOnScrollFinished(swipe -> {
            CanvasCoordinate swipeEnd = new CanvasCoordinate(swipe.getScreenX(), swipe.getScreenY());
            double differenceX = Math.pow((swipeStart.getX() - swipeEnd.getX()), 2);
            double differenceY = Math.pow((swipeStart.getY() - swipeEnd.getY()), 2);
            double lengthXY = Math.sqrt(differenceX + differenceY);
            if (lengthXY > 130 && Math.abs(System.currentTimeMillis() - timeElapsed) < 300 && !multipleFingers && !DisplayUtils.externalTouchEvent) {
                displayTouchController.displaySwipe(swipeEnd, swipeStart);
                double swipeBearing = MathUtils.getHeadingBetweenTwoCoodinates(swipeStart, swipeEnd);
                swipeAction(swipe, swipeBearing);
                previousCoordinate = null;
            }
        });

        touchPane.addEventFilter(TouchEvent.ANY, touch -> {
            if (touch.getTouchPoints().size() == 2 && DisplayUtils.zoomLevel != 1 && DisplayUtils.externalZoomEvent) {
                DisplayUtils.externalDragEvent = false;
                DisplayUtils.externalTouchEvent = true;
                double touchX = (touch.getTouchPoints().get(0).getX() + touch.getTouchPoints().get(1).getX()) / 2;
                double touchY = (touch.getTouchPoints().get(0).getY() + touch.getTouchPoints().get(1).getY()) / 2;
                System.out.println("Change X: " + touchX);
                System.out.println("Change Y: " + touchY);
                DisplayUtils.dragDisplay((int) touchX, (int) touchY);
//                raceViewController.redrawCourse();
//                raceViewController.redrawBoatPaths();
//                selectionController.deselectBoat();
            }
        });
    }

    private void swipeAction(ScrollEvent swipe, double swipeBearing) {
        double boatHeading = race.getBoatById(clientID).getHeading();
        double headingDifference = abs(boatHeading - swipeBearing) % 180;
        if (root.getTransforms().size() > 1) {
            headingDifference = swipeBearing;
        }
        if (headingDifference <= 15 || headingDifference >= 165) {
            if (Math.abs(boatHeading - swipeBearing) < 15 || Math.abs(boatHeading - swipeBearing) > 345) {
                if (playersBoat.isSailsIn()) {
                    commandInt = BoatAction.SAILS_IN.getType();
                    setChanged();
                    notifyObservers();
                }
            } else {
                if (!playersBoat.isSailsIn()) {
                    commandInt = BoatAction.SAILS_IN.getType();
                    setChanged();
                    notifyObservers();
                }
            }
        } else {
            double optimumHeading = playersBoat.getTackOrGybeHeading(race.getCourse(), polarTable);
            if (Math.abs(optimumHeading - swipeBearing) < 50 || Math.abs(optimumHeading - swipeBearing) > 310) {
                commandInt = BoatAction.TACK_GYBE.getType();
                setChanged();
                notifyObservers();
            }
        }
    }

    private void checkTouchMoved(TouchEvent touchEvent) {
        Boat playersBoat = race.getBoatById(clientID);
        if (touchEvent.getTouchPoints().size() == 1 && !DisplayUtils.externalTouchEvent && (System.currentTimeMillis() - touchTime) > 200) {
            CanvasCoordinate touchPoint = new CanvasCoordinate(touchEvent.getTouchPoint().getSceneX(), touchEvent.getTouchPoint().getSceneY());
            CanvasCoordinate boatPosition = DisplayUtils.convertFromLatLon(playersBoat.getCurrentPosition());
            double windAngle = race.getCourse().getWindDirection() + 180;
            double touchAngle = touchPoint.getAngleFromSceneCentre(boatPosition) - 270;

            if (touchAngle < 0) {
                touchAngle += 360;
            }

            if (windAngle > 360) {
                windAngle -= 360;
            }

            double boatHeading = playersBoat.getHeading();

            //downWind towards wind arrow - 6
            //upWind away from wind arrow - 5

            double oppositeWindAngle = windAngle - 180;
            if (oppositeWindAngle < 0) {
                oppositeWindAngle += 360;
            }
            double newWindAngle;
            double newBoatHeading;
            double newTouchAngle;


            if (!((boatHeading > (touchAngle - 2)) && (boatHeading < (touchAngle + 2)))) {
                if ((((boatHeading - touchAngle) < 180) && ((boatHeading - touchAngle) > 0)) ||
                        (((boatHeading - touchAngle) < -180) && ((boatHeading - touchAngle) > -360))) {             //Anti Clockwise
                    if (boatHeading > touchAngle) {
                        commandInt = changeRotationDirection(windAngle, oppositeWindAngle, boatHeading, touchAngle);
                    } else if (boatHeading < touchAngle) {
                        if (windAngle < 180) {
                            newWindAngle = windAngle + 360;
                        } else {
                            newWindAngle = windAngle;
                            oppositeWindAngle += 360;
                        }
                        newBoatHeading = boatHeading + 360;

                        commandInt = changeRotationDirection(newWindAngle, oppositeWindAngle, newBoatHeading, touchAngle);
                    }
                } else if ((((boatHeading - touchAngle) > 180) && ((boatHeading - touchAngle) < 360)) ||
                        (((boatHeading - touchAngle) > -180) && ((boatHeading - touchAngle) < 0))) {                //Clockwise
                    if (boatHeading < touchAngle) {
                        commandInt = changeOppositeRotationDirection(windAngle, oppositeWindAngle, boatHeading, touchAngle);
                    } else if (boatHeading > touchAngle) {
                        if (windAngle < 180) {
                            newWindAngle = windAngle + 360;
                        } else {
                            newWindAngle = windAngle;
                            oppositeWindAngle += 360;
                        }

                        newTouchAngle = touchAngle + 360;

                        commandInt = changeOppositeRotationDirection(newWindAngle, oppositeWindAngle, boatHeading, newTouchAngle);
                    }
                }
            } else {
                commandInt = -1;
            }

            if (commandInt != -1) {
                setChanged();
                notifyObservers();
            }
        }
    }


    private int changeRotationDirection(double windAngle, double oppositeWindAngle, double boatHeading, double touchAngle) {

        int direction = -1;

        if (boatHeading > windAngle && touchAngle < windAngle) {
            direction = 6;
        } else if (boatHeading > oppositeWindAngle && touchAngle < oppositeWindAngle) {
            direction = 5;
        } else if (((touchAngle - windAngle) < 180 && (touchAngle - windAngle) > 0) || ((touchAngle - windAngle) < -180 && (touchAngle - windAngle) > -360)) {
            direction = 6;
        } else if (((touchAngle - oppositeWindAngle) < 180 && (touchAngle - oppositeWindAngle) > 0) || ((touchAngle - oppositeWindAngle) < -180 && (touchAngle - oppositeWindAngle) > -360)) {
            direction = 5;
        }

        return direction;
    }

    private int changeOppositeRotationDirection(double windAngle, double oppositeWindAngle, double boatHeading, double touchAngle) {

        int direction = -1;

        if (boatHeading < windAngle && touchAngle > windAngle) {
            direction = 6;
        } else if (boatHeading < oppositeWindAngle && touchAngle > oppositeWindAngle) {
            direction = 5;
        } else if (((touchAngle - windAngle) < 180 && (touchAngle - windAngle) > 0) || ((touchAngle - windAngle) < -180 && (touchAngle - windAngle) > -360)) {
            direction = 5;
        } else if (((touchAngle - oppositeWindAngle) < 180 && (touchAngle - oppositeWindAngle) > 0) || ((touchAngle - oppositeWindAngle) < -180 && (touchAngle - oppositeWindAngle) > -360)) {
            direction = 6;
        }
        return direction;
    }

    int getCommandInt() {
        return commandInt;
    }

    public void setUp(Group root, Pane touchPane){
        this.touchPane = touchPane;
        this.root = root;
        this.displayTouchController = new DisplayTouchController(touchPane);
        touchEventListener();
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }
}

