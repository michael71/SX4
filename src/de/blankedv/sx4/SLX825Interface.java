/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import static de.blankedv.sx4.Constants.*;
import static de.blankedv.sx4.SX4.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import purejavacomm.CommPortIdentifier;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;
import purejavacomm.UnsupportedCommOperationException;
import static com.esotericsoftware.minlog.Log.*;

/**
 *
 * @author mblank
 *
 * TODO - output auf standard thread bringen
 *
 */
public class SLX825Interface extends GenericSXInterface {

    private String portName;

    private final int baudrate;
    CommPortIdentifier serialPortId;
    Enumeration enumComm;
    SerialPort serialPort;
    OutputStream outputStream;
    InputStream inputStream;
    Boolean serialPortGeoeffnet = false;
    private int lastAdrSent = -1;

    private static int leftover;
    private static boolean leftoverFlag = false;

    Boolean regFeedback = false;
    int regFeedbackAdr = 0;
    private int interfaceActiveCount = 0;
    private final int POWER_CHAN = 127;
    private boolean firstDoUpdateCall = true;
    private boolean lastPowerState = false;

    public SLX825Interface(String portName, int baud) {

        this.portName = portName;
        this.baudrate = baud;
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
            //error("port: "+serialPortId.getName());
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
            serialPort = (SerialPort) serialPortId.open("Öffnen und Senden", 10);
        } catch (PortInUseException e) {
            error("Port belegt");
        }
        try {
            outputStream = serialPort.getOutputStream();
        } catch (IOException e) {
            error("Keinen Zugriff auf OutputStream");
        }

        try {
            leftoverFlag = false;
            inputStream = serialPort.getInputStream();
        } catch (IOException e) {
            error("Keinen Zugriff auf InputStream");
        }

        try {
            serialPort.setSerialPortParams(baudrate,
                    SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
        } catch (UnsupportedCommOperationException e) {
            error("Konnte Schnittstellen-Paramter nicht setzen");
        }
        serialPortGeoeffnet = true;
        connected = true;

        setInterfaceMode();   // start "Rautenhaus mode"

        return true;
    }

    @Override
    public void close() {
        if (serialPortGeoeffnet == true) {
            error("Schließe Serialport");
            serialPort.close();
            serialPortGeoeffnet = false;
        } else {
            error("Serialport bereits geschlossen");
        }
        connected = false;
    }

    private synchronized boolean sendToInterface(int addr, int data) {
        // darf nicht unterbrochen werden    
        Byte[] b = {0, 0};

        if (data == INVALID_INT) {
            // this is a READ
            b[0] = (byte) (addr);
            b[1] = (byte) 0;
        } else {
            // this is a WRITE
            b[0] = (byte) (addr + 0x80);
            b[1] = (byte) data;
            if ((addr == POWER_CHAN) && (data != 0)) {
                data = 0x80;
                b[1] = (byte) data;
            }
        }

        if (serialPortGeoeffnet != true) {
            error("Fehler beim Senden, serial port nicht geöffnet.");
            return false;
        }

        if (DEBUG) {
            if ((b[0] & 0x80) != 0) {
                debug("wr-Cmd: adr=" + addr + ", data=" + (int) b[1]);
            } else {
                debug("rd-Cmd: adr=" + addr);
            }
        }

        try {
            outputStream.write(b[0]);
            outputStream.write(b[1]);
            outputStream.flush();
        } catch (IOException e) {
            error("Fehler beim Senden");
            return false;
        }
        return true;

    }

    public synchronized boolean send(Byte[] data) {
        // darf nicht unterbrochen werden     

        if (serialPortGeoeffnet != true) {
            error("Fehler beim Senden, serial port nicht geöffnet und simul. nicht gesetzt");
            return false;
        }

        try {

            outputStream.write(data[0]);
            outputStream.write(data[1]);
            outputStream.flush();

        } catch (IOException e) {
            error("Fehler beim Senden");
            return false;
        }
        return true;

    }

