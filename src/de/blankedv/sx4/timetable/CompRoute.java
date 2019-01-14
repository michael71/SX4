package de.blankedv.sx4.timetable;

import static com.esotericsoftware.minlog.Log.debug;
import static com.esotericsoftware.minlog.Log.error;
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
    
    private final boolean DEBUG_COMPROUTE = false;

    private long clearRouteTime = Long.MAX_VALUE;  // i.e. => never, if not set
     
    /**
     * constructs a composite route
     *
     *
     */
    public CompRoute(int routeAddr, String sRoutes) {
        super("CR",routeAddr);
        setState(RT_INACTIVE);
        // this string written back to config file.
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
        if (DEBUG_COMPROUTE) debug("creating comproute id=" + routeAddr + " - " + myroutes.size() + " routes in this route.");

    }

    public void clearOffendingRoutes() {
        if (DEBUG_COMPROUTE) debug(" clearing (active) offending Routes");

        for (Route rt : myroutes) {
            rt.clearOffendingRoutes();

        }
    }

    public boolean set() {

        if (DEBUG_COMPROUTE) debug(" setting comproute id=" + getAdr());
  
        clearRouteTime = System.currentTimeMillis() + AUTO_CLEAR_ROUTE_TIME_SECONDS * 1000L;
        
        setState(RT_ACTIVE);
        // check if all routes can be set successfully
        boolean res = true;
        // get current train number from first sensor of first route
        Route start = myroutes.get(0);
        int trainNumber = start.getStartTrainNumber();
        for (Route rt : myroutes) {
            res = rt.set(trainNumber);
            if (res == false) {
                error("cannot set comproute id=" + getAdr() + " because route=" + rt.getAdr() + " cannot be set.");
                return false;  // cannot set comproute.
            }
            // else continue with next route
        }
        return res;
    }

     public static void auto() {
        // check for auto reset of allCompRoutes
        // this function is only needed for the lanbahn-value display, because the individual single routes,
        // which are set by a compound route, are autocleared by the "Route.auto()" function
        for (CompRoute rt : allCompRoutes) {
            if ( (( System.currentTimeMillis() - rt.clearRouteTime) > 0 ) 
                    && (rt.getState() == RT_ACTIVE) ) {
                rt.setState(RT_INACTIVE);
             }

        }

    }
     
     public static CompRoute getFromAddress(int a) {
        for (CompRoute cr : allCompRoutes) {
            if (cr.getAdr() == a) return cr;
        }
        return null;
    }
}
