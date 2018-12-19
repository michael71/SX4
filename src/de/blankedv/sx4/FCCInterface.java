/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import static de.blankedv.sx4.Constants.*;
import static de.blankedv.sx4.SX4.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import purejavacomm.CommPortIdentifier;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;
import purejavacomm.UnsupportedCommOperationException;
import static com.esotericsoftware.minlog.Log.*;

/**
 *
 * @author mblank
 */
public class FCCInterface extends GenericSXInterface {

    private String portName;

    CommPortIdentifier serialPortId;
    Enumeration enumComm;
    SerialPort serialPort;
    OutputStream outputStream;
    InputStream inputStream;
    Boolean serialPortGeoeffnet = false;

    Boolean regFeedback = false;
    int regFeedbackAdr = 0;

    private static int fccErrorCount = 0;

    private int lastPowerState = INVALID_INT;

    FCCInterface(String port) {
        this.portName = port;
    }

    @Override
    public boolean open() {
        Boolean foundPort = false;
        if (serialPortGeoeffnet != false) {
            error("Serialport bereits geöffnet");
            return false;
        }
        info("Öffne Serialport " + portName);
        enumComm = CommPortIdentifier.getPortIdentifiers();
        while (enumComm.hasMoreElements()) {
            serialPortId = (CommPortIdentifier) enumComm.nextElement();
            if (portName.contentEquals(serialPortId.getName())) {
                foundPort = true;
                break;
            }
        }
        if (foundPort != true) {
            error("Serialport nicht gefunden: " + portName);
            return false;
        }
        try {
            serialPort = (SerialPort) serialPortId.open("Öffnen und Senden", 500);
        } catch (PortInUseException e) {
            error("Port belegt");
            return false;
        }
        try {
            outputStream = serialPort.getOutputStream();
        } catch (IOException e) {
            error("Keinen Zugriff auf OutputStream");
            return false;
        }

        try {
            inputStream = serialPort.getInputStream();
            while (inputStream.available() >= 1) {  // empty.
                int b = inputStream.read();
            }
        } catch (IOException e) {
            error("Keinen Zugriff auf InputStream");
            return false;
        }

        try {
            serialPort.setSerialPortParams(230400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            // fixed baudrate for FCC
        } catch (UnsupportedCommOperationException e) {
            error("Konnte Schnittstellen-Paramter nicht setzen");
            return false;
        }

        serialPortGeoeffnet = true;
        connected = true;

        return true;
    }

    @Override
    public void close() {
        if (serialPortGeoeffnet == true) {
            debug("Schließe Serialport");
            serialPort.close();
            serialPortGeoeffnet = false;
        } else {
            //error("Serialport bereits geschlossen");
        }
        fccErrorCount = 0; // reset errors
        connected = false;
    }

    /*
    send update (if necessary) for power and sx-channels to FCC
    then read all channels from FCC
    (this routine is run > 3 times a second)
     */
    @Override
    public String doSendUpdate() {
        if (fccErrorCount > 10) {
            error("ERROR: Keine Response von der FCC, SerialPort Settings überprüfen");
            return ("ERROR: Keine Response von der FCC, SerialPort Settings überprüfen");

        }
        if (serialPortGeoeffnet) {
            lastConnected = System.currentTimeMillis();
            //error(lastConnected);
            try {  // empty input
                while (inputStream.available() >= 1) {
                    int b = inputStream.read();
                }
            } catch (IOException ex) {
                ;
            }
            if ((powerToBe.get() != INVALID_INT) && (powerToBe.get() != lastPowerState)) {
                //error("powertoBe="+powerToBe.get()+" SXD.getPower()="+SXData.getPower());
                sendPower();
            }
            while (!dataToSend.isEmpty()) {
                IntegerPair sxd = dataToSend.poll();
                if (sxd != null) {
                    sendWrite(sxd);
                }

            }
            connectionOK = true;
        }
        return "";
    }

    /*
    send update (if necessary) for power and sx-channels to FCC
    then read all channels from FCC
    (this routine is run > 3 times a second)
     */
    @Override
    public String doUpdate() {

        if (fccErrorCount > 10) {
            error("ERROR: Keine Response von der FCC, SerialPort Settings überprüfen");
            return ("ERROR: Keine Response von der FCC, SerialPort Settings überprüfen");

        }
        if (serialPortGeoeffnet) {
            lastConnected = System.currentTimeMillis();
            //error(lastConnected);
            try {  // empty input
                while (inputStream.available() >= 1) {
                    int b = inputStream.read();
                }
            } catch (IOException ex) {
                ;
            }
            if ((powerToBe.get() != INVALID_INT) && (powerToBe.get() != lastPowerState)) {
                //error("powertoBe="+powerToBe.get()+" SXD.getPower()="+SXData.getPower());
                sendPower();
            }
            while (!dataToSend.isEmpty()) {
                IntegerPair sxd = dataToSend.poll();
                if (sxd != null) {
                    sendWrite(sxd);
                }
            }
            try {
                // request block of SX0 / SX1 bus data
                Byte[] b = {0x78, 0x03};
                outputStream.write(b[0]);
                outputStream.write(b[1]);
                outputStream.flush();
            } catch (IOException ex) {
                error("ERROR: Serial-IO where trying to write");
                fccErrorCount++;
            }
            shortSleep();  // must wait 40 milliseconds to have all 226 channels received
            try {
                //int count = 0;  // byte numbering in FCC-manual starts with 1 !
                // but for consistency with sxData array we start with 0 here

                byte[] buf = new byte[226];  // in case w
                int nread = inputStream.read(buf, 0, 226);

                if (nread != 226) {
                    error("ERROR wrong number of bytes read=" + nread);
                    fccErrorCount++;
                    return "ERROR";
                } else {
                    fccErrorCount = 0;
                    //error("226 bytes gelesen");
                }

                for (int count = 0; count < 226; count++) {

                    if (count < SXMAX_USED) {
                        if ((buf[count] & 0xff) != SXData.get(count)) {
                            SXData.update(count, (buf[count] & 0xff), false);
                        }
                    } else if (count == 112) {
                        //error("power="+buf[count]);
                        if (buf[count] == 0) {
                            SXData.setPower(0, false);
                            lastPowerState = 0;
                            //error("FCC power is off");
                        } else {
                            SXData.setPower(1, false);
                            lastPowerState = 1;
                            //error("FCC power is on");
                        }
                    } // ignore SX1 data

                }
                connectionOK = true;

            } catch (IOException ex) {
                error("ERROR: Serial-IO where trying to read");
                fccErrorCount++;
            }
        }

        return "";
    }

    private void shortSleep() {
        try {
            Thread.sleep(40);
        } catch (InterruptedException ex) {
            error("FCC " + ex.getMessage());
        }
    }

    private void veryShortSleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            error("FCC " + ex.getMessage());
        }
    }

    @Override
    public String getMode() {
        if (!connected) {
            return "-";
        }

        // siehe FCC Interface Manual, Seite 7
        switch (SXData.get(110) & 0x0f) {
            case 0x00:
                return "nur SX1";
            case 0x02:
                return "SX1+SX2";
            case 0x04:
                return "SX1,SX2,DCC";
            case 0x05:
                return "SX1 + SX2 + MM";
            case 0x06:
                return "nur DCC";
            case 0x07:
                return "nur MM";
            case 0x0b:
                return "SX1,SX2,DCC,MM";
            default:
                return "check FCC mode";
        }
    }

    @Override
    public void registerFeedback(int adr
    ) {
        // not necessary, because it is polled every second
    }

    // für alle Schreibbefehle and die FCC muss zusätzlich zur Kanalnummer
    // das höchste Bit auf 1 gesetzt werden
    //Gleisspannung ein (SX1/2-Bus 0):
    //Vom PC: 0x00 0xFF Ungleich 0x00 Zum PC: 0x00
    //Gleisspannung aus (SX1/2-Bus 0):
    //Vom PC: 0x00 0xFF Gleich 0x00 Zum PC: 0x00
    private void sendPower() {
        Byte[] b = {(byte) 0x00, (byte) 0xFF, (byte) 0x00};
        if (powerToBe.get() != 0) {
            debug("FCC: switchPowerOn");
            b[2] = (byte) 0x01;
        } else {
            debug("FCC: switchPowerOff");
            b[2] = (byte) 0x00;
        }

        try {
            outputStream.write(b[0]);
            outputStream.write(b[1]);
            outputStream.write(b[2]);
            outputStream.flush();
        } catch (IOException e) {
            error("Error: Serial Fehler beim Senden");
        }
        veryShortSleep();
        try {
            int result = inputStream.read();
            if (result != 0) {
                error("Error: Serial Fehler beim Empfangen");
            }
        } catch (IOException ex) {
            error("Error: Serial Fehler beim Empfangen");
        }
    }

    @Override
    public void requestPower() {
        // not necessary, because it is polled every second
        connectionOK = true;
    }

    /**
     * für alle Schreibbefehle an die FCC muss zusätzlich zur Kanalnummer das
     * höchste Bit auf 1 gesetzt werden
     */
    private boolean sendWrite(IntegerPair sxd) {
        int addr = sxd.addr;
        int data = sxd.data;

        if (addr > SXMAX_USED) {
            error("ERROR: SX addr invalid addr=" + addr);
            return false;
        }
        try {
            outputStream.write((byte) 0);  // BUS SX0
            outputStream.write((byte) (addr + 0x80)); // set highest bit for writing
            outputStream.write((byte) data);
            outputStream.flush();
            // done via polling in LanbahnUI // doLanbahnUpdate((byte)(data[0] & 0x7f), data[1]);
        } catch (IOException e) {
            error("Fehler beim Senden");
            return false;
        }
        veryShortSleep();
        // quittung abwarten
        try {
            int result = inputStream.read();
            if (result != 0) {
                error("Error: Serial Fehler beim Empfangen");
                return false;
            } else {
                return true;
            }
        } catch (IOException ex) {
            error("Error: Serial Fehler beim Empfangen");
            return false;
        }

    }

    @Override
    public void request(int addr) {
        // not necessary because of regular update from FCC
    }

}
