/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.blankedv.sx4.timetable;

import de.blankedv.sx4.SXUtils;

/**
 *
 * @author mblank
 */
public class Loco {
    private boolean forward = true;
    private int speed = 0;
    private boolean licht = false;
    private boolean horn = false;
    private int lok_adr = 1;
 
    public Loco() {
    }

    public Loco(int locoAddr, int locoDir, int locoSpeed) {
        if (SXUtils.isValidSXAddress(locoAddr)) {
            lok_adr = locoAddr;
        } 
        if (locoDir == 0) {
            forward = true;
        } else {
            forward = false;
        }
        if (locoSpeed > 31) {
            speed = 31;
        } else {
            if (locoSpeed >= 0) {
                speed = locoSpeed;
            }
        }
    }
    public boolean isForward() {
        return forward;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public boolean isHorn() {
        return horn;
    }

    public void setHorn(boolean horn) {
        this.horn = horn;
    }

    public boolean isLicht() {
        return licht;
    }

    public void setLicht(boolean licht) {
        this.licht = licht;
    }

    public int getLok_adr() {
        return lok_adr;
    }

    public void setLok_adr(int lok_adr) {
        this.lok_adr = lok_adr;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        if (speed <0 ) speed = 0;
        if (speed >31) speed = 31;
        this.speed = speed;
    }
    
    public int getSX() {
        int data = speed;
        if (!forward) data += 32;
        if (licht) data += 64;
        if (horn) data += 128;
        return data;
    }

}
