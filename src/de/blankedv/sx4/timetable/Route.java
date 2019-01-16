package de.blankedv.sx4.timetable;

import static com.esotericsoftware.minlog.Log.*;
import static de.blankedv.sx4.Constants.*;
import de.blankedv.sx4.LanbahnData;
import de.blankedv.sx4.SXData;
import de.blankedv.sx4.SXUtils;
import static de.blankedv.sx4.timetable.Vars.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Class Route stores a complete route, which contains sensors, signals and
 * turnouts. Offending allRoutes are calculated automatically (defined as all
 * allRoutes which also set one of the turnouts). In addition offending
 * allRoutes can also be defined in the config file (needed for crossing
 * allRoutes, which cannot be found automatically)
 *
 * adapted from lanbahnpanel (android software)
 *
 * @author mblank
 *
 */
public class Route extends PanelElement {

    String routeString = "";
    String sensorsString = "";
    String offendingString = ""; // comma separated list of adr's of offending
    // allRoutes

    // sensors turnout activate for the display of this route
    private ArrayList<PanelElement> rtSensors = new ArrayList<>();
    public PanelElement endSensor = null;   // used for auto- "route completed" detection
    // NOT used in compound routes.

    // signals of this route
    private ArrayList<RouteSignal> rtSignals = new ArrayList<>();

    // turnouts of this route
    private ArrayList<RouteTurnout> rtTurnouts = new ArrayList<>();

    // offending allRoutes
    private ArrayList<Route> rtOffending = new ArrayList<>();

    private long clearRouteTime = Long.MAX_VALUE;  // i.e. => never, if not set

