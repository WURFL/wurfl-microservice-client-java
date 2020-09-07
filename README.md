# ScientiaMobile WURFL Microservice Client for Java

WURFL Microservice (by ScientiaMobile, Inc.) is a mobile device detection service that can quickly and accurately detect over 500 capabilities of visiting devices. It can differentiate between portable mobile devices, desktop devices, SmartTVs and any other types of devices that have a web browser.

This is the Java Client API for accessing the WURFL Microservice. The API is released under Open-Source and can be integrated with other open-source or proprietary code. In order to operate, it requires access to a running instance of the WURFL Microservice product, such as:

- WURFL Microservice for Docker: https://www.scientiamobile.com/products/wurfl-microservice-docker-detect-device/

- WURFL Microservice for AWS: https://www.scientiamobile.com/products/wurfl-device-detection-microservice-aws/ 

- WURFL Microservice for Azure: https://www.scientiamobile.com/products/wurfl-microservice-for-azure/

- WURFL Microservice for Google Cloud Platform: https://www.scientiamobile.com/products/wurfl-microservice-for-gcp/

Java implementation of the WM Client api.
Requires Java 7 or above (only version 2.0.0 is compatible with Java 6)

The Example project contains an example of client api usage for a console application :


```java
package com.scientiamobile.wmclient.example;

import com.scientiamobile.wurfl.wmclient.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import static java.lang.System.out;

public class Example {

    public static void main(String[] args) {


        try {
            // First we need to create a WM client instance, to connect to our WM server API at the specified host and port.
            WmClient client = WmClient.create("http", "localhost", "8080", "");
            // We ask Wm server API for some Wm server info such as server API version and info about WURFL API and file used by WM server.
            Model.JSONInfoData info = client.getInfo();
            out.println("Printing WM server information");
            out.println("WURFL API version: " + info.getWurflApiVersion());
            out.println("WM server version:  " + info.getWmVersion());
            out.println("Wurfl file info: " + info.getWurflInfo());

            String ua = "Mozilla/5.0 (Linux; Android 7.1.1; ONEPLUS A5000 Build/NMF26X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Mobile Safari/537.36";

            // By setting the cache size we are also activating the caching option in WM client. In order to not use cache, you just to need to omit setCacheSize call
            client.setCacheSize(100000);

            // set the capabilities we want to receive from WM server
            client.setRequestedStaticCapabilities(new String[]{"brand_name", "model_name"});
            client.setRequestedVirtualCapabilities(new String[]{"is_smartphone", "form_factor"});

            out.println();
            out.println("Detecting device for user-agent: " + ua);

            // Perform a device detection calling WM server API
            Model.JSONDeviceData device = client.lookupUseragent(ua);
            // Applicative error, ie: invalid input provided
            if (device.error != null && device.error.length() > 0) {
                out.println("An error occurred: " + device.error);
            } else {
                // Let's get the device capabilities and print some of them
                Map<String, String> capabilities = device.capabilities;
                out.println("Detected device WURFL ID: " + capabilities.get("wurfl_id"));
                out.println("Device brand & model: " + capabilities.get("brand_name") + " " + capabilities.get("model_name"));
                out.println("Detected device form factor: " + capabilities.get("form_factor"));
                if (capabilities.get("is_smartphone").equals("true")) {
                    out.println("This is a smartphone");
                }

                // Iterate over all the device capabilities and print them
                out.println("All received capabilities");
                Iterator<String> it = capabilities.keySet().iterator();
                while (it.hasNext()) {
                    String k = it.next();
                    out.println(k + ": " + capabilities.get(k));
                }
            }

            // Get all the device manufacturers, and print the first twenty
            int limit = 20;
            String[] deviceMakes = client.getAllDeviceMakes();
            out.printf("Print the first %d Brand of %d retrieved from server\n", limit, deviceMakes.length);

            // Sort the device manufacturer names
            Arrays.sort(deviceMakes);
            for (int i = 0; i < limit; i++) {
                out.printf(" - %s\n", deviceMakes[i]);
            }

            // Now call the WM server to get all device model and marketing names produced by Apple
            out.println("Print all Model for the Apple Brand");
            Model.JSONModelMktName[] devNames = client.getAllDevicesForMake("Apple");

            // Sort ModelMktName objects by their model name
            Arrays.sort(devNames, new ByModelNameComparer());

            for (Model.JSONModelMktName modelMktName : devNames) {
                out.printf(" - %s %s\n", modelMktName.modelName, modelMktName.marketingName);
            }

            // Now call the WM server to get all operative system names
            out.println("Print the list of OSes");
            String[] oses = client.getAllOSes();
            // Sort and print all OS names
            Arrays.sort(oses);
            for (String os : oses) {
                out.printf(" - %s\n", os);
            }

            // Let's call the WM server to get all version of the Android OS
            out.println("Print all versions for the Android OS");
            String[] osVersions = client.getAllVersionsForOS("Android");
            // Sort all Android version numbers and print them.
            Arrays.sort(osVersions);
            for (String ver : osVersions) {
                out.printf(" - %s\n", ver);
            }
            // Cleans all client resources. Any call on client API methods after this one will throw a WmException
            client.destroyConnection();
        } catch (WmException e) {
            // problems such as network errors  or internal server problems
            out.println("An error has occurred: " + e.getMessage());
            e.printStackTrace();
        }

    }
}


// Comparator used to sort JSONModelMktName objects according to their model name property, for which is used the String natural ordering.
class ByModelNameComparer implements Comparator<Model.JSONModelMktName> {

    @Override
    public int compare(Model.JSONModelMktName o1, Model.JSONModelMktName o2) {

        if (o1 == null && o2 == null) {
            return 0;
        }

        if (o1 == null && o2 != null) {
            return 1;
        }

        if (o1 != null && o2 == null) {
            return -1;
        }

        return o1.modelName.compareTo(o2.modelName);
    }
}
```
