/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import static de.blankedv.sx4.SX4.*;

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

    



    /**
     * is the address a valid SX0 or SX1 address
     *
     * @param address
     * @return true or false
     */
    public static boolean isValidSXAddress(int a) {
        if (((a >= SXMIN) && (a <= SXMAX))) {
            //if (DEBUG) System.out.println("isValidSXAddress? "+a + " true (SX0");
            return true;  // 0..111 or 127
        }

        //if (DEBUG) System.out.println("isValidSXAddress? "+a + " false");
        return false;
    }

    /**
     * is the sxbit a valid SX sxbit? (1...8)
     *
     * @param bit
     * @return true or false
     */
    public static boolean isValidSXBit(int bit) {

        if ((bit >= 1) && (bit <= 8)) {
            return true;  // 1..8
        }

        //if (DEBUG) System.out.println("isValidSXAddress? "+a + " false");
        return false;
    }

    /*
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
    } */

    /** update the internal state if there is a (or several) matching panel 
     * elements for this sxAddr
     * !!! DO NOT SEND AN UPDATE TO THE SX INTERFACE (-> LOOP !!)
     *
     * @param sxAddr
     * @param sxdata 
     */
    /*
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
    } */
    
    
}
