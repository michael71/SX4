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

import static de.blankedv.sx4.Constants.*;
import static de.blankedv.sx4.SX4.*;
import static com.esotericsoftware.minlog.Log.*;


/**
 * abstract class for generic selectrix interfaces
 * 
 * @author mblank
 */
abstract public class GenericSXInterface {

    protected boolean connected = false;
    
    abstract public boolean open();
    
    abstract public void close();
    
    abstract public void request(int addr);   // send request to update a channel to command station
    
    abstract public void requestPower();  // trigger power read from command station
      
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
        // requested regularly and in SLX825 (data updates received automatically)
        return "";
    }
  
 
  
   
    
  
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
       
            trace("adr:" + lok_adr + " s:" + speed + " l:" + licht + " forw:" + forward + " h:" + horn);
        
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
        trace("update loco " + Integer.toHexString(data));
        
        return SXData.update(lok_adr, data, true); // send to SX Command station
    }

    // the feedback routines are only needed for "polling mode", i.e. for the
    // very simple 66824 interface which does send updated SX data not 
    // automatically
    public void registerFeedback(int adr){};


    public void unregisterFeedback() {
    };

    public void unregisterFeedback(int adr) {
    };

 }