    /**
     * constructs a route
     *
     * @param routeAddr unique identifier (int)
     * @param route string for route setting like "770,1;720,2"
     * @param allSensors string for sensors like "2000,2001,2002"
     * @param offending string with offending allRoutes, separated by comma
     */
    public Route(int routeAddr, String route, String allSensors,
            String offending) {

        super("RT", routeAddr);
        this.setState(RT_INACTIVE);
        // these strings are written back to config file.
        this.routeString = route;
        this.sensorsString = allSensors;
        this.offendingString = offending;

        // route = "750,1;751,2" => set 750 turnout 1 and 751 turnout value 2
        String[] routeElements = route.split(";");
        for (int i = 0; i < routeElements.length; i++) {
            String reInfo[] = routeElements[i].split(",");

            PanelElement pe = PanelElement.getSingleByAddress(Integer.parseInt(reInfo[0]));

            // if this is a signal, then add to my signal list "rtSignals"
            if (pe != null) {
                if (pe.isSignal()) {
                    if (reInfo.length == 3) {  // route signal with dependency
                        rtSignals.add(new RouteSignal(pe,
                                Integer.parseInt(reInfo[1]),
                                Integer.parseInt(reInfo[2])));
                        //  debug("RT, add sig(dep) " + pe.getAdr());
                    } else {
                        rtSignals.add(new RouteSignal(pe, Integer
                                .parseInt(reInfo[1])));
                        //  debug("RT, add sig  " + pe.getAdr());
                    }

                } else if (pe.isTurnout()) {
                    rtTurnouts.add(new RouteTurnout(pe,
                            Integer.parseInt(reInfo[1])));
                    //  debug("RT, add turnout " + pe.getAdr());
                }
            }
        }

        // format for sensors: just a list of addresses, seperated by comma ","
        String[] sensorAddresses = allSensors.split(",");
        for (int i = 0; i < sensorAddresses.length; i++) {
            // add the matching elements turnout sensors list
            for (PanelElement pe : panelElements) {
                if (pe.isSensor()) {
                    if (pe.getAdr() == Integer.parseInt(sensorAddresses[i])) {
                        rtSensors.add(pe);
                        if (CFG_DEBUG) {
                            debug("RT, add sensor " + pe.getAdr());
                        }
                    }
                }
            }
        }
        if (CFG_DEBUG) {
            debug("creating route id/adr=" + this.getAdr() + " - " + rtSignals.size() + " signals/" + rtTurnouts.size() + " turnouts/" + rtSensors.size() + " sensors");
        }

        String[] offRoutes = offendingString.split(",");
        for (int i = 0; i < offRoutes.length; i++) {
            for (Route rt : allRoutes) {
                try {
                    int offID = Integer.parseInt(offRoutes[i]);
                    if ((rt.getAdr() == offID) && (rt.getState() == RT_ACTIVE)) {
                        rtOffending.add(rt);
                        // (debug)("RT, add off. rt " + rt.getAdr());
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        //	if (DEBUG)
        //		Log.d(TAG, rtOffending.size() + " offending allRoutes in config");
    }

    public void clear() {
        clearRouteTime = Long.MAX_VALUE;  // i.e. => never, if not set
        // automatically
        debug("clearing route id=" + this.getAdr());

        // deactivate sensors
        for (PanelElement se : rtSensors) {
            se.setInRoute(false);
            // reset trainNumber data for all but last sensor
            if (se != rtSensors.get(rtSensors.size() - 1)) {
                se.setTrain(0);
            }
            LanbahnData.update(se.getSecondaryAdr(), 0);
        }

        Set<Integer> sxAddressesToUpdate = new HashSet<>();
        // set signals turnout red
        for (RouteSignal rs : rtSignals) {
            rs.signal.setStateAndUpdateSXData(STATE_RED);
            sxAddressesToUpdate.add(rs.signal.getAdr() / 10);
        }

        for (int sxaddr : sxAddressesToUpdate) {
            if (SXUtils.isValidSXAddress(sxaddr)) {
                SXData.update(sxaddr, SXData.get(sxaddr), true);  // true => write to Interface
            }
        }
        // TODO unlock turnouts
        /*
		 * for (RouteTurnout to : rtTurnouts) { 
		 *     String cmd = "U " + to.turnout.adr;
		 *     sendQ.add(cmd); 
		 * }
         */
        // notify that route was cleared
        this.setState(RT_INACTIVE);

    }

    public void clearOffendingRoutes() {
        /* disabled
        debug(" clearing (active) offending Routes");

        String[] offRoutes = offendingString.split(",");
        for (int i = 0; i < offRoutes.length; i++) {
            for (Route rt : allRoutes) {
                try {
                    int offID = Integer.parseInt(offRoutes[i]);
                    if ((rt.getAdr() == offID) && (rt.getState() == RT_ACTIVE)) {
                        rt.clear();
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
         */
    }

    public boolean offendingRouteActive() {
        debug(" checking for (active) offending Routes");
        String[] offRoutes = offendingString.split(",");
        for (int i = 0; i < offRoutes.length; i++) {
            for (Route rt : allRoutes) {
                try {
                    int offID = Integer.parseInt(offRoutes[i]);
                    if ((rt.getAdr() == offID) && (rt.getState() == RT_ACTIVE)) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        return false;
    }

    public void clearIn10Seconds() {
        debug("rt# " + getAdr() + " will be cleared in 10 sec");
        clearRouteTime = System.currentTimeMillis() + 10 * 1000;
    }

    /**
     * default set for single route
     *
     * @return
     */
    public boolean set() {
        return set(INVALID_INT, false);
    }

    /**
     * set function if the route is part of a compound route
     *
     * @param startTrain
     * @return
     */
    public boolean set(int startTrain, boolean partOfCompRoute) {

        if (startTrain == 0) {
            error("invalid startTrain parameter in set route id=" + this.getAdr());
            return false;
        } else if (startTrain == INVALID_INT) {
            // get from occupation of first sensor
            startTrain = getStartTrainNumber();
            if (startTrain == 0) {
                error("cannot set route id=" + getAdr() + " because no train on start sensor=" + getStartSensor().getAdr());
                return false;  // cannot set route.
            }
        }

        clearRouteTime = System.currentTimeMillis() + AUTO_CLEAR_ROUTE_TIME_SECONDS * 1000L;

        if (offendingRouteActive()) {
            debug(" offending route active");
            return false;
        }

        if (!isFreeExceptStart()) {
            error("cannot set route id=" + getAdr() + " because there is a train on route!");
            return false;
        }

        for (PanelElement se : rtSensors) {
            se.setInRoute(true);
            LanbahnData.update(se.getSecondaryAdr(), 1);  // st to "inroute
            // only virtual, no matching real SX address
        }
        debug(" setting route id=" + this.getAdr() + " startTrain=" + startTrain);

        // automatically
        clearOffendingRoutes();

        // activate sensors, set "IN_ROUTE" (this is stored in as "LanbahnData"
        // in secondary address of the sensor
        for (PanelElement se : rtSensors) {
            se.setInRoute(true);
            if (se.getState() == STATE_FREE) {
                // do not override if not free !              
                se.setTrain(startTrain);
            }
            LanbahnData.update(se.getSecondaryAdr(), 1);  // st to "inroute
            // only virtual, no matching real SX address
        }

        // add sxadr values to a set, then update all these via RS232 only once
        Set<Integer> sxAddressesToUpdate = new HashSet<>();
        // set signals
        for (RouteSignal rs : rtSignals) {
            int d = rs.dynamicValueToSetForRoute();
            /* UNNECESSARY ??? SXAddrAndBits sxab = SXUtils.lbAddr2SX(rs.signal.getAdr());
            if (d == 0) {   // TODO multi-aspect - only red and green are used at the moment
                SXUtils.clearBitSxData(sxab.sxAddr, sxab.sxBit);
            } else {
                SXUtils.clearBitSxData(sxab.sxAddr, sxab.sxBit);
            } */
            rs.signal.setStateAndUpdateSXData(d);
            sxAddressesToUpdate.add(rs.signal.getAdr() / 10);
        }
        // set and // TODO lock turnouts
        for (RouteTurnout rtt : rtTurnouts) {
            int d = rtt.valueToSetForRoute;   // can be only 1 or 0
            /* UNNECESSARY ??? SXAddrAndBits sxab = SXUtils.lbAddr2SX(rtt.turnout.getAdr());
             if (d == 0) {  
                SXUtils.clearBitSxData(sxab.sxAddr, sxab.sxBit);
            } else {
                SXUtils.clearBitSxData(sxab.sxAddr, sxab.sxBit);
            } */
            rtt.turnout.setStateAndUpdateSXData(d);
            // debug("RT, set turn= " + rtt.turnout.getAdr() + " state=" + d);
            sxAddressesToUpdate.add(rtt.turnout.getAdr() / 10);

        }
        for (int sxaddr : sxAddressesToUpdate) {
            if (SXUtils.isValidSXAddress(sxaddr)) {
                SXData.update(sxaddr, SXData.get(sxaddr), true);  // true => write to Interface
            }
        }
        if (!partOfCompRoute) {  // for a compound route, the startSensor of a sub-route can be FREE
            // therefor this checking is only done if we are not checking a "single" route
            PanelElement startSensor = getStartSensor();
            if (startSensor.getState() == STATE_FREE) {
                error("cannot set route id=" + getAdr() + " because train is missing on start sensor=" + startSensor.getAdr());
                return false;  // cannot set comproute.
            }
        }
        // the endsensor should always be free - both as part of a compound route or 
        endSensor = getEndSensor();
        if (endSensor.getState() != STATE_FREE) {
            error("cannot set route id=" + getAdr() + " because train is already on END sensor=" + endSensor.getAdr());
            return false;  // cannot set comproute.
        }

        this.setState(RT_ACTIVE);
        return true;
    }

    public boolean isActive() {
        return (this.getState() == RT_ACTIVE);
    }

    public int getStartTrainNumber() {
        return rtSensors.get(0).getTrain();   // train occupation of starting sensor
    }

    public PanelElement getStartSensor() {
        return rtSensors.get(0);   // start (first) sensor
    }

    public PanelElement getEndSensor() {
        int si = rtSensors.size();
        if (si > 1) {
            return rtSensors.get(si - 1);  // last sensor
        } else {
            return rtSensors.get(0);
        }
    }

    public boolean isFreeExceptStart() {
        // check if route is FREE (except for startSensor)
        for (int i = 1; i < rtSensors.size(); i++) {
            if (rtSensors.get(i).getState() != STATE_FREE) {
                return false;
            }
        }
        return true;
    }

    public boolean isFree() {
        //check if route is FREE
        for (int i = 0; i < rtSensors.size(); i++) {
            if (rtSensors.get(i).getState() != STATE_FREE) {
                return false;
            }
        }
        return true;
    }

    protected class RouteSignal {

        PanelElement signal;
        private int valueToSetForRoute;
        private int depFrom;

        RouteSignal(PanelElement se, int value) {
            signal = se;
            valueToSetForRoute = value;
            depFrom = INVALID_INT;
        }

        RouteSignal(PanelElement se, int value, int dependentFrom) {
            signal = se;
            valueToSetForRoute = value;
            depFrom = dependentFrom;
        }

        int dynamicValueToSetForRoute() {
            // set standard value if not green
            if ((depFrom == INVALID_INT) || (valueToSetForRoute != STATE_GREEN)) {
                return valueToSetForRoute;
            } else {
                // if standard-value == GREEN then check the other signal, which
                // this signal state depends on (can only be a ONE other signal)
                PanelElement depPe = PanelElement.getSingleByAddress(depFrom);
                if (depPe.getState() == STATE_RED) {
                    // if other signal red, then set to yellow
                    return STATE_YELLOW;
                } else {
                    return valueToSetForRoute;
                }

            }
        }
    }

    protected void updateDependencies() {
        // update signals which have a dependency from another signal
        // set signals
        for (RouteSignal rs : rtSignals) {
            if (rs.depFrom != INVALID_INT) {
                if (rs.signal.getState() != rs.dynamicValueToSetForRoute()) {
                    rs.signal.setState(rs.dynamicValueToSetForRoute());
                    //LbUtils.updateLanbahnData(rs.signal.adr, rs.signal.state);
                }
            }
        }

    }

    protected class RouteTurnout {

        PanelElement turnout;
        int valueToSetForRoute;

        RouteTurnout(PanelElement te, int value) {
            turnout = te;
            valueToSetForRoute = value;
        }
    }

    /**
     * check for auto reset of allRoutes
     *
     */
    public static void auto() {

        // debug("checking route auto clear");
        for (Route rt : allRoutes) {
            if (rt.getState() == RT_ACTIVE) {  // check only active routes
                if ((System.currentTimeMillis() - rt.clearRouteTime) > 0) {
                    rt.clear();
                }
                // update dependencies
                rt.updateDependencies();
                // check for route end sensor - if it gets occupied (train reached end of route), rt will be cleared
                if ((rt.endSensor != null) && (rt.endSensor.getState() == STATE_OCCUPIED)) {
                    debug("end sensor" + rt.endSensor.getAdr() + " occupied =>  route#" + rt.getAdr() + " cleared");
                    rt.clear();
                }
            }
        }

    }

    public void addOffending(Route rt2) {
        // check if not already contained in offending string
        if (!rtOffending.contains(rt2)) {
            rtOffending.add(rt2);
        }
    }

    public String getOffendingString() {

        StringBuilder sb = new StringBuilder("");
        for (Route r : rtOffending) {
            if (sb.length() == 0) {
                sb.append(r.getAdr());
            } else {
                sb.append(",");
                sb.append(r.getAdr());
            }
        }
        /*		if (sb.length() == 0)
			Log.d(TAG, "route adr=" + adr + " has no offending allRoutes.");
		else
			Log.d(TAG, "route adr=" + adr + " has offending allRoutes with ids="
					+ sb.toString()); */
        return sb.toString();

    }

    public static void calcOffendingRoutes() {
        for (Route rt : allRoutes) {
            for (RouteTurnout t : rt.rtTurnouts) {
                // iterate over all turnouts of rt and check, if another route
                // activates the same turnout to a different position 
                for (Route rt2 : allRoutes) {
                    if (rt.getAdr() != rt2.getAdr()) {
                        for (RouteTurnout t2 : rt2.rtTurnouts) {
                            if ((t.turnout.getAdr() == t2.turnout.getAdr())
                                    && (t.valueToSetForRoute != t2.valueToSetForRoute)) {
                                rt.addOffending(rt2);
                                break;
                            }

                        }
                    }
                }
            }
            rt.offendingString = rt.getOffendingString();
        }

    }

    public static Route getFromAddress(int a) {
        for (Route r : allRoutes) {
            if (r.getAdr() == a) {
                return r;
            }
        }
        return null;
    }
}
