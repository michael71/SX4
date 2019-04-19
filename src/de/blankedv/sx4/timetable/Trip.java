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
import static com.esotericsoftware.minlog.Log.info;
import static de.blankedv.sx4.Constants.*;
import de.blankedv.sx4.SXData;
import static de.blankedv.sx4.timetable.PanelElement.STATE_FREE;
import static de.blankedv.sx4.timetable.PanelElement.STATE_OCCUPIED;
import static de.blankedv.sx4.timetable.Vars.allCompRoutes;
import static de.blankedv.sx4.timetable.Vars.allLocos;
import static de.blankedv.sx4.timetable.Vars.allRoutes;
import static de.blankedv.sx4.timetable.VarsFX.allTrips;
import java.util.ArrayList;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * A Trip is a combination of a route with a loco-command (adr,speed,dir) and is
 * used to run a loco/train over a route to a destination (sensor)
 *
 * example for trip xml content: trip id="3100" routeid="2300" sens1="924"
 * sens2="902" loco="29,1,126" stopdelay="1500"
 *
 * @author mblank
 */
public class Trip implements Comparable<Trip> {

    int adr = INVALID_INT;
    int route = INVALID_INT;
    int sens1 = INVALID_INT;     // startSensor
    int sens2 = INVALID_INT;     // stopSensor
    String locoString = "";    // adr,dir,speed
    int locoAddr = INVALID_INT;
    int locoDir = INVALID_INT;
    int locoSpeed = INVALID_INT;
    int startDelay = INVALID_INT;  // milliseconds
    int stopDelay = INVALID_INT;  // milliseconds

    boolean locked = false;

    TripState state = TripState.INACTIVE;
    Loco loco = null;
    final ArrayList<Timeline> myTimelines = new ArrayList<>();   // need references to all running timelines to be able to stop them
    int currSpeedPercent = 0;

    String message = "";

    enum TripState {
        INACTIVE, ACTIVE, WAITING, WAITING_FOR_ROUTE
    }

    Trip() {

    }

    public boolean isLocked() {
        return locked;
    }

