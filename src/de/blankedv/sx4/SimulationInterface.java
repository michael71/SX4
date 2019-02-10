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

import static de.blankedv.sx4.SX4.*;

/**
 *
 * @author mblank
 */
public class SimulationInterface extends GenericSXInterface {

    Boolean serialPortGeoeffnet = false;

    SimulationInterface() {
    }
    
    @Override
    public boolean open() {
        serialPortGeoeffnet = true;
        connected = true;
        return true;
    }

    @Override
    public void close() {
        connected = false;
        serialPortGeoeffnet = false;
    }
       
    @Override
    public void registerFeedback(int adr) {
    }


    @Override
    public void requestPower() {
        connectionOK = true;  // the only reason for this request is to 
        // check whether command station is still reachable
    }

    @Override
    public void request(int addr) {
        // not needed, because sim-state is represented in SXData.d values
    }
    
    @Override
    public String doUpdate() {
        // empty send queue
       while (!dataToSend.isEmpty()) {
                IntegerPair sxd = dataToSend.poll();
               
       }
       return "OK";
    }

    
}
