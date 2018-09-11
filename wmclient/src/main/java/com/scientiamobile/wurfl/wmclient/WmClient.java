/*
    Copyright (c) ScientiaMobile, Inc.
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

import com.google.gson.Gson;
import com.scientiamobile.wurfl.wmclient.Model.Request;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.scientiamobile.wurfl.wmclient.Model.newRequest;

/**
 * Main class for Java WM client. Performs requests to WURFL Microservice server and handles response.<br>
 * Author(s):  Andrea Castello
 * Date: 19/07/2017.
 */
public class WmClient {

    private final static String DEVICE_ID_CACHE_TYPE = "dId-cache";
    private final static String USERAGENT_CACHE_TYPE = "ua-cache";
    private static final String CLIENT_TOKEN_HEADER = "X-Md5-Signature";

    private String scheme;
    private String host;
    private String port;
    private String baseURI;

    // These are the lists of all static or virtual that can be returned by the running wm server
    private String[] staticCaps;
    private String[] virtualCaps;

    // Requested are used in the lookup requests, accessible via the SetRequested[...] methods
    private String[] requestedStaticCaps;
    private String[] requestedVirtualCaps;

    private String[] importantHeaders;

    // Internal caches
    private LRUCache<String, Model.JSONDeviceData> devIDCache; // Maps device ID -> JSONDeviceData
    private LRUCache<String, Model.JSONDeviceData> uaCache; // Maps concat headers (mainly UA) -> JSONDeviceData

    // Time of last WURFL.xml file load on server
    private String ltime;

    // Token sent to server for API permission check
    private String clientToken;

    // Stores the result of time consuming call getAllMakeModel
    public Model.JSONMakeModel[] makeModels = new Model.JSONMakeModel[0];
    // Lock object used for MakeModel safety
    private final Object mkMdLock = new Object();

    // internal http client
    private CloseableHttpClient _internalClient;

