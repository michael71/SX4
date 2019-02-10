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

import static com.esotericsoftware.minlog.Log.*;
import static de.blankedv.sx4.Constants.*;
import static de.blankedv.sx4.SX4.*;
import de.blankedv.sx4.timetable.CompRoute;
import de.blankedv.sx4.timetable.Loco;
import de.blankedv.sx4.timetable.PanelElement;
import de.blankedv.sx4.timetable.Route;
import static de.blankedv.sx4.timetable.Vars.allLocos;
import static de.blankedv.sx4.timetable.Vars.panelElements;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * hanles one session (=1 mobile device)
 */
public class SXnetClient implements Runnable {

    private static int session_counter = 0;  // class variable !
    private String lastRes = "";
    private long lastSent = 0;

    private int sn; // session number
    private final Socket incoming;
    private PrintWriter out;

    // list of channels which are of interest for this device
    private final int[] sxDataCopy = new int[SXMAX_USED + 1];
    private int lastClientConnect = INVALID_INT;
    private final ConcurrentHashMap<Integer, Integer> oldLanbahnData = new ConcurrentHashMap<>(500);
    private final ConcurrentHashMap<PanelElement, Integer> oldTrainNumberData = new ConcurrentHashMap<>(500);

    private boolean powerCopy = false;
    private int lastRouting = INVALID_INT;

    private Thread worker;

    protected int tickCounter = 0;

    /**
     * Constructs a handler.
     *
     * @param sock the incoming socket
     */
    public SXnetClient(Socket sock) {
        incoming = sock;
        for (int i = 0; i < SXMAX_USED + 1; i++) {
            sxDataCopy[i] = INVALID_INT;
        }
        sn = session_counter++;
    }

    public void stop() {
        worker.interrupt();
    }

