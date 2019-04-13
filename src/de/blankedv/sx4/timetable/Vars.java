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

import java.util.ArrayList;

public class Vars {

    public static final boolean DEBUG = true;
    public static boolean timetableRunning = false;
    public static String panelName = "?";
    
    public static final int AUTO_CLEAR_ROUTE_TIME_SECONDS = 90;  // time after which routes are cleared automatically

    // signals
    static final int STATE_RED = 0;
    static final int STATE_GREEN = 1;
    static final int STATE_YELLOW = 2;
    static final int STATE_YELLOW_FEATHER = 3;
    static final int STATE_SWITCHING = 4;

    static final int RT_INACTIVE = 0;
    static final int RT_ACTIVE = 1;
    
    static final int MAX_START_STOP_DELAY = 100000;

    public static final ArrayList<PanelElement> panelElements = new ArrayList<>();
    public static final ArrayList<Loco> allLocos =  new ArrayList<>();

    public static final ArrayList<Route> allRoutes  = new ArrayList<>();
    public static final ArrayList<CompRoute> allCompRoutes = new ArrayList<>();

    public static final ArrayList<TripsTable> allTripsTables = new ArrayList<>();

}
