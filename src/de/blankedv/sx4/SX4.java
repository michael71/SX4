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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.text.SimpleDateFormat;

import static de.blankedv.sx4.Constants.*;
import static com.esotericsoftware.minlog.Log.*;
import de.blankedv.sx4.timetable.CompRoute;
import de.blankedv.sx4.timetable.FXGUI;
import de.blankedv.sx4.timetable.FileWatcher;
import de.blankedv.sx4.timetable.PanelElement;

import de.blankedv.sx4.timetable.ReadConfig;
import de.blankedv.sx4.timetable.ReadConfigTrips;
import de.blankedv.sx4.timetable.Route;
import de.blankedv.sx4.timetable.Trip;
import de.blankedv.sx4.timetable.TimetableUI;
import static de.blankedv.sx4.timetable.Vars.panelElements;
import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.prefs.Preferences;
import static de.blankedv.sx4.timetable.Vars.allTimetableUIs;

/**
 *
 * with main() function
 *
 * @author mblank
 */
public class SX4 {

    // TODO mix Betrieb Fahrstrasse und Stellen per Hand einführen
    public static volatile boolean running = true;
    public static boolean routingEnabled = false;
    public static boolean guiEnabled = false;

    public static ArrayBlockingQueue<IntegerPair> dataToSend = new ArrayBlockingQueue<>(400);

    public static List<InetAddress> myips;
    public static String configFilename = "";

    public static GenericSXInterface sxi;
    static WifiThrottle wifiThrottle;

    public static int baudrate;
    public static String portName;
    public static boolean simulation = false;
    public static String ifType;
    public static int timeoutCounter = 0;
    public static final int TIMEOUT_SECONDS = 10;  // check for connection every 30secs

    public static boolean connectionOK = false;  // watchdog for connection
    public static final int NUMBER_OF_FILES_TO_RETAIN = 5;

    private static int updateCount = 0;
    private static Timer timer = new Timer();
    private static long lastSavedTrainNumbersTime = 0;
    public static volatile long lastConnected = System.currentTimeMillis();

    private FileWatcher panelWatch;

    @SuppressWarnings("SleepWhileInLoop")
    public static void main(String[] args) {
        SX4 app = new SX4();
        app.runSX4(args);

    }