    /**
     * Thread receives messages from one mobile device
     *
     */
    public void run() {
        worker = Thread.currentThread();
        try {
            OutputStream outStream = incoming.getOutputStream();
            out = new PrintWriter(outStream, true /* autoFlush */);
            InputStream inStream = incoming.getInputStream();
            Scanner in = new Scanner(inStream);

            Timer sendUpdatesTimer = new Timer();
            sendUpdatesTimer.schedule(new SendUpdatesTask(), 500, 200);  // every 200 msecs

            sendMessage("SXnetServer - client" + sn);  // welcome string

            while (running && in.hasNextLine()) {
                String msg = in.nextLine().trim().toUpperCase();
                if (msg.length() > 0) {
                    debug("sxnet" + sn + " read: " + msg);

                    String[] cmds = msg.split(";");  // multiple commands per line possible, separated by semicolon
                    for (String cmd : cmds) {
                        handleCommand(cmd.trim());
                        // sends feedback message  XL 'addr' 'data' (or INVALID_INT) back to mobile device
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    error("client" + sn + " Thread was interrupted");
                }

            }
            info("client" + sn + " disconnected" + incoming.getRemoteSocketAddress().toString() + "\n");
            sendUpdatesTimer.cancel();

        } catch (IOException e) {
            error("SXnetServerHandler" + sn + " Error: " + e);
        }
        try {
            incoming.close();
        } catch (IOException ex) {
            error("SXnetServerHandler" + sn + " Error: " + ex);
        }

        info("Closing SXnetserverHandler" + sn + "\n");
    }

    // handles feedback, if the sxData have been changed on the SX-Bus
    // feedback both for low (<256) addresses == SX-only (+ Lanbahn if mapping exists)
    // and for high "lanbahn" type addresses
    class SendUpdatesTask extends TimerTask {

        public void run() {
            tickCounter++;
            checkForChangedSXDataAndSendUpdates(tickCounter);
            checkForLanbahnChangesAndSendUpdates();
            checkForTrainNumberChangesAndSendUpdates();
        }
    }

    /**
     * SX Net Protocol (ASCII, all msg terminated with '\n') REV JULY 2018 sent
     * by mobile device -> SX3-PC sends back:
     * ---------------------------------------|------------------- R cc = Read
     * channel cc (0..127) -> returns "X cc dd" S cc.b dd = Set channel cc bit b
     * to Data dd (0 or 1) -> returns "X cc dd" SX cc dd = Set channgel cc to
     * byte dd -> returns "X cc dd"
     *
     * channel 127 bit 8 == Track Power
     *
     * for all channels 0 ... 104 (SXMAX_USED) and 127 all changes are
     * transmitted to all connected clients ,
     */
    private void handleCommand(String m) {
        String[] param = m.split("\\s+");  // remove >1 whitespace
        if (param == null) {
            error("irregular msg: " + m);
        }
        if (param[0].equals("READPOWER")) {
            String res = readPower();  // no parameters
            sendMessage(res);
            return;
        } else if (param[0].equals("UNLOCK")) {
            // used in case of unfinished (i.e. not cleared) routes
            int n = PanelElement.unlockAll();
            sendMessage("XUNLOCKED " + n);
            return;
        }

        String result = "";
        switch (param[0]) {    // commands with 1 or more parameters
            case "SETPOWER":
                result = setPower(param);
                break;

            case "SETLOCO":   // complete byte set 
                result = setSXLocoMessage(param);
                break;
            case "S":    // SX Byte set, used by SX-Loconet Bridge and Andropanel
            case "SX":
                result = setSXByteMessage(param);
                break;

            case "R":    // read sx value, used by SX-Loconet Bridge and Andropanel
                result = readSXByteMessage(param);
                break;
            case "READLOCO":
                result = readLocoMessage(param);
                break;
            case "REQ":   // set or clear route
                if (routingEnabled) {
                    result = requestRouteMessage(param);
                } else {
                    result = "ERROR";
                }
                break;
            case "SET": // for addresses > 1200 (lanbahn sim./routes)
                // will check for "locked" PanelElements
                result = setLanbahnMessage(param);
                break;
            case "SETTRAIN": // for addresses > 1200 (lanbahn sim./routes)
                result = setLanbahnTrainMessage(param);
                break;
            case "READTRAIN": // for addresses > 1200 (lanbahn sim./routes)
                result = createLanbahnTrainMessage(param);
                break;
            case "READ": // for addresses > 1200 (lanbahn sim./routes)
                result = createLanbahnFeedbackMessage(param);
                break;

            default:
                result = "ERROR";
        }
        sendMessage(result);

    }

    // still used by SX-Loconet Bridge and Andropanel !!
    private String readSXByteMessage(String[] par) {
        if (par.length < 2) {
            return "ERROR";
        }
        int adr = getSXAddrFromString(par[1]);
        if (adr == INVALID_INT) {
            error("addr in msg invalid");
            return "ERROR";
        }
        return "X " + adr + " " + SXData.get(adr);
    }

    private String readLocoMessage(String[] par) {
        if (par.length < 2) {
            return "ERROR";
        }
        int adr = getSXAddrFromString(par[1]);
        if (adr == INVALID_INT) {
            error("addr in msg invalid");
            return "ERROR";
        }
        if (Loco.getByAddress(adr) == null) {
            allLocos.add(new Loco(adr));
        }
        return "XLOCO " + adr + " " + SXData.get(adr);
    }

    /**
     * setting (=1) or clearing (=0) routes
     *
     * @param par
     * @return
     */
    private String requestRouteMessage(String[] par) {
        if (DEBUG) {
            error("requestRouteMessage");
        }
        if (par.length <= 2) {
            if (DEBUG) {
                error("par.length <=2");
            }
            return "ERROR";
        }

        // parse string
        int lbAddr = getLanbahnAddrFromString(par[1]);
        int lbdata = getLanbahnDataFromString(par[2]);   // can only be 1= set and 0=clear
        if ((lbAddr == INVALID_INT) || ((lbdata != 0) && (lbdata != 1))) {
            if (DEBUG) {
                error("LB-addr or -data invalid");
            }
            return "ERROR";
        }

        // check whether there is a route with this address(=adr)
        Route r = Route.getFromAddress(lbAddr);
        if (r != null) {
            boolean res = true;
            if (lbdata == 1) {
                res = r.set();  // manual route setting, do not check occupancy
            } else {
                r.clear();  // manual route setting, do not check occupancy 
            }
            if (res) {
                return "XL " + lbAddr + " " + r.getState();  // success
            } else {
                if (DEBUG) {
                    error("route invalid");
                }
                return "ROUTE_INVALID";
            }
        }

        // check whether there is a compound route with this address(=adr)
        CompRoute cr = CompRoute.getFromAddress(lbAddr);
        if (cr != null) {
            boolean res = true;
            if (lbdata == 1) {
                res = cr.set();  // manual route setting, do not check occupancy
            } else {
                cr.clear();
            }
            if (res) {
                return "XL " + lbAddr + " " + cr.getState();  // success
            } else {
                if (DEBUG) {
                    error("comp route invalid");
                }
                return "ROUTE_INVALID";
            }
        }
        if (DEBUG) {
            error("no route or compound found");
        }
        return "ERROR";

    }

    // used by SX-Loconet Bridge and Andropanel
    private String setSXByteMessage(String[] par) {
        if (par.length < 3) {
            return "ERROR";
        }
        debug("setSXByteMessage");

        int adr = getSXAddrFromString(par[1]);
        int data = getByteFromString(par[2]);

        if ((adr == INVALID_INT) || (data == INVALID_INT)) {
            return "ERROR";
        }

        int dNew = SXData.update(adr, data, true);  // synchronized 
        // TODO ?? sxDataCopy[adr] = dNew;  // + store locally (to not duplicate the feedback message)

        return "OK";
    }

    // used by SX-Loconet Bridge and Andropanel
    private String setSXLocoMessage(String[] par) {
        if (par.length < 3) {
            return "ERROR";
        }
        debug("setSXLocoMessage");

        int adr = getSXAddrFromString(par[1]);
        int data = getByteFromString(par[2]);

        if ((adr == INVALID_INT) || (data == INVALID_INT)) {
            return "ERROR";
        }

        if (Loco.getByAddress(adr) == null) {
            allLocos.add(new Loco(adr));
        }

        int dNew = SXData.update(adr, data, true);  // synchronized 
        // TODO ?? sxDataCopy[adr] = dNew;  // + store locally (to not duplicate the feedback message)

        return "OK";
    }

    private String setPower(String[] par) {

        info("setPowerMessage");

        if (par.length < 2) {
            return "ERROR";
        }
        int value = getByteFromString(par[1]);
        if (value == 0) {
            SXData.setPowerToBe(false);
        } else {
            SXData.setPowerToBe(true);
        }

        return "OK";

    }

    private String readPower() {
        powerCopy = SXData.getActualPower();
        if (powerCopy) {
            return ("XPOWER 1");
        } else {
            return ("XPOWER 0");
        }
    }

    /**
     * when setting the data for a lanbahn address, there are 3 possible
     * scenarios: A) it is within the SX address range and only has a single bit
     * of data B) it is within the SX address range, but has 2 bits. The first
     * bit will be a pure SX-bit, the second bit can either be an SX-bit or a
     * virtual lanbahn address C) it is outside the SX address range => it is a
     * virtual lanbahn address
     *
     * @param par
     * @return
     */
    private String setLanbahnMessage(String[] par) {

        info("setLanbahnMessage");

        if (par.length < 3) {
            return "ERROR";
        }
        int lbaddr = getLanbahnAddrFromString(par[1]);
        int lbdata = getLanbahnDataFromString(par[2]);

        if ((lbaddr == INVALID_INT) || (lbdata == INVALID_INT)) {
            return "ERROR";
        }

        if (PanelElement.isAddressLocked(lbaddr)) {
            // cannot set because panel element locked, return current state 
            debug("address " + lbaddr + " is locked.");
            int d;
            if (lbaddr >= LBPURE) {
                d = LanbahnData.get(lbaddr);
            } else {
                d = SXData.get((lbaddr / 10), lbaddr % 10);
            }
            if (d != INVALID_INT) {
                return "XL " + lbaddr + " " + d;
            }
        }

        // not locked, we can set the corresponding data/bit
        if (lbaddr >= LBPURE) {
            // pure lanbahn virtual address
            int res = LanbahnData.update(lbaddr, lbdata);
            if (res != INVALID_INT) {
                return "OK";
            }
        } else {
            // SX data range (=real data)
            int sxaddr = lbaddr / 10;
            int sxbit = lbaddr % 10;

            if (SXUtils.isValidSXAddress(sxaddr) && SXUtils.isValidSXBit(sxbit)) {
                if (lbdata != 0) {
                    SXData.setBit(sxaddr, sxbit, true);
                } else {
                    SXData.clearBit(sxaddr, sxbit, true);
                }
                return "OK";
            }

        }
        return "ERROR";
    }

    private String setLanbahnTrainMessage(String[] par) {

        info("setLanbahnTrainMessage");

        if (par.length < 3) {
            return "ERROR";
        }
        int addr = getNumberFromString(par[1]);
        int data = getDataFromString(par[2]);
        if ((addr == INVALID_INT) || (data == INVALID_INT)) {
            return "ERROR";
        }
        // update train number info
        if (PanelElement.setTrain(addr, data)) {
            return "OK";
        }

        return "ERROR";
    }

    private String createLanbahnFeedbackMessage(String[] par) {

        debug("createLanbahnFeedbackMessage");

        if (par.length < 2) {
            return "ERROR";
        }
        int lbAddr = getLanbahnAddrFromString(par[1]);
        if (lbAddr == INVALID_INT) {
            return "ERROR";
        }
        if (lbAddr >= LBPURE) {
            int d = LanbahnData.get(lbAddr);
            if (d != INVALID_INT) {
                return "XL " + lbAddr + " " + d;
            }
        } else {
            int sxaddr = lbAddr / 10;
            int sxbit = lbAddr % 10;
            if (SXUtils.isValidSXAddress(sxaddr) && SXUtils.isValidSXBit(sxbit)) {
                if (SXUtils.isSet(SXData.get(sxaddr), sxbit)) {
                    return "XL " + lbAddr + " 1";
                } else {
                    return "XL " + lbAddr + " 0";
                }

            }
        }
        return "ERROR";

    }

    private String createLanbahnTrainMessage(String[] par) {

        debug("createLanbahnTrainMessage");

        if (par.length < 2) {
            return "ERROR";
        }
        int addr = getNumberFromString(par[1]);
        if (addr == INVALID_INT) {
            return "ERROR";
        }
        int d = PanelElement.getTrain(addr);
        if (d != INVALID_INT) {
            return "XTRAIN " + addr + " " + d;
        }

        return "ERROR";

    }

    private int getByteFromString(String s) {
        // converts String to integer between 0 and 255 
        //    (= range of SX Data and of Lanbahn data values)
        try {
            int data = Integer.parseInt(s);
            if ((data >= 0) && (data <= 255)) {  // 1 byte
                return data;
            }
        } catch (NumberFormatException e) {
            //
        }
        return INVALID_INT;
    }

    private int getLanbahnDataFromString(String s) {
        // converts String to integer between 0 and 3
        //    (= range Lanbahn data values)
        Integer data;
        try {
            data = Integer.parseInt(s);
            if ((data >= LBDATAMIN) && (data <= LBDATAMAX)) {
                return data;
            }
        } catch (Exception e) {
            //
        }
        return INVALID_INT;
    }

    private int getDataFromString(String s) {
        // converts String to integer between 0 and 9999
        //    (= range train numbers)
        Integer data;
        try {
            data = Integer.parseInt(s);
            if ((data >= 0) && (data <= 9999)) {
                return data;
            }
        } catch (Exception e) {
            //
        }
        return INVALID_INT;
    }

    /**
     * extract the selectrix address from a string, only valid addresses 0...111
     * are allowed, else "INVALID_INT" is returned
     *
     * @param s
     * @return addr (or INVALID_INT)
     */
    int getSXAddrFromString(String s) {

        try {
            int channel = Integer.parseInt(s);
            if ((channel >= SXMIN) && (channel <= SXMAX_USED)) {
                return channel;
            } else {
                return INVALID_INT;
            }

        } catch (Exception e) {
            error("ERROR: number conversion error input=" + s);
            return INVALID_INT;
        }
    }

    /**
     * parse String to extract a lanbahn address
     *
     * @param s
     * @return lbaddr (or INVALID_INT)
     */
    int getLanbahnAddrFromString(String s) {

        Integer lbAddr;
        try {
            lbAddr = Integer.parseInt(s);
            if ((lbAddr >= LBMIN) && (lbAddr <= LBMAX)) {
                return lbAddr;
                // OK, valid lanbahn channel - either SX-mapped or PURE lanbahn
            } else {
                error("lbAddr=" + lbAddr + " not valid");
                return INVALID_INT;
            }
        } catch (Exception e) {
            error("number conversion error input=" + s);
            return INVALID_INT;
        }
    }

    /**
     * parse String to extract a number between 0 and LBMAX
     *
     * @param s
     * @return lbaddr (or INVALID_INT)
     */
    int getNumberFromString(String s) {

        Integer addr;
        try {
            addr = Integer.parseInt(s);
            if ((addr >= 0) && (addr <= LBMAX)) {
                return addr;
                // OK, valid lanbahn channel - either SX-mapped or PURE lanbahn
            } else {
                error("addr=" + addr + " not valid");
                return INVALID_INT;
            }
        } catch (Exception e) {
            error("number conversion error input=" + s);
            return INVALID_INT;
        }
    }

    public void sendMessage(String res) {

        // don't send empty messages and don't send duplicate messages within 200 ms
        if (res.isEmpty() || (res.equals(lastRes) && (System.currentTimeMillis() - lastSent < 200))) {
            return;
        }

        // store for later use
        lastRes = res;
        lastSent = System.currentTimeMillis();

        out.println(res);
        //out.flush(); autoflush is set to true

        debug("sxnet" + sn + " send: " + res);

    }

    /**
     * check for changed sxData and send update in case of change
     */
    private void checkForChangedSXDataAndSendUpdates(int tick) {
        StringBuilder msg = new StringBuilder();
        boolean first = true;

        // report change in power channel (but only if "stable")
        // send also as "connected" tick
        if (((tick % 20) == 0) || (SXData.getActualPower() != powerCopy)) {
            powerCopy = SXData.getActualPower();
            if (powerCopy) {
                msg.append("XPOWER 1");
            } else {
                msg.append("XPOWER 0");
            }
            first = false;
        }

        // report change in connect status
        if ((lastClientConnect == INVALID_INT) || (sxi.connState() != lastClientConnect)) {
            lastClientConnect = sxi.connState();
            if (!first) {
                msg.append(";");
            }
            msg.append("XCONN ");
            msg.append(lastClientConnect); // 1 or 0
            first = false;
        }

        // report routing status (only ONCE ! at startup)
        if (lastRouting == INVALID_INT) {
            if (routingEnabled) {
                lastRouting = 1;
            } else {
                lastRouting = 0;
            }
            if (!first) {
                msg.append(";");
            }
            msg.append("ROUTING ");
            msg.append(lastRouting); // 1 or 0
            first = false;
        }

        // report changes in other channels
        for (int ch = 0; ch <= SXMAX_USED; ch++) {
            if (SXData.get(ch) != sxDataCopy[ch]) {
                sxDataCopy[ch] = SXData.get(ch);
                // channel data changed, send update to mobile device 
                if (!first) {
                    msg.append(";");
                }

                // if (locoAddresses.contains(ch)) {
                //     msg.append("XLOCO ");
                // } else {
                msg.append("X ");
                // }
                msg.append(ch).append(" ").append(sxDataCopy[ch]);  // SX Feedback Message
                first = false;

                if (msg.length() > 60) {
                    sendMessage(msg.toString());
                    msg.setLength(0);  // =delete content
                    first = true;
                }
            }

        }
        sendMessage(msg.toString());  // send all messages, separated with ";"
    }

    /**
     * check for changed (exclusiv) lanbahn data and send update in case of
     * change
     *
     */
    private void checkForLanbahnChangesAndSendUpdates() {
        StringBuilder msg = new StringBuilder();
        for (Map.Entry e : LanbahnData.getAll().entrySet()) {
            Integer key = (Integer) e.getKey();
            Integer value = (Integer) e.getValue();
            if (oldLanbahnData.containsKey(key)) {
                // key (channel) is known, but data have changed
                if (!Objects.equals(oldLanbahnData.get(key), value)) {
                    // value has changed
                    oldLanbahnData.put(key, value);
                    if (msg.length() != 0) {
                        msg.append(";");
                    }
                    msg.append("XL ").append(key).append(" ").append(value);
                    if (msg.length() > 60) {
                        sendMessage(msg.toString());
                        msg.setLength(0);  // =delete content
                    }
                }
            } else {
                // new key
                oldLanbahnData.put(key, value);
                if (msg.length() != 0) {
                    msg.append(";");
                }
                msg.append("XL ").append(key).append(" ").append(value);
                if (msg.length() > 60) {
                    sendMessage(msg.toString());
                    msg.setLength(0);  // =delete content
                }

            }
        }
        if (msg.length() > 0) {
            sendMessage(msg.toString());
        }
    }

    /**
     * check for changed train number data and send update in case of change
     * this is only done for sensors - because these are the only panel elements
     * which can have valid train numbers
     *
     */
    private void checkForTrainNumberChangesAndSendUpdates() {
        StringBuilder msg = new StringBuilder();
        for (PanelElement pe : panelElements) {
            if (pe.isSensor()) {
                Integer data = (Integer) pe.getTrain();

                if (oldTrainNumberData.containsKey(pe)) {
                    // key (channel) is known, but data have changed
                    if (!Objects.equals(oldTrainNumberData.get(pe), data)) {
                        // value has changed
                        oldTrainNumberData.put(pe, data);
                        if (msg.length() != 0) {
                            msg.append(";");
                        }
                        msg.append("XTRAIN ").append(pe.getAdr()).append(" ").append(data);
                        if (msg.length() > 60) {
                            sendMessage(msg.toString());
                            msg.setLength(0);  // =delete content
                        }
                    }
                } else {
                    // new key
                    oldTrainNumberData.put(pe, data);
                    if (msg.length() != 0) {
                        msg.append(";");
                    }
                    msg.append("XTRAIN ").append(pe.getAdr()).append(" ").append(data);
                    if (msg.length() > 60) {
                        sendMessage(msg.toString());
                        msg.setLength(0);  // =delete content
                    }

                }
            }
        }
        if (msg.length() > 0) {
            sendMessage(msg.toString());
        }
    }

}
