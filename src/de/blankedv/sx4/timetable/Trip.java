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
import static de.blankedv.sx4.Constants.*;
import de.blankedv.sx4.SXData;
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
    boolean active = false;
    Loco loco = null;

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
            loco = new Loco(locoAddr, locoDir, locoSpeed);

        } catch (NumberFormatException e) {
            return false;

        }
        return true;
    }

    public String start() {
        if (SXData.getActualPower() == false) {
            return "ERROR: keine Gleisspannung, kann Fahrt nicht starten!";
        }
        setRoutes();

        // aquire locoString and start 'full' speed
        startLocoDelayed();
        active = true;
        return "OK";
    }
    
    public void stop() {
    
        // stop current loco
        stopLoco();
        active = false;
        clearRoutes();

    }

    private void startLoco() {

        loco.setSpeed(locoSpeed);
        loco.setForward(locoDir == 0);
        loco.setLicht(true);
        //sxi.sendLoco(loco.getLok_adr(), loco.getSpeed(), true, loco.isForward(),  false);  // light = true, horn = false
        SXData.update(loco.getLok_adr(), loco.getSX(), true); // true => send to SXinterface
    }
    
    private void startLocoDelayed() {
        final int startDelay = 5000; 
        loco.setSpeed(0);  // licht an, richtige direction, aber noch nicht losfahren
        loco.setForward(locoDir == 0);
        loco.setLicht(true);
        SXData.update(loco.getLok_adr(), loco.getSX(), true); // true => send to SXinterface
        Timeline timeline = new Timeline(new KeyFrame(
               Duration.millis(startDelay),
                      ae -> startLoco() ));
        timeline.play();
    }
    
    public void stopLoco() {

        loco.setSpeed(0);
        loco.setForward(locoDir == 0);
        //sxi.sendLoco(loco.getLok_adr(), loco.getSpeed(), true, loco.isForward(),  false);  // light = true, horn = false
        SXData.update(loco.getLok_adr(), loco.getSX(), true); // true => send to SXinterface
    }
    
    private void stopLocoDelayed() {
        if (stopDelay == INVALID_INT) stopLoco();
        Timeline timeline = new Timeline(new KeyFrame(
               Duration.millis(stopDelay),
                      ae -> stopLoco() ));
        timeline.play();
    }

    private void setRoutes() {
        debug("trip: setRoute =" + routeid);

        for (Route r : allRoutes) {
            if (r.getAdr() == routeid) {
                r.set();
            }
        }
        for (CompRoute cr : allCompRoutes) {
            if (cr.getAdr() == routeid) {
                cr.set();
            }
        }

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
        if ((active == false) || (seEnd == null)) {
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
            if (tr.active) {
                boolean end = tr.checkEndSensor();
                if (end) {
                    tr.stopLocoDelayed();
                    tr.active = false;
                    debug("trip "+tr.id+" ends");
                }
            }
        }
    }
}
