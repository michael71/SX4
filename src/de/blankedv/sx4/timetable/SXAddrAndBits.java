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

package de.blankedv.sx4.timetable;

import static de.blankedv.sx4.Constants.INVALID_INT;

/**
 *
 * @author mblank
 */
public class SXAddrAndBits {
    public int sxAddr = INVALID_INT;
    public int sxBit = INVALID_INT;
    public int nBit = 1;
    
   public SXAddrAndBits() {
        nBit = 1;
   }
    
   public SXAddrAndBits(int a, int b) {
        sxAddr = a;
        sxBit = b;
        nBit = 1;
        
    }
    
    public SXAddrAndBits(int a, int b, int n) {
        sxAddr = a;
        sxBit = b;
        nBit = n;
        
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if (sxAddr == INVALID_INT) {
            return "invalid";
        } else {
            sb.append(" sxAddr=");
            sb.append(sxAddr);
            sb.append(" bits=");
            for (int i=nBit; i>=1; i--) {
               sb.append((sxBit+i-1));           
            }
            return sb.toString();
        }
    }
}
