/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import static de.blankedv.sx4.SX4.*;

/**
 *
 * @author mblank
 */
public class SXSimulationInterface extends GenericSXInterface {

    Boolean serialPortGeoeffnet = false;

    SXSimulationInterface() {
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
    public synchronized boolean request(int addr) {
        // not needed, because sim-state is represented in SXData.d values
        return true;
    }

    
}
