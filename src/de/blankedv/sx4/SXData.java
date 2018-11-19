/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import static de.blankedv.sx4.SX4.DEBUG;
import static de.blankedv.sx4.SX4.SXMAX;
import static de.blankedv.sx4.SX4.sxi;

/**
 *
 * @author mblank
 * 
 * central data class, can be used from all threads
 * 
 */
public class SXData {
    
    static private int[] d = new int[SXMAX];
    static private boolean pow = false;
    
    public static synchronized int update(int addr, int data, boolean writeFlag) {
        if (!SXUtils.isValidSXAddress(addr)) return 0;
        
        d[addr] = 0xFF & data;
        if (writeFlag && (sxi != null)) {
            sxi.sendWrite(addr, d[addr]);
        }
        //  System.out.println("set: SX[" + addr + "]=" + d[addr] + " ");

        return d[addr];
    }
    
    public static int get(int addr) {
        return d[addr];
    }
    
    
    synchronized static public void setBit(int addr, int bit, boolean writeFlag) { 
        if (!SXUtils.isValidSXAddress(addr) || (!SXUtils.isValidSXBit(bit))) return;
        
        if (DEBUG) System.out.println("setBit addr="+addr+" bit="+bit);
        d[addr] = SXUtils.setBit(d[addr], bit);  
        if (DEBUG) System.out.println("sxData["+addr+"]="+d[addr]);
        if (writeFlag && (sxi != null)) {
            sxi.sendWrite(addr, d[addr]);
        }
    }

    synchronized static public void clearBit(int addr, int bit, boolean writeFlag) {
        if (!SXUtils.isValidSXAddress(addr) || (!SXUtils.isValidSXBit(bit))) return;
         
        if (DEBUG) System.out.println("clearBit addr="+addr+" bit="+bit);
        d[addr] = SXUtils.clearBit(d[addr], bit);  
        if (DEBUG) System.out.println("sxData["+addr+"]="+d[addr]);
        if (writeFlag && (sxi != null)) {
            sxi.sendWrite(addr, d[addr]);
        }
    }
    
    public static synchronized int setPower(boolean on, boolean writeFlag) {
        pow = on;
        if (writeFlag) {
           sxi.setPower(on);           
        }
        if (pow) {
            return 1;
        } else {
            return 0;
        }
    }
    
    public static synchronized int setPower(int onState, boolean writeFlag) {
        pow = (onState == 1);
        if (writeFlag) {
           sxi.setPower(pow);           
        }
        if (pow) {
            return 1;
        } else {
            return 0;
        }
    }
    
    public static int getPower() {
        if (pow) {
            return 1;
        } else {
            return 0;
        }
    }
    
}
