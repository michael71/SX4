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
import java.util.ArrayList;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 *
 * @author mblank
 */
public class Timetable {

    private int adr = INVALID_INT;

    //ArrayList<Integer> tripAdrs = new ArrayList<>();
    private final ArrayList<Trip> tripsList = new ArrayList<>();
    private int currentTripIndex = INVALID_INT;
    private String name = "";

    TT_State state = INACTIVE;
    private String tripsString = "";
    private final ArrayList<Timeline> myTimelines = new ArrayList<>();   // need references to all running timelines to be able to stop them

    String message = "";

    /**
     * construct a timetable from address, allTripsString and name info
     *
     * @param adr
     * @param allTtripsString
     * @param name
     */
    Timetable(int adr, String allTtripsString, String name) {
        // parse "time" to "startTime" array
        tripsString = allTtripsString;
        String[] sTrip = allTtripsString.split(",");
        this.name = name;

        this.adr = adr;

        // convert allTripsString to ArrayList of trips
        for (String s2 : sTrip) {
            int t = INVALID_INT;
            try {
                t = Integer.parseInt(s2);
            } catch (NumberFormatException ex) {
            }
            //debug("tripIDs, added #" + t);
            Trip tr = Trip.getTripByAddress(t);
            if (tr != null) {
                tripsList.add(tr);
            }
        }

        state = INACTIVE;   // not started yet
    }

    /**
     * check if all trains in this timetable are currently located at the right
     * sensors
     *
     * @return
     */
    public String checkPositions() {
        // iterate over tripsList

        ArrayList<Integer> ttLocos = new ArrayList<>();

        for (Trip tr : tripsList) {
            int loco = tr.getLocoAddr();
            if (!ttLocos.contains(loco)) {
                // so far we did not check this train/loco
                if (PanelElement.isSensorOccupied(tr.sens1) && (PanelElement.getTrain(tr.sens1) == loco)) {
                    // position o.k.
                    ttLocos.add(loco);
                } else {
                    return "Zug# " + loco + " nicht auf Start-Position (Sensor " + tr.sens1 + ")";
                }
            }
        }
        return "";  // all positions are correct
    }

    // start a new timetable with 0 .. n trips, return true if successful
    public boolean start() {

        // start first trip (index 0)
        currentTripIndex = 0;
        if (tripsList.isEmpty()) {
            currentTripIndex = INVALID_INT;
            message = "timetable empty - no trips";
            error(message);
            return false;
        }

        boolean result = startNewTrip(tripsList.get(currentTripIndex));
        if (result) {
            debug("started timetable=" + adr + " and trip=" + tripsList.get(currentTripIndex).adr);
        } else {
            debug("could not start timetable=" + adr);
        }
        return isActive();
    }

    public String getMessage() {
        return message;
    }

    public ArrayList<Trip> getTripsList() {
        return tripsList;
    }

    public int getCurrentTripIndex() {
        return currentTripIndex;
    }

    public boolean stop() {
        // finish current timetable
        state = INACTIVE;   // stops also "auto() function
        if (currentTripIndex == INVALID_INT) {
            message = "stopping timetable=" + adr + " (no current trip)";
            debug(message);
            return false;
        }

        stopAllTimelines();

        tripsList.get(currentTripIndex).finish();
        message = "finishing timetable=" + adr;
        debug(message);
        currentTripIndex = INVALID_INT;
        return true;

    }

    // continue timetable at the selected index
    public boolean cont(int index) {
        // get trip by index
        debug("cont timetable at trip-index=" + index);
        currentTripIndex = index;
        state = INACTIVE;
        Trip tr = tripsList.get(currentTripIndex);
        if (tr == null) {
            return false;
        }

        return startNewTrip(tr);
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

    public int getAdr() {
        return adr;
    }
    
    public String getName() {
        return name;
    }

    public boolean advanceToNextTrip(boolean repeat) {
        if (state == INACTIVE) {
            error("cannot advance to next Trip because TimeTable is INACTIVE");
            return false;
        }
        if (currentTripIndex == INVALID_INT) {
            error("cannot advance to next Trip because currentTripIndex is INVALID");
            return false;
        }
        currentTripIndex++;
        // is there a next trip??
        if (currentTripIndex >= tripsList.size()) {
            // the was the last trip.
            state = INACTIVE;
            currentTripIndex = INVALID_INT;  // reset
            debug("last trip of timetable was finished.");
            if (repeat) {
                start();   // start again
            }
            return true;
        } else {
            Trip tr = tripsList.get(currentTripIndex);
            debug("starting next trip " + tr.adr + " in " + tr.startDelay + " msecs.");

            return startNewTrip(tr);
        }
    }

    @Override
    public String toString() {
        if (state == INACTIVE) {
            return "Fahrplan(" + adr + "): " + tripsString;
        } else {

            return "Fahrplan(" + adr + "): " + tripsString + " läuft.";
        }
    }

    public void auto(boolean repeat) {

        if (currentTripIndex != INVALID_INT) {
            Trip cTrip = tripsList.get(currentTripIndex);
            switch (state) {
                case ACTIVE:
                    if (cTrip.state == TripState.INACTIVE) {   // current trip has been finished, start a new one (delayed)                        
                        state = WAITING;   // wait for start of new trip
                        debug("current trip " + cTrip.adr + " has ended. start new one 3 seconds after train stop.");
                        // currentTrip has ended, wait three seconds, then start next
                        Timeline timeline = new Timeline(new KeyFrame(
                                Duration.millis(3000 + cTrip.stopDelay),
                                ae -> {
                                    advanceToNextTrip(repeat);
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
