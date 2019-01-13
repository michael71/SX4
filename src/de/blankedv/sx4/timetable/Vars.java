/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4.timetable;

import java.util.ArrayList;

public class Vars {

    public static final boolean DEBUG = true;
    public static boolean timetableRunning = false;
    
    public static final int AUTO_CLEAR_ROUTE_TIME_SECONDS = 90;  // time after which routes are cleared automatically

    // signals
    static final int STATE_RED = 0;
    static final int STATE_GREEN = 1;
    static final int STATE_YELLOW = 2;
    static final int STATE_YELLOW_FEATHER = 3;
    static final int STATE_SWITCHING = 4;

    static final int RT_INACTIVE = 0;
    static final int RT_ACTIVE = 1;

    public static ArrayList<PanelElement> panelElements = new ArrayList<>();

    public static ArrayList<Trip> allTrips = new ArrayList<>();   // all Locos we have heard of (via sxnet)
    public static ArrayList<Timetable> allTimetables = new ArrayList<>();

    public static ArrayList<Route> allRoutes = new ArrayList<>();   // TODO eleminate
    public static ArrayList<CompRoute> allCompRoutes = new ArrayList<>();  // TODO eleminate


}
