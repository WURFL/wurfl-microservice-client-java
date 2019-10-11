# ScientiaMobile WURFL Microservice Client for Java

WURFL Microservice (by ScientiaMobile, Inc.) is a mobile device detection service that can quickly and accurately detect over 500 capabilities of visiting devices. It can differentiate between portable mobile devices, desktop devices, SmartTVs and any other types of devices that have a web browser.

This is the Java Client API for accessing the WURFL Microservice. The API is released under Open-Source and can be integrated with other open-source or proprietary code. In order to operate, it requires access to a running instance of the WURFL Microservice product, such as:

- WURFL Microservice for Docker: https://www.scientiamobile.com/products/wurfl-microservice-docker-detect-device/

- WURFL Microservice for AWS: https://www.scientiamobile.com/products/wurfl-device-detection-microservice-aws/ 

Maven Central repo link for wurfl-microservice-client-java : https://search.maven.org/search?q=a:wurfl-microservice 


```

/*
    Copyright 2018 Scientiamobile Inc.
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

import java.util.Iterator;
import java.util.Map;

import static java.lang.System.out;

public class Example {

    public static void main(String[] args)  {


        try {
            WmClient client = WmClient.create("http", "aws-host", "80", "");
            Model.JSONInfoData info = client.getInfo();
            out.println("Printing WM server information");
            out.println("WURFL API version: " + info.wurflApiVersion);
            out.println("WM server version:  " + info.wmVersion);
            out.println("Wurfl file info: " + info.wurflInfo);


            String ua = "Mozilla/5.0 (Linux; Android 7.1.1; ONEPLUS A5000 Build/NMF26X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Mobile Safari/537.36";

            client.setCacheSize(100000);

            // set the capabilities we want to receive from WM server
            client.setRequestedStaticCapabilities(new String[]{"brand_name", "model_name"});
            client.setRequestedVirtualCapabilities(new String[]{"is_smartphone", "form_factor"});

            out.println();
            out.println("Detecting device for user-agent: " + ua);

            Model.JSONDeviceData device = client.lookupUseragent(ua);
            // Applicative error, ie: invalid input provided
            if (device.error != null && device.error.length() > 0) {
                out.println("An error occurred: " + device.error);
            } else {
                Map<String,String> capabilities = device.capabilities;
                out.println("Detected device WURFL ID: " + capabilities.get("wurfl_id"));
                out.println("Device brand & model: " + capabilities.get("brand_name") + " " + capabilities.get("model_name"));
                out.println("Detected device form factor: " + capabilities.get("form_factor"));
                if(capabilities.get("is_smartphone").equals("true")){
                    out.println("This is a smartphone");
                }

                out.println("All received capabilities");
                Iterator<String> it = capabilities.keySet().iterator();
                while (it.hasNext()){
                    String k = it.next();
                    out.println(k + ": " + capabilities.get(k));
                }
            }
        }
        catch (WmException e){ // problems such as network errors  or internal server problems
            out.println("An error has occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

