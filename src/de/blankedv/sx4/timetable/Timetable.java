/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4.timetable;

import static com.esotericsoftware.minlog.Log.debug;
import static com.esotericsoftware.minlog.Log.error;
import static de.blankedv.sx4.Constants.*;
import static de.blankedv.sx4.Constants.TT_State.*;
import static de.blankedv.sx4.timetable.Vars.allTimetables;
import static de.blankedv.sx4.timetable.Vars.allTrips;
import static de.blankedv.sx4.timetable.TripsTable.tableView;
import java.util.ArrayList;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 *
 * @author mblank
 */
public class Timetable {

    int id = INVALID_INT;
    ArrayList<Integer> startTime = new ArrayList<>();
    ArrayList<Integer> tripIds = new ArrayList<>();
    int currentTripIndex = 0;
    Trip cTrip = null;
    TT_State state = INACTIVE;
    private String tripsString = "";

    int nextTimetable = INVALID_INT;

    boolean active = false;

    Timetable(int id, String time, String trip, String next) {
        // parse "time" to "startTime" array
        tripsString = trip;
        String[] sTime = time.split(",");
        String[] sTrip = trip.split(",");
        startTime = new ArrayList<>();

        if (sTime.length != sTrip.length) {
            error("number of start times in timetable does not match number of trips!");
            id = INVALID_INT;
        } else {
            this.id = id;
            for (String s2 : sTime) {
                int t = INVALID_INT;
                try {
                    t = Integer.parseInt(s2);
                } catch (NumberFormatException ex) {
                }
                startTime.add(t);
            }

            for (String s2 : sTrip) {
                int t = INVALID_INT;
                try {
                    t = Integer.parseInt(s2);
                } catch (NumberFormatException ex) {
                }
                //debug("tripIDs, added #" + t);
                tripIds.add(t);
            }
        }

        state = INACTIVE;
    }

    public boolean start() {
        // start a new timetable
        // TODO Fixed = timetable0 !!

        currentTripIndex = 0;
        // start first trip (index 0)
        cTrip = Trip.get(tripIds.get(currentTripIndex));

        if (cTrip == null) {
            error("in Timetable - no trip found for id=" + currentTripIndex);
            return false;
        }


        return startNewTrip(cTrip);
    }

    public boolean startNewTrip(Trip t) {
        // check if start sensor is occupied and endsensor is free
        // TODO check if complete route is free and set route

        // set route(s)
        int start = PanelElement.getSingleByAddress(t.sens1).getState() & 0x01;   // get "occupied" bit
        int end = PanelElement.getSingleByAddress(t.sens2).getState() & 0x01;   // get "occupied" bit

        if ((start != 0) && (end == 0)) {
            System.out.println("start sensor (" + t.sens1 + ") occ and end sensor(" + t.sens2 + ") free, we can start the trip");
            state = ACTIVE;
            t.start();
            return true;

        } else {
            if (start == 0) {
                System.out.println("start sensor (" + t.sens1 + ") free, we CANNOT start the trip");
            }
            if (end != 0) {
                System.out.println("end sensor (" + t.sens2 + ")is not free, we CANNOT start the trip");
            }
            return false;
        }

    }

    public boolean advanceToNextTrip() {
        currentTripIndex++;
        // is there a next trip??
        if (currentTripIndex >= tripIds.size()) {
            // the was the last trip.
            state = INACTIVE;
            debug("last trip of timetable was finished.");
            return true;
        }
        // get trip 
        try {
            cTrip = Trip.get(tripIds.get(currentTripIndex));
        } catch (IndexOutOfBoundsException ex) {
            cTrip = null;
        }
        if (cTrip == null) {
            System.out.println("ERROR in Timetable - no trip found for id=" + currentTripIndex);
            return false;
        }

        return startNewTrip(cTrip);
    }

    @Override
    public String toString() {
        return "Fahrplan(" + id + "): " + tripsString;
    }

    public static void auto() {

        final Timetable tt = allTimetables.get(0);   // TODO currently only 1 timetable is supported
        if (tt.cTrip == null) {
            return;  // NO CURRENT TRIP - do nothing
        }
        switch (tt.state) {
            case ACTIVE:
                for (Trip tr : allTrips) {
                    if (tr.id == tt.cTrip.id) {
                        if (!tr.isActive()) {
                            tt.state = WAITING;   // wait for start of new trip
                            debug("current trip " + tt.cTrip.id + " has ended. start new one 3 seconds after train stop.");
                            // currentTrip has ended, wait three seconds, then start next
                            Timeline timeline = new Timeline(new KeyFrame(
                                    Duration.millis(3000 + tr.stopDelay),
                                    ae -> {
                                        tt.advanceToNextTrip();
                                    }));
                            timeline.play();
                        }

                    }
                }
                break;
            case WAITING:
            case INACTIVE:
                // do nothing
                break;
        }

    }
}
