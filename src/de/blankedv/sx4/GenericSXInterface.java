/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import static de.blankedv.sx4.SX4.*;



/**
 * abstract class for generic selectrix interfaces
 * 
 * @author mblank
 */
abstract public class GenericSXInterface {

    protected boolean connected = false;
    
    abstract public boolean open();
    
    abstract public void close();
    
    public boolean isConnected() {
        return connected;
    }
    
    public int connState() {
        if (connected) {
            return STATUS_CONNECTED;
        } else {
            return STATUS_NOT_CONNECTED;
        }
    }
    
    public String getMode() {
        return "-";
    }
    
    public String doUpdate() {
        ;  // implemented in SXFCCInterface, where a full update can be
        // requested regularly
        return "";
    }

    abstract public boolean request(int addr);   // send request to update a channel to command station

    abstract public int setPower(boolean on) ;
   
    abstract public void requestPower();  // trigger power read from command station

    abstract public boolean sendWrite(int addr, int data) ;
     /*   if (!SXUtils.isValidSXAddress(adr)) {
            System.out.println("ERROR in sendChannel2SX, adr="+adr+ " is invalid");
            return false;
        }
        int data = SXData.get(addr);
        Byte[] b = {(byte) (addr + 128), (byte) data};  // bit 7 muss gesetzt sein zum Schreiben
        return send(b); */
    
    /**
     * sends a loco control command (always SX0 !) to the SX interface
     * 
     * @param lok_adr
     * @param speed
     * @param licht
     * @param forward
     * @param horn 
     */
    public int sendLoco(int lok_adr, int speed, boolean licht, boolean forward, boolean horn) {
        // constructs SX loco data from speed and bit input.
        int data;

        if (speed > 31) {
            speed = 31;
        }
        if (speed < 0) {
            speed = 0;
        }
        if (DEBUG) {
            //System.out.println("adr:" + lok_adr + " s:" + speed + " l:" + licht + " forw:" + forward + " h:" + horn);
        }
        data = speed;  // die unteren 5 bits (0..4)
        if (horn) {
            data += 128; // bit7
        }
        if (licht) {
            data += 64; // bit6
        }
        if (forward == false) {
            data += 32; //bit5
        }
        if (DEBUG) {
            //System.out.println("update loco " + Integer.toHexString(data));
        }
        
        return SXData.update(lok_adr, data, true); // send to SX Command station
    }

    abstract public void registerFeedback(int adr);

 

    public void resetAll() {

        for (int i = 0; i < 112; i++) {
            SXData.update(i, 0, true);
        };

    }

    public void unregisterFeedback() {
    }
    ;

    public void unregisterFeedback(int adr) {
    }
    ;

 }