    @SuppressWarnings("SleepWhileInLoop")
    public void runSX4(String[] args) {

        if (isDebugFlagSet(args)) {  // must be done first to log errors during command line eval)          
            set(LEVEL_DEBUG);
        } else {
            // only 2 different logging levels are used: INFO or DEBUG (if "-d" on command line start)
            set(LEVEL_INFO);
        }

        startLogging();  // start simple logging

        EvalOptions.sx4options(args);

        boolean fxresult = false;
        if (guiEnabled) {
            fxresult = FXGUI.init();
            if (fxresult == false) {
                guiEnabled = false;
                error("could not start javafx");
            } else {
                debug("starting javafx");
            }
        }

        boolean result = false;
        sxi = initSXInterface(portName, baudrate);
        if (sxi != null) {
            result = sxi.open();
        }
        if (!result) {
            error("Could not open SX-interface, SX4 program ends.");
            System.exit(1);
        }
        configFilename = SXUtils.getConfigFilename();

        if (configFilename.isEmpty()) {
            error("no panel...xml file found, NOT starting config server");
            if (routingEnabled) {
                routingEnabled = false;  // override setting
                error("routing and gui disabled - because there is no config file");
                guiEnabled = false;
            }
        } else {
            if (!ReadConfig.readXML(configFilename).equals("OK")) {
                // config has to be read successfully - a requirement for enabling routing
                if (routingEnabled) {
                    routingEnabled = false;  // override setting
                    error("routing disabled - because config file could not be read");
                }
            } else {
                if (guiEnabled) {
                    // read also timetables (needs JavaFX)
                    ReadConfigTrips.readTripsAndTimetables(configFilename);
                }
            }

            loadTrainNumbers();  // sensors must be know when setting train numbers

            try {
                new ConfigWebserver(configFilename);
            } catch (Exception ex) {
                error(ex.getMessage());
            }

            try {
                String pN = "/home/mblank/NetBeansProjects/SX4/panel_sxtest.xml";
                panelWatch = new FileWatcher(new File(pN));
                panelWatch.start();
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        }

        myips = NIC.getmyip();

        if (myips.isEmpty()) {
            error("no network - SX4 program ends.");
            System.exit(1);
        }

        final SXnetServer serv = new SXnetServer();

        wifiThrottle = new WifiThrottle();

        if (guiEnabled) {
            FXGUI.start(args);
        }

        Timer timer = new java.util.Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //System.out.println("m400");
                sxi.doUpdate();     // includes reading all SX data 
                if (routingEnabled) {
                    if (guiEnabled) {
                        for (TimetableUI tt : allTimetableUIs) {
                            tt.auto();
                        }
                        Trip.auto();
                    }
                    Route.auto();
                    CompRoute.auto();
                }
                saveTrainNumbers();

            }
        }, 300, 300);

        shutdownHook(serv);
    }

    private void shutdownHook(SXnetServer server) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    running = false;  // shutdown all threads
                    server.stopClients();
                    if (panelWatch != null) {
                        panelWatch.stopThread();
                    }
                    Thread.sleep(200);
                    info("Shutdown, SX4 ends.");
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                }
            }
        });
    }

    /**
     * save current train numbers for all sensors
     *
     */
    private void saveTrainNumbers() {
        if ((System.currentTimeMillis() - lastSavedTrainNumbersTime) < 30 * 1000) {
            return; // save only every 30 seconds (or less)
        }
        lastSavedTrainNumbersTime = System.currentTimeMillis();
        StringBuilder state = new StringBuilder();
        Collections.sort(panelElements);
        for (PanelElement pe : panelElements) {
            if (pe.isSensor()) {
                state.append(pe.getAdr());
                state.append(" ");
                state.append(pe.getTrain());
                state.append(";");
            }
        }
        final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        prefs.put("trainNumbers", state.toString());
    }

    private void loadTrainNumbers() {
        final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        String trainState = prefs.get("trainNumbers", "");
        if (trainState.isEmpty()) {
            return;
        }

        info("read previous state=" + trainState);
        String[] states = trainState.split(";");
        for (String s : states) {
            String[] keyValue = s.split(" ");
            if (keyValue.length == 2) {
                try {
                    int addr = Integer.parseInt(keyValue[0]);
                    int data = Integer.parseInt(keyValue[1]);
                    PanelElement.setTrain(addr, data);
                } catch (NumberFormatException e) {
                    // invalid data
                }
            }
        }
    }

    private static void initConnectivityCheck() {
        // init timer for connectivity check (if not in simulation)
        // this program is shutdown, if there is no connection for 10 seconds
        info("initConnectivityCheck");
        lastConnected = System.currentTimeMillis();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // error("conn check");
                if ((System.currentTimeMillis() - lastConnected) >= 10 * 1000) {
                    error("lost connection.");
                    running = false;  // finish all threads
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        error("ERROR" + ex.getMessage());
                    }
                    error("SX4 shutdown.");
                    System.exit(1);
                }
            }
        }, 10 * 1000, 1000);
    }

    private static GenericSXInterface initSXInterface(String port, int baud) {
        GenericSXInterface sxInterface = null;
        if (simulation) {
            sxInterface = new SimulationInterface();
            info("simulation");
            // switch on power for simulation
            SXData.setActualPower(true);
            // no connectivityCheck for simulation
        } else if (ifType.contains("FCC")) { // fcc has different interface handling ! 
            sxInterface = new FCCInterface(port);
            initConnectivityCheck();
            info("FCC mode=" + sxi.getMode());

        } else if (ifType.contains("SLX825")) {
            //portName = "/dev/ttyUSB825";
            sxInterface = new SLX825Interface(port, baud);
            info("SLX825 interface");
            //TODO initConnectivityCheck();
        }
        return sxInterface;

    }

    private static boolean isDebugFlagSet(String[] args) {
        for (String s : args) {
            //System.out.println(s);
            if (s.equals("-d") || s.equals("--debug")) {
                return true;
            }
        }
        return false;
    }

    private static void deleteOlderLogfiles() {
        File curDir = new File(".");
        ArrayList<File> logFiles = new ArrayList<>();
        File[] filesList = curDir.listFiles();

        for (File f : filesList) {
            if (f.isFile() && f.getName().startsWith("log") && f.getName().endsWith(".txt")) {
                logFiles.add(f);
            }
        }
        Collections.sort(logFiles);
        if (!logFiles.isEmpty() && (logFiles.size() > NUMBER_OF_FILES_TO_RETAIN)) {
            for (int i = 0; i < (logFiles.size() - NUMBER_OF_FILES_TO_RETAIN); i++) {
                //debug("deleting old log file" + logFiles.get(i).getName());
                logFiles.get(i).delete();
            }
        }
    }

    private static void startLogging() {
        // start simple logging
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currDateTime = df.format(new Date());
        String logFileName = "log." + currDateTime + ".txt";
        setLogger(new MyLogger(logFileName));
        info("starting " + VERSION);
        info("datetime=" + currDateTime);
        deleteOlderLogfiles();

    }
}
