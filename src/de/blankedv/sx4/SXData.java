/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import static de.blankedv.sx4.Constants.*;
import static de.blankedv.sx4.SX4.*;
import static com.esotericsoftware.minlog.Log.*;
import de.blankedv.sx4.timetable.PanelElement;

/**
 *
 * @author mblank
 *
 * central data class, can be used from all threads
 *
 */
public class SXData {

    static private int[] d = new int[SXMAX + 1];
    static private int power = INVALID_INT;

    public static synchronized int update(int addr, int data, boolean writeFlag) {
        if (!SXUtils.isValidSXAddress(addr)) {
            return 0;
        }

        d[addr] = 0xFF & data;
        if (sxi != null) {
            if (writeFlag) {  //WRITE to central station
                try {
                    dataToSend.put(new IntegerPair(addr, d[addr]));
                } catch (InterruptedException ex) {
                    error("ERROR - sendqueue full");
                }
            }
        }
        debug("set: SX[" + addr + "]=" + d[addr] + " ");
        PanelElement.updateFromSXData(addr, d[addr]);

        return d[addr];
    }

    public static int get(int addr) {
        if ((sxi != null) && (d[addr] == INVALID_INT)) {
            // this can only happen for "old SX Interface", where data are
            // polled - for FCC and SLX we always have valid data for ALL channels
            try {
                // request data read
                dataToSend.put(new IntegerPair(addr, INVALID_INT));
            } catch (InterruptedException ex) {
                error("ERROR - sendqueue full");
            }
        }
        return d[addr];
    }

    synchronized static public void setBit(int addr, int bit, boolean writeFlag) {
        if (!SXUtils.isValidSXAddress(addr) || (!SXUtils.isValidSXBit(bit))) {
            return;
        }

        debug("setBit addr=" + addr + " bit=" + bit);

        d[addr] = SXUtils.setBit(d[addr], bit);

        debug("sxData[" + addr + "]=" + d[addr]);

        if (writeFlag && (sxi != null)) {
            try {
                dataToSend.put(new IntegerPair(addr, d[addr]));
            } catch (InterruptedException ex) {
                error("ERROR - sendqueue full");
            }
            //sxi.sendWrite(addr, d[addr]);
        }
    }

    synchronized static public void clearBit(int addr, int bit, boolean writeFlag) {
        if (!SXUtils.isValidSXAddress(addr) || (!SXUtils.isValidSXBit(bit))) {
            return;
        }

        debug("clearBit addr=" + addr + " bit=" + bit);

        d[addr] = SXUtils.clearBit(d[addr], bit);

        debug("sxData[" + addr + "]=" + d[addr]);

        if (writeFlag && (sxi != null)) {
            try {
                dataToSend.put(new IntegerPair(addr, d[addr]));
            } catch (InterruptedException ex) {
                error("ERROR - sendqueue full");
            }
            //sxi.sendWrite(addr, d[addr]);
        }
    }

    public static int getPower() {
        return power;
    }

    public static void setPower(int onState, boolean writeFlag) {
        //error("SetPower to " + onState);

        if ((writeFlag) && (!simulation)) {
            // sending power command to interface
            powerToBe.set(onState);
            debug("powerToBe=" + power);
        } else {
            //received power feedback from interface
            if (onState != 0) {
                power = 1;
            } else {
                power = 0;
            }
        }

    }

}
