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

import static com.esotericsoftware.minlog.Log.debug;
import static com.esotericsoftware.minlog.Log.trace;
import static de.blankedv.sx4.Constants.*;
import de.blankedv.sx4.timetable.PanelElement;
import de.blankedv.sx4.timetable.SXAddrAndBits;
import static de.blankedv.sx4.timetable.Vars.panelElements;
import java.io.File;

//import de.blankedv.timetable.PanelElement;

/**
 *
 * @author mblank
 */
public class SXUtils {

    /**
     * returns 1, when sxbit is set in data d returns 0, when sxbit is not set in
 data d
     *
     * @param d
     * @param bit
     * @return
     */
    static public boolean isSet(int d, int bit) {
        return ((d >> (bit - 1)) & 1) == 1;
    }

    static public int setBit(int d, int bit) {
        return d | (1 << (bit - 1));  // selectrix sxbit !!! 1 ..8
    }

    static public int clearBit(int d, int bit) {
        return d & ~(1 << (bit - 1));  // selectrix sxbit !!! 1 ..8
    }

     synchronized static public void setBitSxData(int addr, int bit, boolean writeFlag) {
        debug("setBit addr="+addr+" bit="+bit);
        SXData.update(addr,  setBit(SXData.get(addr), bit), writeFlag);
        debug("sxData["+addr+"]="+SXData.get(addr));
    }

    synchronized static public void clearBitSxData(int addr, int bit, boolean writeFlag) {
        debug("clearBit addr="+addr+" bit="+bit);
        SXData.update(addr, clearBit(SXData.get(addr),bit), writeFlag);
        debug("sxData["+addr+"]="+SXData.get(addr));
    }
    
    /**
     * is the address a valid SX0 or SX1 address
     *
     * @param address
     * @return true or false
     */
    public static boolean isValidSXAddress(int a) {
        if (((a >= SXMIN) && (a <= SXMAX))) {
            //if (DEBUG) error("isValidSXAddress? "+a + " true (SX0");
            return true;  // in range 0..111
        }
        //if (DEBUG) error("isValidSXAddress? "+a + " false");
        return false;
    }

    /**
     * is the sxbit a valid SX sxbit? (1...8) starting with 1 !!
     *
     * @param bit
     * @return true or false
     */
    public static boolean isValidSXBit(int bit) {

        if ((bit >= 1) && (bit <= 8)) {
            return true;  // 1..8
        }

        //if (DEBUG) error("isValidSXAddress? "+a + " false");
        return false;
    }

    public static SXAddrAndBits lbAddr2SX(int lbAddr) {
        if (lbAddr == INVALID_INT) {
            return null;
        }
        int a = lbAddr / 10;
        int b = lbAddr % 10;
        if (isValidSXAddress(a) && isValidSXBit(b)) {
            return new SXAddrAndBits(a, b, 1);  // TODO generalize for multibit addresses
        } else {
            return null;
        }
    }

    /** update the internal state if there is a (or several) matching panel 
     * elements for this sxAddr
     * !!! DO NOT SEND AN UPDATE TO THE SX INTERFACE (-> LOOP !!)
     *
     * @param sxAddr
     * @param sxdata 
     */
    public static void updatePanelElementsStateFromSX(int sxAddr, int sxdata) {
        for (int sxbit = 1; sxbit <= 8; sxbit++) {
            // check if we have a matching panel element
            int lbAddr = sxAddr * 10 + sxbit;
            for (PanelElement pe : panelElements) {
                if (pe.getAdr() == lbAddr) {
                    pe.setBit0(isSet(sxdata, sxbit));
                }
                if (pe.getSecondaryAdr() == lbAddr) {
                    pe.setBit1(isSet(sxdata, sxbit));
                }
            }
        }
    }
    
    public static String getConfigFilename() {
        String fileName = "";
   
        File curDir = new File(".");
        File[] filesList = curDir.listFiles();
        for (File f : filesList) {
            if (f.getName().matches("panel(.*).xml")) {
                trace("found panel file in cur dir: " + f.getName());
                fileName = f.getName();
            }
        }

        if (fileName.length() > 8) {
            return fileName;
        } else {
           return "";
        }
    }
}
