/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4.timetable;

import static com.esotericsoftware.minlog.Log.debug;
import static com.esotericsoftware.minlog.Log.error;
import static de.blankedv.sx4.Constants.*;
import de.blankedv.sx4.SXData;
import de.blankedv.sx4.SXUtils;
import static de.blankedv.sx4.timetable.Vars.panelElements;
import java.util.ArrayList;

/**
 * all active panel elements, like turnouts, signals, trackindicators (=sensors)
 * are derviced from this class. These elements have a "state" which is exactly
 * the same number as the "data" of the lanbahn messages "SET 810 2" => set
 * state of panel element with address=810 to state=2
 *
 * a panel element has only 1 address (=> double slips are 2 panel elements)
 *
 * @author mblank
 *
 */
public class PanelElement {

   
    private int state = 0;
    private int adr = INVALID_INT;
    private int secondaryAdr = INVALID_INT;  // needed for DCC sensors/signals 
    private int nbit = 1;  // number of significant bits
    private String typeString = "AC";
    // with 2 addresses (adr1=occ/free, 2=in-route)
    protected String route = "";

    // these constants are defined just for easier understanding of the
    // methods of the classes derived from this class
    // turnouts
    protected static final int STATE_CLOSED = 0;
    protected static final int STATE_THROWN = 1;
    protected static final int N_STATES_TURNOUTS = 2;

    // signals
    protected static final int STATE_RED = 0;
    protected static final int STATE_GREEN = 1;
    protected static final int STATE_YELLOW = 2;
    protected static final int STATE_YELLOW_FEATHER = 3;
    protected static final int STATE_SH1 = 3;
    protected static final int N_STATES_SIGNALS = 4;

    // buttons
    protected static final int STATE_NOT_PRESSED = 0;
    protected static final int STATE_PRESSED = 1;

    // sensors
    protected static final int STATE_FREE = 0;
    protected static final int STATE_OCCUPIED = 1;
    protected static final int STATE_INROUTE = 2;
    protected static final int N_STATES_SENSORS = 3;

    protected long lastToggle = 0L;
    protected long lastUpdateTime = 0L;

    public PanelElement() {
    }

    /**
     * constructor for an ACTIVE panel element with 1 address default state is
     * "CLOSED" (="RED")
     */
    public PanelElement(int adr) {

        this.adr = adr;
        this.secondaryAdr = INVALID_INT;
        typeString = "AC";
        this.state = 0;  // initialized to CLOSED / RED / FREE
        lastUpdateTime = System.currentTimeMillis();
    }

    public PanelElement(String t, int adr) {

        this.adr = adr;
        this.secondaryAdr = INVALID_INT;
        typeString = t;
        this.state = 0;  // initialized to CLOSED / RED / FREE
        lastUpdateTime = System.currentTimeMillis();
    }

    public PanelElement(String t, int adr, int adr2) {
        typeString = t;
        this.adr = adr;
        this.secondaryAdr = adr2;
        this.state = 0;  // initialized to CLOSED / RED / FREE
    }

    public int getAdr() {
        return adr;
    }

    public int getSecondaryAdr() {
        return secondaryAdr;
    }

    public int getState() {
        return state;
    }

    public int setState(int val) {
        state = val;
        return state;
    }
    
    public int setStateAndUpdateSXData(int val) {
        state = val;
        updateSXData();
        return state;
    }

    public boolean hasAdrX(int address) {
        if (adr == address) {
            return true;
        } else {
            return false;
        }
    }

    public void setAdr(int adr) {
        this.adr = adr;
        this.state = this.state = 0;  // initialized to CLOSED / RED / FREE
        this.lastUpdateTime = System.currentTimeMillis();
        if (adr != INVALID_INT) {
            //TODO sendQ.add("READ " + adr); // request update for this element
        }
    }

    public void setSecondaryAdr(int adr) {
        this.secondaryAdr = adr;
    }

    public int setBit0(boolean occ) {
        lastUpdateTime = System.currentTimeMillis();
        if (occ) {
            // set bit 0
            state |= 0x01;
        } else {
            state &= ~(0x01);
        }
        return state;
    }

    public int setBit1(boolean occ) {
        lastUpdateTime = System.currentTimeMillis();
        if (occ) {
            // set bit 1
            state |= 0x02;
        } else {
            state &= ~(0x02);
        }
        return state;
    }

