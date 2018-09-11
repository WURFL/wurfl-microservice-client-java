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
package com.scientiamobile.wurfl.wmclient;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Holder of all model data objects used by wm client.
 */
public class Model {

    /**
     * Holds informations about wurfl private cloud server and API
     * <p>
     * Created by Andrea Castello on 19/07/2017.
     */
    public class JSONInfoData {

        @SerializedName("wurfl_api_version")
        public String wurflApiVersion;

        @SerializedName("wm_version")
        public String wmVersion;

        @SerializedName("wurfl_info")
        public String wurflInfo;

        @SerializedName("important_headers")
        public String[] importantHeaders;

        @SerializedName("static_caps")
        public String[] staticCaps;

        @SerializedName("virtual_caps")
        public String[] virtualCaps;

        @SerializedName("ltime")
        public String ltime;

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
        public String[] getImportantHeaders() {
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

        @SerializedName("capabilities")
        public Map<String, String> capabilities;

        @SerializedName("error")
        public String error;

        @SerializedName("mtime")
        public int mtime;

        @SerializedName("ltime")
        public String ltime;

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

        public Request(Map<String, String> lookupHeaders, String[] requestedCaps, String[] requestedVcaps, String wurflId) {
            this.lookupHeaders = lookupHeaders;
            this.requestedCaps = requestedCaps;
            this.requestedVcaps = requestedVcaps;
            this.wurflId = wurflId;
        }

        /**
         * @return the headers used for the lookup process
         */
        public Map<String, String> getLookupHeaders() {
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
        public String getWurflId() {
            return wurflId;
        }
    }


    public class JSONMakeModel {

        @SerializedName("brand_name")
        public String brandName;

        @SerializedName("model_name")
        public String modelName;

        @SerializedName("marketing_name")
        public String marketingName;
    }

    // Factory method
    public static Request newRequest(Map<String, String> lookupHeaders, String[] requestedCaps, String[] requestedVcaps, String wurflId) {
        return new Model().new Request(lookupHeaders, requestedCaps, requestedVcaps, wurflId);
    }

}