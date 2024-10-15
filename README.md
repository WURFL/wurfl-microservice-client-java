# ScientiaMobile WURFL Microservice Client for Java

WURFL Microservice (by ScientiaMobile, Inc.) is a mobile device detection service that can quickly and accurately detect over 500 capabilities of visiting devices. It can differentiate between portable mobile devices, desktop devices, SmartTVs and any other types of devices that have a web browser.

This is the Java Client API for accessing the WURFL Microservice. The API is released under Open-Source and can be integrated with other open-source or proprietary code. In order to operate, it requires access to a running instance of the WURFL Microservice product, such as:

- WURFL Microservice for Docker: https://www.scientiamobile.com/products/wurfl-microservice-docker-detect-device/

- WURFL Microservice for AWS: https://www.scientiamobile.com/products/wurfl-device-detection-microservice-aws/ 

- WURFL Microservice for Azure: https://www.scientiamobile.com/products/wurfl-microservice-for-azure/

- WURFL Microservice for Google Cloud Platform: https://www.scientiamobile.com/products/wurfl-microservice-for-gcp/

Java implementation of the WM Client api.

Minimum Java version compatibility table

|   | Min. Java version | WM client version(s) |
|---|--------------|----------------------|
|   |       8      | 2.1.3 and above      |
|   |       7      | 2.1.0                |
|   |       6      | 2.0.0                |

Version 2.1.2 (Java 7) and 2.1.3 (Java 8) only differ in their dependencies (see CHANGELOG)

### Compiling the client and the example project

```
mvn clean install
```
run on the wmclient directory, compiles the client and installs it in your local maven repository. The command also
runs the unit tests against a running instance of the WURFL Microservice server. Tests fails if no server is running.

** Note that from version 2.1.7, client artifact produced by mvn commands have their name aligned with the maven artifact
naming, changing from wm-client-java-x.y.z.jar to wurfl-microservice-x.y.z.jar.

To compile the example project, move to the example directory and run:

```
mvn clean compile package
```

to generate an executable jar file with all the needed dependencies.
You need to compile the client first, because the example project depends on the it.

In order to work, the example project needs a running instance of the WURFL Microservice server to connect to.

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

### Use all HTTP headers to perform a device detection

```java
 Map<String, String> headers = new HashMap<String, String>();
            headers.put("Accept-Encoding", "gzip, deflate");
            headers.put("Accept", "text/html, application/xml;q=0.9, application/xhtml+xml, image/png, image/webp, image/jpeg, image/gif, image/x-xbitmap, */*;q=0.1");
            headers.put("Accept-Language", "en");
            headers.put("Device-Stock-Ua", "Mozilla/5.0 (Linux; Android 8.1.0; SM-J610G Build/M1AJQ; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36");
            headers.put("Forwarded", "for=\"110.54.224.195:36350\"");
            headers.put("Save-Data", "on");
            headers.put("Referer", "https://www.cram.com/flashcards/labor-and-delivery-questions-889210");
            headers.put("User-Agent", "Opera/9.80 (Android; Opera Mini/51.0.2254/184.121; U; en) Presto/2.12.423 Version/12.16");
            headers.put("X-Clacks-Overhead", "GNU ph");
            headers.put("X-Forwarded-For", "110.54.224.195, 82.145.210.235");
            headers.put("X-Operamini-Features", "advanced, camera, download, file_system, folding, httpping, pingback, routing, touch, viewport");
            headers.put("X-Operamini-Phone", "Android #");
            headers.put("X-Operamini-Phone-Ua", "Mozilla/5.0 (Linux; Android 8.1.0; SM-J610G Build/M1AJQ; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36");
            Model.JSONDeviceData device = client.lookupHeaders(headers);

```

Migrating to Jakarta EE9 (Tomcat 10 and other new servers)
-----------
With Jakarta EE 9, the enterprise Java application ecosystem has faced a huge change. The most impacting one is the naming change from the Oracle owned `javax.*` 
package namespace to the new `jakarta.*`
This naming change will break most of the code that, for example, uses class `HttpServletRequest` or others from the same packages.
So if you want to use the `wurfl-microservice-client-java` in a web/application server that supports Jakarta EE9 such as:
  - Tomcat 10.x
  - Glassfish 6.x
  - Jetty 11.x
  or any other that will comply to this spec in the near future, you will need to migrate both the microservice client and the sample application (or yours, if needed) and build them from the source code.
    
### Step 1: use the Tomcat migration tool
If you use Tomcat 10 you can migrate part of the code by using the Tomcat Migration tool: https://tomcat.apache.org/download-migration.cgi
This tool replaces all the projects javax.* occurrences with jakarta.*. There are different option that you can specify depending on how your application uses the 
Java Enterprise features. Execute the `migrate.sh` script without parameters for more info on the usage options.

The command `./migrate.sh <path/to/wurfl-microservice-client-java> <path/to/destination dir> -profile=EE` will generate a "migrated" copy of the project code using any javax.* package to
the specified destination directory.

### Step 2: check the pom.xml dependencies
The migration tool tries to update the dependencies in your pom.xml files, but it's not perfect in that at the current development stage.
This means you have to check the EE dependencies yourself.

Wurfl microservice client java only depends on the servlet API, which must be modified as shown below 

```xml
<dependencies>
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>5.0.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

Please note that your application may need more dependencies to be updated in order to make it compliant with Jakarta EE 9 on Tomcat 10 (for example JSP and JSTç dependencies).

- JDK 16
- Maven 3.8.1
- Tomcat 10.0.6

### Step 3: updating other dependencies or code (if needed).
If you are migrating your custom project to Jakarta EE9, the project build may fail because some other dependency have not been updated, or its interface has changed. Fix the dependency or code and retry building you project.

### Migrating an already compiled wurfl-microservice-client-java jar file.
There may be cases in which you don't want or cannot rebuild the client or the application from the source code. In that case you can use a tool called [Eclipse Transformer](https://github.com/eclipse/transformer/blob/main/README.md) which can be executed on the wurfl-microservice-client-java JAR file and return an output JAR file in which the bytecode is made compliant with the new namespace **jakarta.***