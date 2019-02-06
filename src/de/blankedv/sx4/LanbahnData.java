/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import static de.blankedv.sx4.Constants.*;
import static de.blankedv.sx4.SX4.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author mblank
 */
public class LanbahnData {
    static private ConcurrentHashMap<Integer,Integer> d = new ConcurrentHashMap<>(500);
 
    public static int update(int addr, int data) {
        if ((addr < LBPURE) || (addr > LBMAX) 
                || (data < LBDATAMIN) || (data > LBDATAMAX)) return INVALID_INT;
        
        d.put(addr, data);
        
        return data;
    }
    
    public static int get(int addr) {
        if ((addr < LBPURE) || (addr > LBMAX)) return INVALID_INT;
        if (d.containsKey(addr)) {
            return d.get(addr);
        } else {
            update(addr, 0); // initialize
            return 0;
        }
    }
    
    public static HashMap<Integer, Integer> getAll() {
        return new HashMap<Integer, Integer>(d);
    }
    
}