    private void setInterfaceMode() {

        Byte[] b = {(byte) 0xFF, (byte) 0xFF};
        /* Falls SLX825:
        Über einen Schreibbefehl auf die Adresse 126 (=0xFE schreiben, =0x7E lesen
        können folgende Funktionen angewählt werden:

        Bit 7 = 1 (128)
        Überwachung „Ein“
        Hiermit wird das Rautenhaus-Befehlsformat eingeschaltet
        Diese Ausgabe löst jedes Mal den einmaligen Transfer der gesamten Datenbusinformation vom Interface
        zum Computer aus. Jede Änderung auf dem Datenbus wird automatisch sofort nach Erkennen an den Rechner
        geschickt. Im ersten Byte steht die Adresse, im zweiten Byte das zugehörige Datenwort. Das oberste Bit
        im Adressbyte kennzeichnet den Datenbus, bei dem die Änderung auftrat. Beim SLX825 mit nur einem
        Datenbus ist das Bit immer 0.

        Bit 6 = 1 (64)
        Überwachung „Aus“
        Das Rautenhaus-Befehlsformat wird ausgeschaltet.

        Bit 5 = 1 (32)
        Feedback „Ein“
        Bei Überwachung „Ein“ wird auch dann eine Änderung übermittelt, wenn die Änderung vom Rechner
        selbst über eine Ausgabe an das Interface ausgelöst wurde.

        Bit 4 = 1 (16)
        Feedback „Aus“

        Die Lesebefehle unterscheiden sich nicht von den Lesebefehlen im Trix-Format.
        * ******************************************************************
        * Falls ZS1/ZS" ebenfalls 128+32 setzen
         * */

        b[0] = (byte) 0xFE; // Rautenhaus ein
        b[1] = (byte) 0xA0; // Feedback ein

        try {
            outputStream.write(b[0]);
            outputStream.write(b[1]);
            outputStream.flush();
        } catch (IOException e) {
            error("Fehler beim Senden");
        }
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
        }
    }

    private void sendPower() {
        if (serialPortGeoeffnet && SXData.isPowerControlEnabled()) {

            if (SXData.isPowerToBe()) {
                debug("SLX825: switchPowerOn");
                //sendToInterface(POWER_CHAN, 1);
                switchPowerOn();
            } else {
                debug("SLX825: switchPowerOff");
                //sendToInterface(POWER_CHAN, 0);
                switchPowerOff();
            }
        }
    }

    public synchronized void switchPowerOff() {
        // 127 (ZE ein/aus) +128(schreiben) = 0x00       
        Byte[] b = {(byte) 0xFF, (byte) 0x00};
        try {
            outputStream.write(b[0]);
            outputStream.write(b[1]);
            outputStream.flush();
        } catch (IOException e) {
            error("Fehler beim Senden");
        }

    }

    public synchronized void switchPowerOn() {
        // 127 (ZE ein/aus) +128(schreiben) = 0xFF   
        Byte[] b = {(byte) 0xFF, (byte) 0x80};
        try {
            outputStream.write(b[0]);
            outputStream.write(b[1]);
            outputStream.flush();
        } catch (IOException e) {
            error("Fehler beim Senden");
        }
    }

    @Override
    public void requestPower() {
        if (serialPortGeoeffnet) {
            //dataToSend.add(new IntegerPair(POWER_CHAN, INVALID_INT));
            //sendToInterface(POWER_CHAN, INVALID_INT);
            dataToSend.add(new IntegerPair(POWER_CHAN, INVALID_INT));
            debug("requestPower sent");

        }

    }

    public synchronized void readPower() {
        Byte[] b = {(byte) 127, (byte) 0x00};   // read power state
        send(b);
    }

    @Override
    public void request(int addr) {
        if (serialPortGeoeffnet) {
            dataToSend.add(new IntegerPair(addr, INVALID_INT));
        }

    }

    @Override
    public String doUpdate() {
        if (serialPortGeoeffnet) {
            readSerialPortAndUpdateSXData();
            if (SXData.isPowerToBe() != lastPowerState) {
                //error("powertoBe="+powerToBe.get()+" SXD.getPower()="+SXData.getPower());
                sendPower();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SLX825Interface.class.getName()).log(Level.SEVERE, null, ex);
                }
                readSerialPortAndUpdateSXData();
            }
            while (!dataToSend.isEmpty()) {
                IntegerPair sxd = dataToSend.poll();
                if (sxd != null) {
                    sendToInterface(sxd.addr, sxd.data);
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SLX825Interface.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    readSerialPortAndUpdateSXData();
                }
            }
            if (interfaceActiveCount > 10) {
                interfaceActiveCount = 0;
                requestPower();
            }
            interfaceActiveCount++;
        }

        return "";
    }

    private void readSerialPortAndUpdateSXData() {

        // Achtung: immer auf 2 Byte warten .... TODO: timer reset wenn länger als 10 ms keine Bytes
        try {
            int adr, data;

            byte[] readBuffer = new byte[500];
            lastConnected = System.currentTimeMillis();
            while (inputStream.available() > 1) {

                int numBytes = inputStream.read(readBuffer);

                debug("read n=" + numBytes);

                int offset;
                if (leftoverFlag) {
                    offset = 1;
                    data = (int) (readBuffer[0] & 0xFF);
                    SXData.update(leftover, data, false); // DO NOT SEND BACK TO SXI (loop !)
                } else {
                    offset = 0;
                }
                for (int i = offset; i < numBytes; i = i + 2) {
                    adr = (int) (readBuffer[0 + i] & 0xFF);
                    if ((i + 1) < numBytes) {
                        data = (int) (readBuffer[1 + i] & 0xFF);
                        if ((adr == 0) && (data == 0)) {
                            // ignore, this seems to be a bug in purejavacomm
                        } else if (adr == POWER_CHAN) {  // power channel
                            if (data != 0) {
                                SXData.setActualPower(true);
                                lastPowerState = true;
                            } else {
                                SXData.setActualPower(false);
                                lastPowerState = false;
                            }
                            debug("rec. power=" + data);
                        } else {
                            SXData.update(adr, data, false); // DO NOT SEND BACK TO SXI (loop !)
                            debug("read a=" + adr + " d=" + data);
                        }
                        leftoverFlag = false;

                    } else {
                        // leftover data, no even number of data sent
                        // use next time
                        leftover = adr;
                        leftoverFlag = true;
                    }
                }

            }

        } catch (IOException e) {
            error("Fehler beim Lesen empfangener Daten");
        }

    }

    public static int toUnsignedInt(byte value) {
        return (value & 0x7F) + (value < 0 ? 128 : 0);
    }

}
