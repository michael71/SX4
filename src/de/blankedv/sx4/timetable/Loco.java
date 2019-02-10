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

import de.blankedv.sx4.SXUtils;
import static de.blankedv.sx4.timetable.Vars.allLocos;

/** Loco class used for storing loco info
 * 
 * Locos can be created 
 * 1. from the <locolist> tag in the config file
 * 2. adhoc with the LOCO command via the net
 * 3. from the <trip> tag in the config file
 *
 * @author mblank
 */

public class Loco {
    // example from xml file
    //<loco adr="97" name="SchoenBB" mass="2" vmax="120" />
    private boolean forward = true;
    private int speed = 0;
    private boolean licht = false;
    private boolean horn = false;
    private int addr = 1;
    private String name ="";
    private int mass = 3;
    private int vmax = 160;
 
    public Loco() {
    }

    public Loco(int locoAddr, int locoDir, int locoSpeed) {
        if (SXUtils.isValidSXAddress(locoAddr)) {
            addr = locoAddr;
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
    
    public Loco(int locoAddr) {
        if (SXUtils.isValidSXAddress(locoAddr)) {
            addr = locoAddr;
        } 
           forward = true;
            speed = 0;
        mass = 3;
        vmax = 160;
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

    public int getAddr() {
        return addr;
    }

    public void setAddr(int addr) {
        this.addr = addr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMass() {
        return mass;
    }

    public void setMass(int mass) {
        if ((mass >= 1) && (mass <=5)) {
        this.mass = mass;
        } else {
            mass = 3;
        }
    }

    public int getVmax() {
        return vmax;
    }

    public void setVmax(int vmax) {
         if (( vmax >= 30) && ( vmax <=300)) {
            this.vmax = vmax;
         } else {
            this.vmax = 160;
         }
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

    public static Loco getByAddress(int a) {
        for (Loco lo : allLocos) {
            if (lo.addr == a) {
                return lo;
            }
        }
        return null;
    }
}
