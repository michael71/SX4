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

package de.blankedv.sx4;

/**
 *
 * @author mblank
 */
public class Constants {

    /**
     * {@value #VERSION} = program version, displayed in HELP window
     */
    public static final String VERSION = "SX4 - rev1.24 - 13 Mar 2019";
    
    // switch one more debugging?
    // DEBUG can be set via args - and therefor is a variable
    public static final boolean CFG_DEBUG = false;   
    public static final boolean DEBUG_COMPROUTE = false;

    public static final int STATUS_CONNECTED = 1;
    public static final int STATUS_NOT_CONNECTED = 0;

    public static int INVALID_INT = -1;
    public static int INVALID_TRAIN = 0;

    /**
     * {@value #SX_MIN} = minimale SX adresse angezeigt im Monitor
     */
    public static final int SXMIN = 0;
    /**
     * maximale SX adresse (SX0), maximale adr angezeigt im Monitor
     */
    public static final int SXMAX = 111;
    /**
     * {@value #SX_MAX_USED} = maximale Adresse für normale Benutzung (Loco,
     * Weiche, Signal) higher addresses reserved for command stations/loco
     * programming
     */
    public static final int SXMAX_USED = 106;

    /**
     * {@value #LBMIN} =minimum lanbahn channel number
     */
    public static final int LBMIN = 10;
    public static final int LBPURE = (SXMAX + 1) * 10; // lowest pure lanbahn addres
    /**
     * {@value #LBMAX} =maximum lanbahn channel number
     */
    public static final int LBMAX = 9999;
    /**
     * {@value #LBDATAMIN} =minimum lanbahn data value
     */
    public static final int LBDATAMIN = 0;
    /**
     * {@value #LBDATAMAX} =maximum lanbahn data value (== 2 bits in SX world)
     */
    public static final int LBDATAMAX = 3;  // 
    
    public static enum TT_State {
        INACTIVE, ACTIVE, WAITING
    };
}
