/*
SX4
Copyright (C) 2019 Michael Blank

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.blankedv.sx4.timetable;

import static com.esotericsoftware.minlog.Log.debug;
import static com.esotericsoftware.minlog.Log.error;
import static de.blankedv.sx4.Constants.*;
import static de.blankedv.sx4.Constants.TT_State.*;
import static de.blankedv.sx4.timetable.PanelElement.STATE_FREE;
import static de.blankedv.sx4.timetable.PanelElement.STATE_OCCUPIED;
import de.blankedv.sx4.timetable.Trip.TripState;
import static de.blankedv.sx4.timetable.VarsFX.allTimetables;
import static de.blankedv.sx4.timetable.VarsFX.allTrips;
import java.util.ArrayList;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 *
 * @author mblank
 */
public class Timetable {

    int adr = INVALID_INT;
    ArrayList<Integer> tripAdrs = new ArrayList<>();
    int nextTT = 0;
    int currentTripIndex = 0;
    Trip cTrip = null;
    TT_State state = INACTIVE;
    private String tripsString = "";
    final ArrayList<Timeline> myTimelines = new ArrayList<>();   // need references to all running timelines to be able to stop them

    int nextTimetable = INVALID_INT;

    private TripsTable tripsTable = null;

    String message = "";

    Timetable(int adr, String trip, int next) {
        // parse "time" to "startTime" array
        tripsString = trip;
        String[] sTrip = trip.split(",");
        nextTT = next;  // next timetable after this one is finished

        this.adr = adr;

        for (String s2 : sTrip) {
            int t = INVALID_INT;
            try {
                t = Integer.parseInt(s2);
            } catch (NumberFormatException ex) {
            }
            //debug("tripIDs, added #" + t);
            tripAdrs.add(t);
        }

        state = INACTIVE;
    }

    // start a new timetable with 0 .. n trips, return true if successful
    public boolean start(TripsTable ttable) {

        tripsTable = ttable;  // store for reference, we need it at end of this tt.

        // start first trip (index 0)
        currentTripIndex = 0;
        cTrip = Trip.get(tripAdrs.get(currentTripIndex));

        if (cTrip == null) {
            message = "in Timetable - no trip found for adr=" + currentTripIndex;
            error(message);
            return false;
        }

        boolean result = startNewTrip(cTrip);
        if (result) {
            debug("started timetable=" + adr + " and trip=" + cTrip.adr);
        } else {
            debug("could not start timetable=" + adr);
        }
        return isActive();
    }

    public String getMessage() {
        return message;
    }

    public boolean stop() {
        // finish current timetable
        // TODO Fixed = timetable0 !!
        state = INACTIVE;   // stops also "auto() function
        cTrip = Trip.get(tripAdrs.get(currentTripIndex));
        stopAllTimelines();

        if (cTrip == null) {
            message = "stopping timetable=" + adr + " (no current trip)";
            debug(message);
            return false;
        } else {

            cTrip.finish();
            message = "finishing timetable=" + adr;
            debug(message);
            return true;
        }

    }

    public boolean startNewTrip(Trip t) {
        // check if start sensor is occupied and endsensor is free
        debug("try starting new trip with adr=" + t.adr + " from sens1=" + t.sens1 + " to sens2=" + t.sens2);

        // set route(s)
        int start = PanelElement.getByAddress(t.sens1).getState();
        int end = PanelElement.getByAddress(t.sens2).getState();

        if ((start == STATE_OCCUPIED) && (end == STATE_FREE)) {
            debug("start sensor (" + t.sens1 + ") occupied and end sensor(" + t.sens2 + ") free, we can start the trip");
            state = ACTIVE;
            boolean result = t.start();
            if (result) {
                message = "trip started";
                state = ACTIVE;
                return true;
            } else {
                message = t.getMessage();
                state = INACTIVE;
                return false;
            }

        } else {
            if (start != STATE_OCCUPIED) {
                message = "start sensor (" + t.sens1 + ") free, we CANNOT start the trip";
                debug(message);
            }
            if (end != STATE_FREE) {
                message = "end sensor (" + t.sens2 + ")is not free, we CANNOT start the trip";
                debug(message);
            }
            return false;
        }

    }

    public boolean isActive() {
        switch (state) {
            case ACTIVE:
            case WAITING:
                return true;
            case INACTIVE:
            default:
                return false;
        }
    }

    public boolean advanceToNextTrip() {
        if (state == INACTIVE) {
            error("cannot advance to next Trip because TimeTable is INACTIVE");
        }
        currentTripIndex++;
        // is there a next trip??
        if (currentTripIndex >= tripAdrs.size()) {
            // the was the last trip.
            state = INACTIVE;
            debug("last trip of timetable was finished.");
            for (Timetable tt : allTimetables) {
                if (tt.adr == nextTT) {
                    debug("next timetable=" + nextTT);
                    tripsTable.startNewTimetable(tt);
                    return true;
                }
            }
            return true;
        }
        // get trip 
        try {
            cTrip = Trip.get(tripAdrs.get(currentTripIndex));
            debug("found next trip in timetable - trip=" + cTrip.adr);
        } catch (IndexOutOfBoundsException ex) {
            cTrip = null;
        }
        if (cTrip == null) {
            error("ERROR in Timetable - no trip found for adr=" + currentTripIndex);
            return false;
        }

        return startNewTrip(cTrip);
    }

    @Override
    public String toString() {
        String nextS = "";
        if (nextTT > 0) {
            nextS += "(danach Fahrplan" + nextTT + ")";
        }
        if (state == INACTIVE) {
            return "Fahrplan(" + adr + "): " + tripsString + nextS;
        } else {

            return "Fahrplan(" + adr + "): " + tripsString + "läuft." + nextS;
        }
    }

    public void timetableCheck() {

        if (cTrip == null) {
            return;  // NO CURRENT TRIP - do nothing
        }
        switch (state) {
            case ACTIVE:
                if (cTrip.state == TripState.INACTIVE) {   // current trip has been finished, start a new one (delayed)                        
                    state = WAITING;   // wait for start of new trip
                    debug("current trip " + cTrip.adr + " has ended. start new one 5 seconds after train stop.");
                    // currentTrip has ended, wait three seconds, then start next
                    Timeline timeline = new Timeline(new KeyFrame(
                            Duration.millis(5000 + cTrip.stopDelay),
                            ae -> {
                                advanceToNextTrip();
                            }));
                    timeline.play();
                    addTimeline(timeline);
                }

                break;
            case WAITING:
            case INACTIVE:
                // do nothing
                break;
        }

    }

    public void addTimeline(Timeline t) {
        myTimelines.add(t);
    }

    // STOP all timers of this timetable, like loco speed increase, decrease, start new trip etc.
    public void stopAllTimelines() {
        debug("Timetable: stopping all Timelines");
        for (Timeline t : myTimelines) {
            t.stop();
        }
        myTimelines.clear();
    }
}
