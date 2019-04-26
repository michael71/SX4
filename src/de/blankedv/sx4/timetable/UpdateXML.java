/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4.timetable;

import static com.esotericsoftware.minlog.Log.debug;
import static com.esotericsoftware.minlog.Log.error;
import static de.blankedv.sx4.SX4.configFilename;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author mblank
 */
public class UpdateXML {

    public static boolean setTripDelay(int tripAdr, int value, boolean isStart) {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        Document doc;
        try {
            docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.parse(configFilename);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            error(ex.getMessage());
            return false;
        }

        NodeList items;
        Element root = doc.getDocumentElement();

        items = root.getElementsByTagName("trip");
        debug("config: " + items.getLength() + " trips");

        Node toChange = null;

        for (int i = 0; i < items.getLength(); i++) {
            int a = getTripAddr(items.item(i));
            if (a == tripAdr) {
                toChange = items.item(i);
                break;
            }
        }

        if (toChange == null) {
            return false;
        }

        NamedNodeMap attr = toChange.getAttributes();
        Node nodeAttr = null;
        if (isStart) {
          nodeAttr = attr.getNamedItem("startdelay");
        } else {
            nodeAttr = attr.getNamedItem("stopdelay");
        }
        if (nodeAttr == null) return false;
        
        nodeAttr.setTextContent("" + value);

        /**
         * write it back to the xml
         */
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(configFilename));
            transformer.transform(source, result);

        } catch (TransformerException ex) {
            error(ex.getMessage());
            return false;
        }

        debug("changed node.");
        return true;
    }

    public static boolean setLocoString(int tripAdr, String value) {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        Document doc;
        try {
            docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.parse(configFilename);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            error(ex.getMessage());
            return false;
        }

        NodeList items;
        Element root = doc.getDocumentElement();

        items = root.getElementsByTagName("trip");
        debug("config: " + items.getLength() + " trips");

        Node toChange = null;

        for (int i = 0; i < items.getLength(); i++) {
            int a = getTripAddr(items.item(i));
            if (a == tripAdr) {
                toChange = items.item(i);
                break;
            }
        }

        if (toChange == null) {
            return false;
        }

        NamedNodeMap attr = toChange.getAttributes();
        Node nodeAttr = attr.getNamedItem("loco");
        
        if (nodeAttr == null) return false;
        
        nodeAttr.setTextContent(value);

        /**
         * write it back to the xml
         */
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(configFilename));
            transformer.transform(source, result);

        } catch (TransformerException ex) {
            error(ex.getMessage());
            return false;
        }

        debug("changed node.");
        return true;
    }

    private static int getTripAddr(Node item) {

        Trip t = new Trip();

        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);
            switch (theAttribute.getNodeName()) {
                case "adr":
                    return Integer.parseInt(theAttribute.getNodeValue());
               
                default:
                    break;
            }
        }
        return 0;

    }
}