    private WmClient(String scheme, String host, String port, String baseURI) throws WmException {

        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.baseURI = baseURI;

        if (StringUtils.isEmpty(scheme)) {
            throw new WmException("WM client scheme cannot be empty");
        }

        if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            // Increase max total connection to 200
            cm.setMaxTotal(200);
            _internalClient = HttpClients.custom().setConnectionManager(cm).build();
        } else {
            throw new WmException("Invalid connection scheme specified:  [" + scheme + " ]");
        }
    }

    private String createUrl(String path) {
        String bpath = scheme + "://" + host + ":" + port + "/";
        if (StringUtils.isNotEmpty(baseURI)) {
            bpath += baseURI + "/";
        }
        return bpath + "/" + path;
    }

    /**
     * Creates an instance of a WURFL Microservice client
     * @param scheme protocol scheme
     * @param host host of the WM server
     * @param port port of the WM server
     * @param baseURI any base URI which must be added after the host (NOT including the endpoints, which are handled by the client).
     *                This may be useful, for example, with thrird parties VMs (like docker or AWS). Leave empty or null if not needed.
     * @return The instance of the WM client
     * @throws WmException In case a connection error occurs
     */
    public static WmClient create(String scheme, String host, String port, String baseURI) throws WmException {

        try {
            WmClient client = new WmClient(scheme, host, port, baseURI);
            client.createMD5Token();
            // Test server connection and save important headers taken using getInfo function
            Model.JSONInfoData info = client.getInfo();

            if (!checkData(info)) {
                throw new WmException("Error getting data from WM server : client not authorized");
            }

            client.importantHeaders = info.getImportantHeaders();
            client.staticCaps = info.getStaticCaps();
            client.virtualCaps = info.getVirtualCaps();
            Arrays.sort(client.staticCaps);
            Arrays.sort(client.virtualCaps);
            client.ltime = info.ltime;
            return client;
        } catch (Exception e) {
            throw new WmException("Unable to create wm client: " + e.getMessage());
        }
    }

    private static boolean checkData(Model.JSONInfoData info) {
        // If these are empty there's something wrong
        return StringUtils.isNotEmpty(info.getWmVersion()) && StringUtils.isNotEmpty(info.getWurflApiVersion()) && StringUtils.isNotEmpty(info.getWurflInfo())
                && (ArrayUtils.isNotEmpty(info.getStaticCaps()) || ArrayUtils.isNotEmpty(info.getVirtualCaps()));
    }

    /**
     * @return A JSONInfoData instance holding the capabilities exposed from WM server, the headers used for device detection, WURFL file and API version
     * @throws WmException If server cannot send data or incomplete data are sent
     */
    public Model.JSONInfoData getInfo() throws WmException {
        try {
            final HttpGet req = new HttpGet(createUrl("/v2/getinfo/json"));
            req.addHeader(CLIENT_TOKEN_HEADER, this.clientToken);
            Class<Model.JSONInfoData> type = Model.JSONInfoData.class;
            Model.JSONInfoData info = _internalClient.execute(req, new WmDataHandler<Model.JSONInfoData>(type));
            // Check if cache must be cleared
            clearCachesIfNeeded(info.ltime);
            return info;
        } catch (Exception e) {
            throw new WmException("Unable to get information from WM server :" + e.getMessage(), e);
        }
    }

    /**
     * @return An array of JSONMakeModel structures, holding brand, model and marketing name of all devices handled by WM server.
     * @throws WmException In case a connection error occurs or malformed data are sent
     */
    public Model.JSONMakeModel[] getAllMakeModel() throws WmException {

        // If makeModel cache has values we return them
        synchronized (mkMdLock) {
            if (this.makeModels != null && this.makeModels.length > 0) {
                return this.makeModels;
            }
        }

        // No cache found, let's do a server lookup
        try {
            final HttpGet req = new HttpGet(createUrl("/v2/alldevices/json"));
            req.addHeader(CLIENT_TOKEN_HEADER, this.clientToken);
            Class<Model.JSONMakeModel[]> type = Model.JSONMakeModel[].class;
            Model.JSONMakeModel[] localMakeModels = _internalClient.execute(req, new WmDataHandler<Model.JSONMakeModel[]>(type));
            synchronized (mkMdLock) {
                if (this.makeModels == null || this.makeModels.length == 0) {
                    this.makeModels = localMakeModels;
                }
            }
            return localMakeModels;
        } catch (IOException e) {
            throw new WmException("An error occurred gettin all devices " + e.getMessage(), e);
        }
    }

    /**
     * Performs a device detection against a user agent header
     * @param useragent a user agent header
     * @return An object containing the device capabilities
     * @throws WmException In case any error occurs during device detection
     */
    public Model.JSONDeviceData lookupUseragent(String useragent) throws WmException {

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", useragent);
        Request request = newRequest(headers, this.requestedStaticCaps, this.requestedVirtualCaps, null);
        return internalRequest("/v2/lookupuseragent/json", request, USERAGENT_CACHE_TYPE);
    }

    /**
     * Returns the device matching the given WURFL ID
     * @param wurflId a WURFL device identifier
     * @return An object containing the device capabilities
     * @throws WmException In case any error occurs
     */
    public Model.JSONDeviceData lookupDeviceId(String wurflId) throws WmException {

        Request request = newRequest(null, this.requestedStaticCaps, this.requestedVirtualCaps, wurflId);
        return internalRequest("/v2/lookupdeviceid/json", request, DEVICE_ID_CACHE_TYPE);
    }

    /**
     * Performs a device detection using an HTTP request object, as passed from Java Web applications
     * @param httpRequest an instance of HTTPServletRequest
     * @return An object containing the device capabilities
     * @throws WmException In case any error occurs during device detection
     */
    public Model.JSONDeviceData lookupRequest(HttpServletRequest httpRequest) throws WmException {

        if (httpRequest == null) {
            throw new WmException("HttpServletRequest cannot be null");
        }

        Map<String, String> reqHeaders = new HashMap<String, String>();
        for (String hname : importantHeaders) {
            String hval = httpRequest.getHeader(hname);
            if (!StringUtils.isEmpty(hval)) {
                reqHeaders.put(hname, hval);
            }
        }

        Model.JSONDeviceData device = internalRequest("/v2/lookuprequest/json", newRequest(reqHeaders, this.requestedStaticCaps,
                this.requestedVirtualCaps, null), USERAGENT_CACHE_TYPE);
        return device;
    }

    public void setRequestedStaticCapabilities(String[] capsList) {

        if (capsList == null) {
            this.requestedStaticCaps = null;
            this.clearCaches();
            return;
        }

        List<String> stCaps = new ArrayList<String>();
        for (String name : capsList) {
            if (hasStaticCapability(name)) {
                stCaps.add(name);
            }
        }
        this.requestedStaticCaps = stCaps.toArray(new String[stCaps.size()]);
        clearCaches();
    }

    public void setRequestedVirtualCapabilities(String[] vcapsList) {

        if (vcapsList == null) {
            this.requestedVirtualCaps = null;
            this.clearCaches();
            return;
        }

        List<String> vCaps = new ArrayList<String>();
        for (String name : vcapsList) {
            if (hasVirtualCapability(name)) {
                vCaps.add(name);
            }
        }
        this.requestedVirtualCaps = vCaps.toArray(new String[vCaps.size()]);
        clearCaches();
    }

    /**
     *
     * @param capName capability name
     * @return true if the given static capability is handled by this client, false otherwise
     */
    public boolean hasStaticCapability(String capName) {
        return ArrayUtils.contains(this.staticCaps, capName);
    }

    /**
     * @param capName capability name
     * @return true if the given virtual capability is handled by this client, false otherwise
     */
    public boolean hasVirtualCapability(String capName) {
        return ArrayUtils.contains(this.virtualCaps, capName);
    }

    public void setRequestedCapabilities(String[] capsList) {
        if (capsList == null) {
            this.requestedStaticCaps = null;
            this.requestedVirtualCaps = null;
            this.clearCaches();
            return;
        }

        List<String> capNames = new ArrayList<String>();
        List<String> vcapNames = new ArrayList<String>();

        for (String name : capsList) {
            if (hasStaticCapability(name)) {
                capNames.add(name);
            } else if (hasVirtualCapability(name)) {
                vcapNames.add(name);
            }
        }
        if (CollectionUtils.isNotEmpty(capNames)) {
            this.requestedStaticCaps = capNames.toArray(new String[capNames.size()]);
        }

        if (CollectionUtils.isNotEmpty(vcapNames)) {
            this.requestedVirtualCaps = vcapNames.toArray(new String[vcapNames.size()]);
        }
        clearCaches();
    }

    /**
     * Deallocates all resources used by client. All subsequent usage of client will result in a WmException (you need to create the client again
     * with a call to WmClient.create().
     * @throws WmException In case of closing connection errors.
     */
    public void destroyConnection() throws WmException {
        try {
            clearCaches();
            uaCache = null;
            devIDCache = null;
            makeModels = null;
            _internalClient.close();
        } catch (IOException e) {
            throw new WmException("Unable to close client: " + e.getMessage(), e);
        }
    }

    /**
     * @return All static capabilities handled by this client
     */
    public String[] getStaticCaps() {
        return staticCaps;
    }

    /**
     * @return All the virtual capabilities handled by this client
     */
    public String[] getVirtualCaps() {
        return virtualCaps;
    }

    /**
     * @return list all HTTP headers used for device detection by this client
     */
    public String[] getImportantHeaders() {
        return importantHeaders;
    }

    private Model.JSONDeviceData internalRequest(String path, Request request, String cacheType) throws WmException {

        Model.JSONDeviceData device;
        String cacheKey = null;
        if (DEVICE_ID_CACHE_TYPE.equals(cacheType)) {
            cacheKey = request.getWurflId();
        } else if (USERAGENT_CACHE_TYPE.equals(cacheType)) {
            cacheKey = this.getUserAgentCacheKey(request.getLookupHeaders(), cacheType);
        }

        // First, do a cache lookup
        if (StringUtils.isNotEmpty(cacheType) && !StringUtils.isNotEmpty(cacheKey)) {
            if (cacheType.equals(DEVICE_ID_CACHE_TYPE) && devIDCache != null) {
                device = devIDCache.getEntry(request.getWurflId());
                if (device != null) {
                    return device;
                }
            } else if (cacheType.equals(USERAGENT_CACHE_TYPE) && uaCache != null) {
                device = uaCache.getEntry(cacheKey);
                if (device != null) {
                    return device;
                }
            }
        }

        // No device found in cache, let's try a server lookup
        Gson gson = new Gson();
        StringEntity requestEntity = new StringEntity(
                gson.toJson(request),
                ContentType.APPLICATION_JSON);

        HttpPost postMethod = new HttpPost(createUrl(path));
        postMethod.addHeader(CLIENT_TOKEN_HEADER, this.clientToken);
        postMethod.setEntity(requestEntity);

        Class<Model.JSONDeviceData> type = Model.JSONDeviceData.class;
        try {
            device = _internalClient.execute(postMethod, new WmDataHandler<Model.JSONDeviceData>(type));
            if (StringUtils.isNotEmpty(device.error)) {
                throw new WmException("Unable to complete request to WM server: " + device.error);
            }

            // Check if caches must be cleared before adding a new device
            clearCachesIfNeeded(device.ltime);
            if (cacheType != null) {
                if (cacheType.equals(USERAGENT_CACHE_TYPE) && devIDCache != null && !cacheKey.equals("")) {
                    safePutDevice(uaCache, cacheKey, device);
                } else if (cacheType.equals(DEVICE_ID_CACHE_TYPE) && uaCache != null && !cacheKey.equals("")) {
                    safePutDevice(devIDCache, cacheKey, device);
                }
            }
            return device;
        } catch (Exception e) {
            throw new WmException("Unable to complete request to WM server: " + e.getMessage(), e);
        }
    }

    /**
     * Sets the client cache size
     * @param uaMaxEntries maximum cache dimension
     */
    public void setCacheSize(int uaMaxEntries) {
        this.uaCache = new LRUCache<String, Model.JSONDeviceData>(uaMaxEntries);
        this.devIDCache = new LRUCache<String, Model.JSONDeviceData>(); // this has the default cache size
    }

    /**
     * @return This client API version
     */
    public String getApiVersion() {
        return "1.1.0.0";
    }

    private void clearCaches() {
        if (uaCache != null) {
            uaCache.clear();
        }

        if (devIDCache != null) {
            devIDCache.clear();
        }

        makeModels = new Model.JSONMakeModel[0];
    }


    private void clearCachesIfNeeded(String ltime) {
        if (ltime != null && !ltime.equals(this.ltime)) {
            this.ltime = ltime;
            clearCaches();
        }
    }

    private String getUserAgentCacheKey(Map<String, String> headers, String cacheType) throws WmException {
        String key = "";

        if (headers == null && USERAGENT_CACHE_TYPE.equals(cacheType)) {
            throw new WmException("No User-Agent provided");
        }

        // Using important headers array preserves header name order
        for (String h : importantHeaders) {
            String hval = headers.get(h);
            if (hval != null) {
                key += hval;
            }
        }
        return key;
    }

    private void safePutDevice(LRUCache<String, Model.JSONDeviceData> cache, String key, Model.JSONDeviceData device) {
        if (cache != null) {
            cache.putEntry(key, device);
        }
    }

    public int[] getActualCacheSizes() {
        int[] csize = new int[2];

        if (devIDCache != null) {
            csize[0] = devIDCache.size();
        }

        if (uaCache != null) {
            csize[1] = uaCache.size();
        }

        return csize;
    }

    private void createMD5Token() throws WmException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String strDate = sdf.format(now);
            md.update(strDate.getBytes("UTF-8"), 0, strDate.length());
            String s = new BigInteger(1, md.digest()).toString(16);
            StringBuilder b = new StringBuilder(s);
            b.replace(11, 12, "0");
            b.replace(17, 18, "4");
            this.clientToken = b.toString();
        } catch (Exception e) {
            throw new WmException("An error occurred generating MD5 token", e);
        }

    }
}

class WmDataHandler<T> implements ResponseHandler<T> {

    private Class<T> type;

    public WmDataHandler(Class<T> type) {
        this.type = type;
    }

    @Override
    public T handleResponse(HttpResponse res) throws IOException {
        Gson gson = new Gson();
        int status = res.getStatusLine().getStatusCode();
        String json;
        if (status >= 200 && status < 300) {
            HttpEntity entity = res.getEntity();

            json = entity != null ? EntityUtils.toString(entity) : null;


            T result = gson.fromJson(json, type);
            return result;
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    }
}
