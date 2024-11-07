/**
 * Copyright 2018 Scientiamobile Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
Copyright 2019 ScientiaMobile Inc. http://www.scientiamobile.com

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
package com.scientiamobile.wurfl.wmclient;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Holder of all model data objects used by wm client.
 */
public class Model {

    private Model(){}
    static Model m = new Model();

    // Factory methods
    static Request newRequest(Map<String, String> lookupHeaders, String[] requestedCaps, String[] requestedVcaps, String wurflId) {
        return m.new Request(lookupHeaders, requestedCaps, requestedVcaps, wurflId);
    }

    static JSONModelMktName newJSONModelMktName(String modelName, String mktName){
        return new JSONModelMktName(modelName, mktName);
    }

    /**
     * Holds information about wurfl microservice server and API
     * <p>
     * Created by Andrea Castello on 19/07/2017.
     */
    public class JSONInfoData {

        /** WURFL API version used by the wm server */
        @SerializedName("wurfl_api_version")
        String wurflApiVersion;

        /** WURFL Microservice version used by the wm server */
        @SerializedName("wm_version")
        String wmVersion;

        /** WURFL other WURFL information */
        @SerializedName("wurfl_info")
        String wurflInfo;

        @SerializedName("important_headers")
         String[] importantHeaders;

        @SerializedName("static_caps")
         String[] staticCaps;

        @SerializedName("virtual_caps")
         String[] virtualCaps;

        @SerializedName("ltime")
         String ltime;

        /**
         * @return Version of WURFL API used by the wm server
         */
         public String getWurflApiVersion() {
            return wurflApiVersion;
        }

        /**
         * @return Version of the wm server
         */
        public String getWmVersion() {
            return wmVersion;
        }

        /**
         * @return Information about the WURFL file used by the wm server
         */
        public String getWurflInfo() {
            return wurflInfo;
        }

        /**
         * @return List of important headers used when detecting a device using an HTTP Request
         */
         String[] getImportantHeaders() {
            return importantHeaders;
        }

        /**
         * @return List of static capabilities supported by currently running server
         */
        public String[] getStaticCaps() {
            return staticCaps;
        }

        /**
         * @return List of virtual capabilities supported by currently running server
         */
        public String[] getVirtualCaps() {
            return virtualCaps;
        }
    }

    /**
     * Holds the detected device data received from wm server.
     */
    public class JSONDeviceData {

        /**
         * Device capabilities mapped as capability name,capability value
         */
        @SerializedName("capabilities")
        public Map<String, String> capabilities;

        /**
         * Error message
         */
        @SerializedName("error")
        public String error;

        @SerializedName("mtime")
        public int mtime;

        @SerializedName("ltime")
        public String ltime;

        /**
         * Creates a new JSONDeviceData object
         * @param capabilities a map of device capabilities mapped as capability name,capability value
         * @param error an error message
         */
        public JSONDeviceData(Map<String, String> capabilities, String error, int mtime) {


            this.capabilities = capabilities;
            this.error = error;
            this.mtime = mtime;
        }
    }

    /**
     * Holds data relevant for the HTTP request that will be sent to wm server
     */
    class Request {

        @SerializedName("lookup_headers")
        private Map<String, String> lookupHeaders;
        @SerializedName("requested_caps")
        private String[] requestedCaps;
        @SerializedName("requested_vcaps")
        private String[] requestedVcaps;
        @SerializedName("wurfl_id")
        private String wurflId;

        Request(Map<String, String> lookupHeaders, String[] requestedCaps, String[] requestedVcaps, String wurflId) {
            this.lookupHeaders = lookupHeaders;
            this.requestedCaps = requestedCaps;
            this.requestedVcaps = requestedVcaps;
            this.wurflId = wurflId;
        }

        /**
         * @return the headers used for the lookup process
         */
        Map<String, String> getLookupHeaders() {
            return lookupHeaders;
        }

        /**
         * @return List of WURFL static capabilities requested to the server
         */
        public String[] getRequestedCaps() {
            return requestedCaps;
        }

        /**
         * @return List of WURFL virtual capabilities requested to the server
         */
        public String[] getRequestedVcaps() {
            return requestedVcaps;
        }

        /**
         * @return WURFL Id of the requested device (used when calling LookupDeviceID API)
         */
        String getWurflId() {
            return wurflId;
        }
    }

    /**
     * Holds data about a device make and model
     */
    public class JSONMakeModel {

        /** Brand name */
        @SerializedName("brand_name")
        public String brandName;

        /** Model name */
        @SerializedName("model_name")
        public String modelName;

        /** Marketing name (if available) */
        @SerializedName("marketing_name")
        public String marketingName;
    }

    /**
     * Holds data about a device model and marketing name
     */
    public static class JSONModelMktName {
        /** Model name */
        @SerializedName("model_name")
        public String modelName;

        /** Marketing name (if available) */
        @SerializedName("marketing_name")
        public String marketingName;

        /**
         * Creates a new JSONModelMktName object using the given model name and marketing name
         * @param modelName a device model name
         * @param marketingName a device marketing name
         */
        public JSONModelMktName(String modelName, String marketingName) {
            this.modelName = modelName;
            this.marketingName = marketingName;
        }
    }

    /**
     * Holds data about a device os name and version
     */
    public  class JSONDeviceOsVersions {
        /** The device OS name */
        @SerializedName("device_os")
        public String osName;

        /** The device OS version */
        @SerializedName("device_os_version")
        public String osVersion;
    }

}