package de.blankedv.sx4;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author mblank
 */
public class Constants {

    /**
     * {@value #VERSION} = program version, displayed in HELP window
     */
    public static final String VERSION = "SX4 - rev1.17 - 01 Feb 2019";
    
    // switch one more debugging?
    // DEBUG can be set via args - and hence is variable
    public static final boolean CFG_DEBUG = false;   
    public static final boolean DEBUG_COMPROUTE = false;

    public static final int STATUS_CONNECTED = 1;
    public static final int STATUS_NOT_CONNECTED = 0;

    public static int INVALID_INT = -1;
    public static int INVALID_TRAIN = 0;

    /**
     * {@value #SX_MIN} = minimale SX adresse angezeigt im Monitor
     */
    public static final int SXMIN = 0;
    /**
     * maximale SX adresse (SX0), maximale adr angezeigt im Monitor
     */
    public static final int SXMAX = 111;
    /**
     * {@value #SX_MAX_USED} = maximale Adresse für normale Benutzung (Loco,
     * Weiche, Signal) higher addresses reserved for command stations/loco
     * programming
     */
    public static final int SXMAX_USED = 106;

    /**
     * {@value #LBMIN} =minimum lanbahn channel number
     */
    public static final int LBMIN = 10;
    public static final int LBPURE = (SXMAX + 1) * 10; // lowest pure lanbahn addres
    /**
     * {@value #LBMAX} =maximum lanbahn channel number
     */
    public static final int LBMAX = 9999;
    /**
     * {@value #LBDATAMIN} =minimum lanbahn data value
     */
    public static final int LBDATAMIN = 0;
    /**
     * {@value #LBDATAMAX} =maximum lanbahn data value (== 2 bits in SX world)
     */
    public static final int LBDATAMAX = 3;  // 
    
    public static enum TT_State {
        INACTIVE, ACTIVE, WAITING
    };
}