    public boolean isBit0() {
        if ((state & 0x01) != 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isBit1() {
        if ((state & 0x02) != 0) {
            return true;
        } else {
            return false;
        }
    }

    public String getType() {
        return typeString;
    }

    public boolean isSignal() {
        return (typeString.equals("Si"));
    }

    public boolean isTurnout() {
        return (typeString.equals("T"));
    }

    public boolean isSensor() {
        return (typeString.equals("BM"));
    }

    public String toHTML() {
        StringBuilder sb = new StringBuilder();
        switch (typeString) {  // background color depending on type
            case "DS":
                sb.append("<html><p bgcolor='#FFFF00'>DS");
                break;
            case "BM":
                sb.append("<html><p bgcolor='#FFFF00'>BM");
                break;
            case "Si":
                sb.append("<html><p bgcolor='#DDDDDD'>Si");
                break;

            default:
                sb.append("<html><p>");
                sb.append(typeString);
                break;
        }
        if (secondaryAdr != INVALID_INT) {
            sb.append("(");
            sb.append(secondaryAdr);
            sb.append(")");
        }
        sb.append(" <strong>");
        sb.append(adr).append("</strong></p></html>");
        return sb.toString();
    }

    // TODO move sx address calculations to constructor
    public void updateSXData() {
        if (adr == INVALID_INT) return;  
        
        int sxadr = adr / 10;   
        int sxbit = adr % 10;
        if (!SXUtils.isValidSXAddress(sxadr) || !SXUtils.isValidSXBit(sxbit)) {
            return;  // no SX Element, must be virtual
        }
        
        // set low bit
        switch (state) {
            case 0:
            case 2:
                SXUtils.clearBitSxData(sxadr, sxbit, true);  // true => write to SXInterface
                break;
            case 1:
            case 3:
                SXUtils.setBitSxData(sxadr, sxbit, true);  // true => write to SXInterface
                break;
            default:
                System.out.println("invalid state in sx addr a=" + sxadr+ "."+sxbit + " d=" + state);
        }
        
        // set high bit (if there is any)
        if (secondaryAdr != INVALID_INT) {
            int secSxadr = secondaryAdr / 10;
            int secSxbit = secondaryAdr % 10;

            // there is a second address 
            if (SXUtils.isValidSXAddress(secSxadr)) {

                switch (state) {
                    case 0:
                        SXUtils.clearBitSxData(secSxadr, secSxbit, true);  // true => write to SXInterface
                        break;
                    case 1:
                        SXUtils.clearBitSxData(secSxadr, secSxbit, true);  // true => write to SXInterface
                        break;
                    case 2:
                        SXUtils.clearBitSxData(secSxadr, secSxbit, true);  // true => write to SXInterface
                        break;
                    case 3:
                        SXUtils.clearBitSxData(secSxadr, secSxbit, true);  // true => write to SXInterface
                        break;
                    default:
                        error("invalid state in sx (4-aspect) addr a=" + adr + " d=" + state);
                }
            }
        }
        
       
    }
    
    /* public void sendUpdateToSXBus() {
         SXAddrAndBits sx = SXUtils.lbAddr2SX(adr);
         debug("sendUpd->SXBus lbaddr="+adr+" sxaddr="+sx.sxAddr+" d="+SXData.get(sx.sxAddr));
         if (sx != null) {
               sxi.sendChannel2SX(sx.sxAddr);
         }
     } */
     
    public boolean isLanbahnAddress() {
        return (adr >=LBPURE);
    }
    
    public boolean isSecondaryLanbahnAddress() {
        return (secondaryAdr >=LBPURE);
    }

    // STATIC METHODS
    /**
     * search for a panel element when only the address is known
     *
     * @param address
     * @return
     */
    public static ArrayList<PanelElement> getByAddress(int address) {
        ArrayList<PanelElement> peList = new ArrayList<>();
        for (PanelElement pe : panelElements) {
            if (pe.getAdr() == address) {
                peList.add(pe);
            }
        }
        return peList;
    }

    public static int getSecondaryAddressByAddress(int address) {
        for (PanelElement pe : panelElements) {
            if (pe.getAdr() == address) {
                return pe.secondaryAdr;
            }
        }
        return INVALID_INT;
    }

    public static PanelElement getSingleByAddress(int address) {
        for (PanelElement pe : panelElements) {
            if (pe.getAdr() == address) {
                return pe;
            }
        }
        return null;
    }

    
}
