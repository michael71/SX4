/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4.timetable;

import static de.blankedv.sx4.Constants.*;
import java.util.ArrayList;

/**
 *
 * @author mblank
 */
public class Timetable {

    int id = INVALID_INT;
    ArrayList<Integer> startTime = new ArrayList<>();
    ArrayList<Integer> tripIds = new ArrayList<>();
    int currentTripId = 0;
    Trip cTrip = null;

    int nextTimetable = INVALID_INT;

    boolean active = false;

    Timetable(int id, String time, String trip, String next) {
        // parse "time" to "startTime" array

        String[] sTime = time.split(",");
        String[] sTrip = trip.split(",");
        startTime = new ArrayList<>();

        if (sTime.length != sTrip.length) {
            System.out.println("ERROR: number of start times in timetable does not match number of trips!");
            id = INVALID_INT;
        } else {
            this.id = id;
            for (String s2 : sTime) {
                int t = INVALID_INT;
                try {
                    t = Integer.parseInt(s2);
                } catch (NumberFormatException ex) {
                }
                startTime.add(t);
            }

            for (String s2 : sTrip) {
                int t = INVALID_INT;
                try {
                    t = Integer.parseInt(s2);
                } catch (NumberFormatException ex) {
                }
                tripIds.add(t);
            }
        }
    }

    public boolean start() {
        // start a new timetable
        // TODO Fixed = timetable0 !!

        currentTripId = 0;
        // start first trip (index 0)
        cTrip = Trip.get(tripIds.get(currentTripId));

        if (cTrip == null) {
            System.out.println("ERROR in Timetable - no trip found for id=" + currentTripId);
            return false;
        }

        return startNewTrip(cTrip);
    }

    public boolean startNewTrip(Trip t) {
        // check if start sensor is occupied and endsensor is free
        // TODO check if complete route is free and set route
        
        // set route(s)
        
        int start =  PanelElement.getSingleByAddress(t.sens1).getState() & 0x01;   // get "occupied" bit
        int end = PanelElement.getSingleByAddress(t.sens2).getState() & 0x01;   // get "occupied" bit

        if ( (start != 0) && (end == 0) ) {
            System.out.println("start sensor occ and end sensor free, we can start the trip");
            t.start();
            return true;

        } else {
            if (start == 0) {
                System.out.println("start sensor free, we CANNOT start the trip");
            }
            if (end != 0) {
                System.out.println("stop sensor is not free, we CANNOT start the trip");
            }
            return false;
        }

    }

    public boolean advanceToNextTrip() {
        currentTripId++;

        // get trip 
        try {
        cTrip = Trip.get(tripIds.get(currentTripId)); 
        } catch (IndexOutOfBoundsException ex) {
            cTrip = null;
        }
        if (cTrip == null) {
            System.out.println("ERROR in Timetable - no trip found for id=" + currentTripId);
            return false;
        } 

        return startNewTrip(cTrip);
    }
}
