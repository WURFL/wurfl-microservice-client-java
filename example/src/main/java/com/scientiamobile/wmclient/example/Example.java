/*
Copyright 2020 ScientiaMobile Inc. http://www.scientiamobile.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.scientiamobile.wmclient.example;

import com.scientiamobile.wurfl.wmclient.*;

import java.util.*;

import static java.lang.System.exit;
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

            // Perform a device detection calling WM server API, using only the user agent
            //Model.JSONDeviceData device = client.lookupUseragent(ua);

            // Perform a device detection calling WM server API, using a full HTTP request header map (there's also a lookupRequest(HttpServletRequest req)
            // that you may use when running a WM client inside a web application on an application server like tomcat/glassfish/jboss, etc.
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Accept-Encoding", "gzip, deflate");
            headers.put("Accept", "text/html, application/xml;q=0.9, application/xhtml+xml, image/png, image/webp, image/jpeg, image/gif, image/x-xbitmap, */*;q=0.1");
            headers.put("Accept-Language", "en");
            headers.put("Device-Stock-Ua", ua);
            headers.put("Forwarded", "for=\"110.54.224.195:36350\"");
            headers.put("Save-Data", "on");
            headers.put("Referer", "https://www.cram.com/flashcards/labor-and-delivery-questions-889210");
            headers.put("User-Agent", "Opera/9.80 (Android; Opera Mini/51.0.2254/184.121; U; en) Presto/2.12.423 Version/12.16");
            headers.put("X-Clacks-Overhead", "GNU ph");
            headers.put("X-Forwarded-For", "110.54.224.195, 82.145.210.235");
            headers.put("X-Operamini-Features", "advanced, camera, download, file_system, folding, httpping, pingback, routing, touch, viewport");
            headers.put("X-Operamini-Phone", "Android #");
            Model.JSONDeviceData device = client.lookupHeaders(headers);

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
                for (String k : capabilities.keySet()) {
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
        out.println("------------ End of WM Java client example ------------");
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