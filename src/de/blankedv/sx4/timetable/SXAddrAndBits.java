/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
