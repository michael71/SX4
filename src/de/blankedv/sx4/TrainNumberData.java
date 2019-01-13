/*
 * used for storing train number at a given sensor "adr"

 */
package de.blankedv.sx4;

import static de.blankedv.sx4.Constants.*;
import static de.blankedv.sx4.SX4.*;
import de.blankedv.sx4.timetable.PanelElement;
import static de.blankedv.sx4.timetable.Vars.panelElements;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author mblank
 */
public class TrainNumberData {
    static private ConcurrentHashMap<Integer,Integer> d = new ConcurrentHashMap<>(500);
 
    public static int update(int addr, int data) {
        if ((addr < 0) || (addr > LBMAX) ) return INVALID_INT;
        
        d.put(addr, data);
        
        return data;
    }
    
    public static int get(int addr) {
        if ((addr < 0) || (addr > LBMAX)) return INVALID_INT;
        if (d.containsKey(addr)) {
            return d.get(addr);
        } else {
            return INVALID_TRAIN;
        }
    }
    
    public static HashMap<Integer, Integer> getAll() {
        return new HashMap(d);
    }
    
    /* called every ~300msec to reset train numbers if no longer occupied
    TODO: check if last train number must be kept for reference (i.e. move to new sensor address)
    */
    public static void auto() {
        // reset train numbers if no longer occupied
        for (PanelElement pe : panelElements) {
            if ((TrainNumberData.get(pe.getAdr()) != INVALID_TRAIN) && (pe.getState() == 0)) {
                TrainNumberData.update(pe.getAdr(), INVALID_TRAIN);
            }             
        }      
    }
}
