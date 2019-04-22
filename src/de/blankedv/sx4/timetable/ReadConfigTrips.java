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

import static com.esotericsoftware.minlog.Log.debug;
import static com.esotericsoftware.minlog.Log.error;
import static de.blankedv.sx4.Constants.*;
import de.blankedv.sx4.SXUtils;
import static de.blankedv.sx4.timetable.Vars.*;
import static de.blankedv.sx4.timetable.VarsFX.allTimetables;
import static de.blankedv.sx4.timetable.VarsFX.allTrips;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * utility function for the mapping of lanbahn addresses to DCC addresses (and
 * bits) and vice versa
 *
 * @author mblank
 */
public class ReadConfigTrips {

    public static String readTripsAndTimetables(String fname) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            error("ParserConfigException Exception - " + e1.getMessage());
            return "ParserConfigException";
        }
        Document doc;
        try {
            doc = builder.parse(new File(fname));
            parseTripsAndTimetable(doc);
            // sort the trips by ID
            Collections.sort(allTrips, (a, b) -> b.compareTo(a));

        } catch (SAXException e) {
            error("SAX Exception - " + e.getMessage());
            return "SAX Exception - " + e.getMessage();
        } catch (IOException e) {
            error("IO Exception - " + e.getMessage());
            return "IO Exception - " + e.getMessage();
        } catch (Exception e) {
            error("other Exception - " + e.getMessage());
            return "other Exception - " + e.getMessage();
        }

        return "OK";
    }

    // code template from lanbahnPanel
    private static void parseTripsAndTimetable(Document doc) {

        allTrips.clear();
        allTimetables.clear();

        NodeList items;
        Element root = doc.getDocumentElement();

        items = root.getElementsByTagName("trip");

        debug("config: " + items.getLength() + " trips");

        for (int i = 0; i < items.getLength(); i++) {
            Trip tr = parseTrip(items.item(i));
            if (tr != null) {
                if (CFG_DEBUG) {
                    debug("trip adr=" + tr.adr);
                }
                allTrips.add(tr);
            }
        }

        items = root.getElementsByTagName("timetable");

        debug("config: " + items.getLength() + " timetables");

        for (int i = 0; i < items.getLength(); i++) {
            Timetable ti = parseTimetable(items.item(i));
            if (ti != null) {
                if (CFG_DEBUG) {
                    debug("timetable adr=" + ti.getAdr());
                }
                allTimetables.add(ti);
            }
        }

    }

    // code from lanbahnPanel
    private static int getIntValueOfNode(Node a) {
        return Integer.parseInt(a.getNodeValue());
    }

    private static Trip parseTrip(Node item) {

        Trip t = new Trip();

        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);
            switch (theAttribute.getNodeName()) {
                case "adr":
                    t.adr = getIntValueOfNode(theAttribute);
                    break;
                case "route":
                case "routeid":
                    t.route = getIntValueOfNode(theAttribute);
                    break;
                case "sens1":
                    t.sens1 = getIntValueOfNode(theAttribute);
                    break;
                case "sens2":
                    t.sens2 = getIntValueOfNode(theAttribute);
                    break;
                case "loco":
                    t.locoString = theAttribute.getNodeValue();
                    break;
                case "startdelay":
                    t.startDelay = getIntValueOfNode(theAttribute);
                    break;
                case "stopdelay":
                    t.stopDelay = getIntValueOfNode(theAttribute);
                    break;
                default:
                    break;
            }
        }

        // check if Trip information is complete
        if ((t.adr != INVALID_INT)
                && (t.route != INVALID_INT)
                && (t.sens1 != INVALID_INT)
                && (t.sens2 != INVALID_INT)
                && (t.convertLocoData())) {
            // we have the minimum info needed

            if (t.startDelay == INVALID_INT) {
                t.startDelay = 0;
            }
            if (t.stopDelay == INVALID_INT) {
                t.stopDelay = 0;
            }

            return t;
        } else {
            error("invalid trip, adr=" + t.adr);
            return null;
        }
    }

    private static Timetable parseTimetable(Node item) {

        int adr = INVALID_INT;
        String sTrip = "";
        String name = "";

        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);
            switch (theAttribute.getNodeName()) {
                case "adr":
                    adr = getIntValueOfNode(theAttribute);
                    break;
                case "trip":
                    sTrip = theAttribute.getNodeValue();
                    break;
                case "name":
                    name = theAttribute.getNodeValue();
                    break;
                default:
                    break;
            }
        }

        // check if Trip information is complete
        if ((adr != INVALID_INT) && (!sTrip.isEmpty())) {
            // we have the minimum info needed
            return new Timetable(adr, sTrip, name);
        } else {
            error("invalid Timetable, adr=" + adr);
            return null;
        }
    }

}
