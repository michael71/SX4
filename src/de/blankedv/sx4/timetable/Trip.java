/*
 * Copyright (C) 2018 mblank
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.blankedv.sx4.timetable;

import static com.esotericsoftware.minlog.Log.debug;
import static com.esotericsoftware.minlog.Log.error;
import static de.blankedv.sx4.Constants.*;
import de.blankedv.sx4.SXData;
import static de.blankedv.sx4.timetable.PanelElement.STATE_FREE;
import static de.blankedv.sx4.timetable.PanelElement.STATE_OCCUPIED;
import static de.blankedv.sx4.timetable.Vars.RT_INACTIVE;
import static de.blankedv.sx4.timetable.Vars.allCompRoutes;
import static de.blankedv.sx4.timetable.Vars.allRoutes;
import static de.blankedv.sx4.timetable.Vars.allTrips;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.util.Duration;

/**
 *
 * <trip id="3100" routeid="2300" sens1="924" sens2="902" loco="29,1,126" stopdelay="1500" />
 *
 * @author mblank
 */
public class Trip implements Comparable<Trip> {

    int id = INVALID_INT;
    int routeid = INVALID_INT;
    int sens1 = INVALID_INT;     // startSensor
    int sens2 = INVALID_INT;     // stopSensor
    String locoString = "";    // adr,dir,speed
    int locoAddr = INVALID_INT;
    int locoDir = INVALID_INT;
    int locoSpeed = INVALID_INT;
    int stopDelay = INVALID_INT;  // milliseconds
    TripState state = TripState.INACTIVE;
    Loco loco = null;

    int currSpeedPercent = 0;

    enum TripState {
        INACTIVE, ACTIVE, WAITING
    }

    Trip() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRouteid() {
        return routeid;
    }

    public void setRouteid(int routeid) {
        this.routeid = routeid;
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

    public int getLocoAddr() {
        return locoAddr;
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

        } catch (NumberFormatException e) {
            return false;

        }
        return true;
    }

    public boolean start() {
        if (SXData.getActualPower() == false) {
            error("ERROR: keine Gleisspannung, kann Fahrt nicht starten!");
            return false;
        }
        PanelElement startSensor = PanelElement.getSingleByAddress(sens1);

        if (startSensor.getState() == STATE_FREE) {
            error("cannot start trip id=" + id + " because no train on start-sensor " + sens1);
                return false;
        }

        int trainNumber = startSensor.getTrain();
        if (trainNumber != locoAddr) {
            error("cannot start trip id=" + id + " because WRONG train=" + trainNumber + " on start-sensor " + sens1);
            return false;
        }

        boolean couldSetRoutes = setRoute(routeid);
        if (!couldSetRoutes) {
            error("cannot start trip id=" + id + " cannot set (comp)route id=" + routeid);
            return false;
        }

        // aquire locoString and start 'full' speed
        startLocoDelayed();
        state = TripState.ACTIVE;
        return true;
    }

    public void finish() {
        debug("trip " + id + " ends");
        // finish current loco
        stopLoco();

        clearRoutes();

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
        debug("loco-speed=" + speed);
        loco.setSpeed(speed);
        loco.setForward(locoDir == 0);
        loco.setLicht(true);
        //sxi.sendLoco(loco.getLok_adr(), loco.getSpeed(), true, loco.isForward(),  false);  // light = true, horn = false
        SXData.update(loco.getLok_adr(), loco.getSX(), true); // true => send to SXinterface
    }

    private void startLoco() {
        loco.setSpeed(locoSpeed);
        loco.setForward(locoDir == 0);
        loco.setLicht(true);
        //sxi.sendLoco(loco.getLok_adr(), loco.getSpeed(), true, loco.isForward(),  false);  // light = true, horn = false
        SXData.update(loco.getLok_adr(), loco.getSX(), true); // true => send to SXinterface
    }

    private void startLocoDelayed() {
        loco.setSpeed(0);  // licht an, richtige direction, aber noch nicht losfahren
        loco.setForward(locoDir == 0);
        loco.setLicht(true);
        currSpeedPercent = 0;
        SXData.update(loco.getLok_adr(), loco.getSX(), true); // true => send to SXinterface
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(1000),
                ae -> incrLocoSpeed()
        ));
        timeline.setCycleCount(10); // for slow start of loco, increase speed in steps
        timeline.setDelay(Duration.seconds(5)); // then increase speed every second
        timeline.play();
    }

    public void stopLoco() {

        loco.setSpeed(0);
        loco.setForward(locoDir == 0);
        //sxi.sendLoco(loco.getLok_adr(), loco.getSpeed(), true, loco.isForward(),  false);  // light = true, horn = false
        SXData.update(loco.getLok_adr(), loco.getSX(), true); // true => send to SXinterface
    }

    private void finishTripDelayed() {
        if (stopDelay == INVALID_INT) {
            finish();
        }
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(stopDelay),
                ae -> {
                    finish();
                }
        ));
        timeline.play();
    }

    private boolean setRoute(int rID) {
        debug("trip: setRoute =" + rID);

        // todo check if route is free - except for "sens1" sensor
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
        debug("trip: clearRoute =" + routeid);
        for (Route r : allRoutes) {
            if (r.getAdr() == routeid) {
                r.clear();
            }
        }
        for (CompRoute cr : allCompRoutes) {
            if (cr.getAdr() == routeid) {
                cr.clear();
            }
        }

    }

    public boolean checkEndSensor() {
        PanelElement seEnd = PanelElement.getSingleByAddress(sens2);
        if ((state != TripState.ACTIVE) || (seEnd == null)) {
            return false;
        }

        if (seEnd.getState() == STATE_OCCUPIED) {
            // this trip ends

            return true;
        } else {
            return false;
        }
    }

    // to be able to sort the trips by their ID
    @Override
    public int compareTo(Trip o) {
        if (this.id < o.id) {
            return 1;
        } else {
            return -1;
        }
    }

    static Trip get(int index) {
        for (Trip t : allTrips) {
            if (t.id == index) {
                return t;
            }
        }
        return null;  // not found
    }

    public static void auto() {
        for (Trip tr : allTrips) {
            if (tr.state == TripState.ACTIVE) {
                boolean endSensorReached = tr.checkEndSensor();
                if (endSensorReached) {
                    tr.state = TripState.WAITING;  // avaid triggering finish a second time
                    tr.finishTripDelayed();  // incl. finish loco
                }
            }
        }
    }
}
