package de.blankedv.sx4.timetable;

import static com.esotericsoftware.minlog.Log.debug;
import static com.esotericsoftware.minlog.Log.error;
import static de.blankedv.sx4.Constants.DEBUG_COMPROUTE;
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


    private long clearRouteTime = Long.MAX_VALUE;  // i.e. => never, if not set

    /**
     * constructs a composite route
     *
     *
     */
    public CompRoute(int routeAddr, String sRoutes) {
        super("CR", routeAddr);
        setState(RT_INACTIVE);
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
        if (DEBUG_COMPROUTE) {
            debug("creating comproute id=" + routeAddr + " - " + myroutes.size() + " routes in this route.");
        }

    }

    public void clearOffendingRoutes() {
        if (DEBUG_COMPROUTE) {
            debug(" clearing (active) offending Routes");
        }

        for (Route rt : myroutes) {
            rt.clearOffendingRoutes();

        }
    }

    public boolean set() {

        if (DEBUG_COMPROUTE) {
            debug(" setting comproute id=" + getAdr());
        }

        clearRouteTime = System.currentTimeMillis() + AUTO_CLEAR_ROUTE_TIME_SECONDS * 1000L;

        // check if all routes can be set successfully
        boolean res = true;
        // get current train number from first sensor of first route
        Route start = myroutes.get(0);
        int trainNumber = start.getStartTrainNumber();

        if (trainNumber == 0) {
            error("cannot set comproute id=" + getAdr() + " because no train on start sensor=" + start.getStartSensor().getAdr());
            return false;  // cannot set comproute.
        }

        // FIRST check, if all routes of this compound route are free
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

        // SECOND: add train number info - and get last sensor last route (=endSensor)
        for (Route rt : myroutes) {
            res = rt.set(trainNumber, true);
            endSensor = rt.getEndSensor();
            if (res == false) {
                error("cannot set comproute id=" + getAdr() + " because route=" + rt.getAdr() + " cannot be set.");
                return false;  // cannot set comproute.
            }
            // else continue with next route
        }

        if (endSensor.getState() != STATE_FREE) {
            error("SHOULD NOT HAPPEN: cannot set comproute id=" + getAdr() + " because train already on END sensor=" + start.getStartSensor().getAdr());
            return false;  // cannot set comproute.
        }
        if (res == true) {
            // set active only if the comprising routes could be set with success
            if (DEBUG_COMPROUTE) {
                debug(" setting comproute id=" + getAdr() + " successful, endSensor="+ endSensor.getAdr());
            }
            setState(RT_ACTIVE);
        }
        return res;
    }

    public static void auto() {
        // check for auto reset of allCompRoutes
        // this function is only needed for the lanbahn-value display, because the individual single routes,
        // which are set by a compound route, are autocleared by the "Route.auto()" function
        for (CompRoute comp : allCompRoutes) {
            if (comp.getState() == RT_ACTIVE) {
                //debug("comp auto id=" + comp.getAdr());
                if ((System.currentTimeMillis() - comp.clearRouteTime) > 0) {
                    comp.setState(RT_INACTIVE);
                }
                // check for route end sensor - if it gets occupied (train reached end of route), rt will be cleared
                if ((comp.endSensor != null) && (comp.endSensor.getState() == STATE_OCCUPIED)) {
                    debug("end sensor" + comp.endSensor.getAdr() + " occupied => comp,route#" + comp.getAdr() + " cleared");
                    comp.setState(RT_INACTIVE);
                    for (Route rt : comp.myroutes) {
                        rt.clearIn10Seconds();
                    }
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