    public void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
    }

    public int getLocoAddr() {
        return locoAddr;
    }

    public String getMessage() {
        return message;
    }

    public int getAdr() {
        return adr;
    }

    public void setAdr(int adr) {
        this.adr = adr;
    }

    public int getRoute() {
        return route;
    }

    public void setRoute(int route) {
        this.route = route;
    }

    public int getSens1() {
        return sens1;
    }

    public void setSens1(int sens1) {
        this.sens1 = sens1;
    }

    public int getSens2() {
        return sens2;
    }

    public void setSens2(int sens2) {
        this.sens2 = sens2;
    }

    public String getLocoString() {
        return locoString;
    }

    public void setLocoString(String locoString) {
        this.locoString = locoString;
    }

    public void setLocoAddr(int locoAddr) {
        this.locoAddr = locoAddr;
    }

    public int getLocoDir() {
        return locoDir;
    }

    public void setLocoDir(int locoDir) {
        this.locoDir = locoDir;
    }

    public int getLocoSpeed() {
        return locoSpeed;
    }

    public void setLocoSpeed(int locoSpeed) {
        this.locoSpeed = locoSpeed;
    }

    public int getStartDelay() {
        return startDelay;
    }

    public void setStartDelay(int startDelay) {
        this.startDelay = startDelay;
    }

    public int getStopDelay() {
        return stopDelay;
    }

    public void setStopDelay(int stopDelay) {
        this.stopDelay = stopDelay;
    }

    public Loco getLoco() {
        return loco;
    }

    public void setLoco(Loco loco) {
        this.loco = loco;
    }

    public boolean convertLocoData() {
        // convert locoString string to int values for address, direction and speed
        String[] lData = locoString.split(",");
        if (lData.length < 2) {
            return false;
        }

        try {
            locoAddr = Integer.parseInt(lData[0]);
            locoDir = Integer.parseInt(lData[1]);
            if ((locoDir != 0) && (locoDir != 1)) {
                return false;
            }
            if (lData.length >= 3) {
                locoSpeed = Integer.parseInt(lData[2]);
            } else {
                locoSpeed = 28;
            }
            loco = new Loco(locoAddr, locoDir, 0);   // INITIAL SPEED = 0
            if (Loco.getByAddress(locoAddr) == null) {
                // not know, add to allLocos list
                allLocos.add(loco);
            }

        } catch (NumberFormatException e) {
            return false;

        }
        return true;
    }

    public boolean start() {
        if (SXData.getActualPower() == false) {
            message = "ERROR: keine Gleisspannung, kann Fahrt nicht starten!";
            error(message);
            return false;
        }
        PanelElement startSensor = PanelElement.getByAddress(sens1);

        if (startSensor.getState() == STATE_FREE) {

            message = "cannot start trip id=" + adr + " because no train on start-sensor " + sens1;
            error(message);
            return false;
        }

        int trainNumber = startSensor.getTrain();
        if (trainNumber != locoAddr) {
            message = "cannot start trip id=" + adr + " because WRONG train=" + trainNumber + " on start-sensor " + sens1;
            error(message);
            return false;
        }

        boolean couldSetRoutes = setRouteID(route);
        if (!couldSetRoutes) {
            message = "waiting for loco start for trip id=" + adr + " - cannot set (comp)route id=" + route;
            error(message);
            state = TripState.WAITING_FOR_ROUTE;
            return true;
        }

        // aquire locoString and start 'full' speed
        prepareStartLoco();
        message = "starting trip id=" + adr;
        debug(message);
        state = TripState.ACTIVE;
        return true;
    }
    
    public void retryStart() {
        boolean couldSetRoutes = setRouteID(route);
        if (!couldSetRoutes) {
            message = "retry, still waiting for loco start for trip id=" + adr + " - cannot set (comp)route id=" + route;
            error(message);
            return;
        }

        // aquire locoString and start 'full' speed
        prepareStartLoco();
        message = "finally starting trip id=" + adr;
        debug(message);
        state = TripState.ACTIVE;
    }

    public void finish() {
        debug("finish trip " + adr);
        // finish current loco
        stopLoco();
        clearRoutes();
        stopAllTimelines();
        state = TripState.INACTIVE;

    }

    private void incrLocoSpeed() {
        currSpeedPercent += 10;
        if (currSpeedPercent < 0) {
            currSpeedPercent = 0;
        }
        if (currSpeedPercent > 100) {
            currSpeedPercent = 100;
        }

        int speed = (locoSpeed * currSpeedPercent) / 100;

        loco.setSpeed(speed);
        loco.setForward(locoDir == 0);
        loco.setLicht(true);

        SXData.update(loco.getAddr(), loco.getSX(), true); // true => send to SXinterface
    }

    private void startLoco() {
        debug("starting loco");
        loco.setSpeed(0);  // licht an, richtige direction, aber noch nicht losfahren
        loco.setForward(locoDir == 0);
        loco.setLicht(true);
        currSpeedPercent = 0;
        SXData.update(loco.getAddr(), loco.getSX(), true); // true => send to SXinterface
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(1000),
                ae -> incrLocoSpeed()
        ));
        timeline.setCycleCount(10); // for slow start of loco, increase speed in steps
        //timeline.setDelay(Duration.seconds(5)); // then increase speed every second
        timeline.play();
        addTimeline(timeline);
    }

    private void prepareStartLoco() {
        debug("waiting for loco start: " + startDelay + " msecs");
        if (startDelay == 0) {
            startLoco();
        } else {
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.millis(startDelay),
                    ae -> {
                        startLoco();
                    }));
            timeline.play();
            addTimeline(timeline);
        }
    }

    public void stopLoco() {

        loco.setSpeed(0);
        loco.setForward(locoDir == 0);
        //sxi.sendLoco(loco.getLok_adr(), loco.getSpeed(), true, loco.isForward(),  false);  // light = true, horn = false
        SXData.update(loco.getAddr(), loco.getSX(), true); // true => send to SXinterface
    }

    private void finishTripDelayed() {
        if ((stopDelay == INVALID_INT) || (stopDelay == 0)) {
            finish();
        } 
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(stopDelay),
                ae -> {
                    finish();
                }
        ));
        timeline.play();
        addTimeline(timeline);
    }

    private boolean setRouteID(int rID) {
        debug("trip: setRoute  =" + rID);

        for (Route r : allRoutes) {
            if (r.getAdr() == rID) {
                if (r.isFreeExceptStart()) {
                    return r.set(true, locoAddr);   // flag: automatic driving
                } else {
                    return false;
                }
            }
        }
        for (CompRoute cr : allCompRoutes) {
            if (cr.getAdr() == rID) {
                if (cr.isFreeExceptStart()) {
                    return cr.set(true, locoAddr);   // flag: automatic driving
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private void clearRoutes() {
        debug("trip: clearRoute =" + route);
        for (Route r : allRoutes) {
            if (r.getAdr() == route) {
                r.clear();
            }
        }
        for (CompRoute cr : allCompRoutes) {
            if (cr.getAdr() == route) {
                cr.clear();
            }
        }

    }

    public boolean checkEndSensor() {
        PanelElement seEnd = PanelElement.getByAddress(sens2);
        if ((state != TripState.ACTIVE) || (seEnd == null)) {
            return false;
        }

        return (seEnd.getState() == STATE_OCCUPIED); // this trip ends
    }

    // to be able to sort the trips by their ID
    @Override
    public int compareTo(Trip o) {
        if (this.adr < o.adr) {
            return 1;
        } else {
            return -1;
        }
    }

    static Trip get(int index) {
        for (Trip t : allTrips) {
            if (t.adr == index) {
                return t;
            }
        }
        return null;  // not found
    }

    // check for all active trips if an END sensor is reached
    public static void auto() {
        for (Trip tr : allTrips) {
            if (tr.state == TripState.ACTIVE) {
                boolean endSensorReached = tr.checkEndSensor();
                if (endSensorReached) {
                    tr.state = TripState.WAITING;  // avaid triggering finish a second time
                    tr.finishTripDelayed();  // incl. finish loco
                }
            } else if (tr.state == TripState.WAITING_FOR_ROUTE) {
                tr.retryStart();
            }
        }
    }

    public void addTimeline(Timeline t) {
        myTimelines.add(t);
    }

    // STOP timers, like loco speed increase, decrease, start new trip etc.
    public void stopAllTimelines() {
        info("stopping trip=" + adr + " Timelines");
        for (Timeline t : myTimelines) {
            t.stop();
        }
        myTimelines.clear();
    }

    public static Trip getTripByAddress(int addr) {
        for (Trip tr : allTrips) {
            if (tr.adr == addr) {
                return tr;
            }
        }
        return null;
    }
}
