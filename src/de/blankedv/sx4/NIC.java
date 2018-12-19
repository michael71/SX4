package de.blankedv.sx4;

import java.net.*;
import java.util.*;
import static com.esotericsoftware.minlog.Log.*;

class NIC {

    public static List<InetAddress> getmyip() {

        List<InetAddress> addrList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            error("ERROR: no network interfaces found.");
            return null;
        }

        InetAddress localhost = null;

        try {
            localhost = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            error("ERROR: could not determine ip of 'localhost'");
            return null;
        }

        while (interfaces.hasMoreElements()) {
            NetworkInterface ifc = interfaces.nextElement();
            //if (DEBUG) error("Network Interface="+ifc.getName());
            Enumeration<InetAddress> addressesOfAnInterface = ifc.getInetAddresses();

            while (addressesOfAnInterface.hasMoreElements()) {
                InetAddress address = addressesOfAnInterface.nextElement();
                //if (DEBUG) error("has address=" + address.getHostAddress());
                // look for IPv4 addresses which are not==127.0.0.1
                if (!address.equals(localhost) && !address.toString().contains(":")
                        && (!ifc.getName().toString().contains("vir"))
                        && (!ifc.getName().toString().contains("lxc"))) {
                    addrList.add(address);
                    debug("found IP to use (not local, not ipv6, not virtual): " + address.getHostAddress());

                    //	error("FOUND ADDRESS ON NIC: " + address.getHostAddress());

                }
            }
        }
        return addrList;
    }
}
