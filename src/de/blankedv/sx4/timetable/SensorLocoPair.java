/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4.timetable;

import static de.blankedv.sx4.Constants.INVALID_INT;

/**
 *
 * @author mblank
 */
public class SensorLocoPair {

    public int sensor = INVALID_INT;
    public int loco = INVALID_INT;

    @Override
    public String toString() {
        if ((sensor == INVALID_INT) || (loco == INVALID_INT)) {
            return "";
        } else {
            return "" + sensor + " " + loco;
        }
    }
}
