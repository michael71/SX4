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
        return new HashMap<>(d);
    }
    
}
