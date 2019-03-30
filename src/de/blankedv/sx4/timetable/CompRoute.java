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
import de.blankedv.sx4.LanbahnData;
import static de.blankedv.sx4.timetable.Vars.*;

import java.util.ArrayList;


/**
 * composite route, i.e. a list of allRoutes which build a new "compound" route
 * is only a helper for ease of use, no more functionality than the "simple"
 * Route (i.e. a CompRoute contains a "list of Routes")
 *
 * @author mblank
 *
 */
public class CompRoute extends PanelElement {

    String routesString = ""; // identical to config string

    // route is comprised of a list of allRoutes
    private ArrayList<Route> myroutes = new ArrayList<>();
    public PanelElement endSensor = null;

    private boolean automaticFlag = false;

    private long clearRouteTime = Long.MAX_VALUE;  // i.e. => never, if not set

    /**
     * constructs a composite route
     *
     *
     * @param routeAddr
     * @param sRoutes
     */
    public CompRoute(int routeAddr, String sRoutes) {
        super("CR", routeAddr);
        state = RT_INACTIVE;
        // this string is written back to config file.
        this.routesString = sRoutes;

        // allRoutes = "12,13": these allRoutes need to be activated.
        String[] iID = routesString.split(",");
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < iID.length; i++) {
            int routeID = Integer.parseInt(iID[i]);
            for (Route rt : allRoutes) {
                try {
                    if (rt.getAdr() == routeID) {
                        myroutes.add(rt);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        //debug("creating comproute id=" + routeAddr + " - " + myroutes.size() + " routes in this route.");


    }

    public void clearOffendingRoutes() {
        debug(" clearing (active) offending Routes");
        for (Route rt : myroutes) {
            rt.clearOffendingRoutes();

        }
    }

    public void clear() {
        for (Route rt : myroutes) {
            rt.clear();
        }
        setState(RT_INACTIVE);
    }

    @Override
    public int setState(int st) {
        int result = super.setState(st);
        LanbahnData.update(getAdr(), result);
        return result;
    }

    

    public boolean isLocked() {
        boolean locked = false;
        // check, if all routes of this compound route are unlocked
        for (Route rt : myroutes) {
            if (rt.isLocked()) {
                locked = true;
            }
        }
        debug(" comproute id=" + getAdr() + " is locked");
        
        return locked;
    }

    public boolean set() {
        // get current train number from first sensor of first route
        Route start = myroutes.get(0);
        return set(false, start.getStartTrainNumber());
    }
    
    public boolean set(boolean automatic, int tripTrainNumber) {

        automaticFlag = automatic;
        clearRouteTime = Long.MAX_VALUE;   // set only if route could be set successfully


            debug(" setting comproute id=" + getAdr() + "auto-mode=" + automatic);
    
        // check if all (sub-)routes can be set successfully
        boolean res = true;

        // get current train number from first sensor of first route
        Route start = myroutes.get(0);
        int trainNumber = start.getStartTrainNumber();
        if (trainNumber != tripTrainNumber) {
            // can only happen in automatic (trip/timetable) mode
            error("comproute id=" + getAdr() + " - wrong train=" + trainNumber + " on start sensor=" + start.getStartSensor().getAdr());
            return false;
        }

        if (trainNumber == 0) {
            debug("comproute id=" + getAdr() + " - no train on start sensor=" + start.getStartSensor().getAdr());
        } else {
            debug("comproute id=" + getAdr() + " - train " + trainNumber + " on start sensor=" + start.getStartSensor().getAdr());
        }

        // if automatic: SECOND check, if all routes of this compound route are free
        if (automatic) {
            for (Route rt : myroutes) {
                // check if all routes are free
                if (rt == start) {
                    if (!rt.isFreeExceptStart()) {
                        error("cannot set comproute id=" + getAdr() + " because route=" + rt.getAdr() + " is not free");
                        return false;
                    }
                } else {
                    if (!rt.isFree()) {
                        error("cannot set comproute id=" + getAdr() + " because route=" + rt.getAdr() + " is not free");
                        return false;
                    }
                }
            }
            
        }

        // SECOND: add train number info - and get last sensor last route (=endSensor)
        for (Route rt : myroutes) {
            res = rt.set(automatic, trainNumber);
            endSensor = rt.getEndSensor();
            if (res == false) {
                error("cannot set comproute id=" + getAdr() + " because route=" + rt.getAdr() + " cannot be set.");
                return false;  // cannot set comproute.
            }
            // else continue with next route
        }

        if (automatic && (endSensor.getState() != STATE_FREE)) {
            // check only in automatic mode
            error("SHOULD NOT HAPPEN: cannot set comproute id=" + getAdr() + " because train already on END sensor=" + start.getStartSensor().getAdr());
            return false;  // cannot set comproute.
        }

        if (res == true) {
            // set active only if the comprising routes could be set with success
            debug(" setting comproute id=" + getAdr() + " successful, endSensor=" + endSensor.getAdr());

            if (!automatic) {
                clearRouteTime = System.currentTimeMillis() + AUTO_CLEAR_ROUTE_TIME_SECONDS * 1000L;
            }
            setState(RT_ACTIVE);
        }
        return res;
    }

    public boolean isFreeExceptStart() {
        if (myroutes.size() == 0) {
            return true;  // should not happen
        }
        // for first route, check if everything is free EXCEPT-start
        if (!myroutes.get(0).isFreeExceptStart()) {
            return false;
        }

        for (int i = 1; i < myroutes.size(); i++) {
            // for other routes, check if all sensors are free
            if (!myroutes.get(i).isFree()) {
                return false;
            }
        }
        return true;
    }

    public static void auto() {
        // check for auto reset of allCompRoutes
        // this function is only needed for the lanbahn-value display, because the individual single routes,
        // which are set by a compound route, are autocleared by the "Route.auto()" function
        for (CompRoute comp : allCompRoutes) {
            if (comp.getState() == RT_ACTIVE) {
                //debug("comp auto id=" + comp.getAdr());

                if (comp.automaticFlag) {
                    // check for route end sensor - if it gets occupied (train reached end of route), rt will be cleared
                    if ((comp.endSensor != null) && (comp.endSensor.getState() == STATE_OCCUPIED)) {
                        debug("end sensor " + comp.endSensor.getAdr() + " occupied => comp,route#" + comp.getAdr() + " cleared");
                        comp.setState(RT_INACTIVE);
                        for (Route rt : comp.myroutes) {
                            rt.clearIn3Seconds();
                        }
                    }
                } 
                
                    if ((System.currentTimeMillis() - comp.clearRouteTime) > 0) {
                        comp.setState(RT_INACTIVE);
                    }
               
                    
            }
        }

    }

    public static CompRoute getFromAddress(int a) {
        for (CompRoute cr : allCompRoutes) {
            if (cr.getAdr() == a) {
                return cr;
            }
        }
        return null;
    }
}
