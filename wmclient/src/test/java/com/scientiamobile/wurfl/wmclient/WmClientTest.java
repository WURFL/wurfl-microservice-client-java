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

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.*;

import static org.testng.Assert.*;

/*
 * Main test class
 * Author(s):  Andrea Castello
 * Date: 24/07/2017.
 * NOTE: this test assumes you have a running WURFL Microservice running.
 */
public class WmClientTest {

    // internal instance that can be reused. Not used for create method tests.
    private WmClient _client;

    @BeforeClass
    public void createTestClient() throws WmException {
        String host = "localhost";
        String port = "8080";
        String envHost = System.getenv("WM_HOST");
        String envPort = System.getenv("WM_PORT");
        if (StringUtils.isNotEmpty(envHost)){
            host = envHost;
        }
        if(StringUtils.isNotEmpty(envPort)){
            port = envPort;
        }
        _client = WmClient.create("http", host, port, "");

    }


    public WmClient createCachedTestClient(int csize) throws WmException {
        String host = "localhost";
        String port = "8080";
        String envHost = System.getenv("WM_HOST");
        String envPort = System.getenv("WM_PORT");
        if (StringUtils.isNotEmpty(envHost)){
            host = envHost;
        }
        if(StringUtils.isNotEmpty(envPort)){
            port = envPort;
        }
        WmClient cl = WmClient.create("http", host, port, "");
        cl.setCacheSize(csize);
        return cl;
    }


    @Test
    public void createOkTest() throws WmException {
        String host = "localhost";
        String port = "8080";
        String envHost = System.getenv("WM_HOST");
        String envPort = System.getenv("WM_PORT");
        if (StringUtils.isNotEmpty(envHost)){
            host = envHost;
        }
        if(StringUtils.isNotEmpty(envPort)){
            port = envPort;
        }
        WmClient client = WmClient.create("http", host, port, "");
        assertNotNull(client);
        assertTrue(client.getImportantHeaders().length > 0);
        assertTrue(client.getVirtualCaps().length > 0);
        assertTrue(client.getStaticCaps().length > 0);

    }

    @Test(expectedExceptions = {WmException.class})
    public void createWithServerDownTest() throws WmException {
        WmClient.create("http", "localhost", "18080", "");
    }

    @Test(expectedExceptions = {WmException.class})
    public void TestCreateWithoutHost() throws WmException {
        WmClient.create("http", "", "8080", "");
    }

    @Test(expectedExceptions = {WmException.class})
    public void TestCreateWithEmptyServerValues() throws WmException {
        WmClient.create("", "", "", "");
    }

    @Test
    public void getInfoTest() throws WmException {
        Model.JSONInfoData jsonInfoData = _client.getInfo();
        assertNotNull(jsonInfoData);
        assertTrue(jsonInfoData.getWurflInfo().length() > 0);
        assertTrue(jsonInfoData.getImportantHeaders().length > 0);
        assertTrue(jsonInfoData.getStaticCaps().length > 0);
        assertTrue(jsonInfoData.getVirtualCaps().length > 0);
    }

    @Test
    public void lookupUserAgentTest() throws WmException {
        String ua = "Mozilla/5.0 (Linux; Android 7.0; SAMSUNG SM-G950F Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/5.2 Chrome/51.0.2704.106 Mobile Safari/537.36";
        Model.JSONDeviceData device = _client.lookupUseragent(ua);
        assertNotNull(device);
        Map<String, String> capabilities = device.capabilities;
        int dcount = capabilities.size();
        assertTrue( dcount >= 40);

        assertEquals(capabilities.get("model_name"), "SM-G950F");
        assertEquals("false", capabilities.get("is_app"));
        assertEquals("false", capabilities.get("is_app_webview"));
    }

    @Test
    public void lookupUserAgentWithSpecificCapsTest() throws WmException {
        String[] reqCaps = {"brand_name", "model_name", "physical_screen_width", "device_os", "is_android", "is_ios", "is_app"};

        _client.setRequestedCapabilities(reqCaps);
        String ua = "Mozilla/5.0 (Nintendo Switch; WebApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341";
        Model.JSONDeviceData device = _client.lookupUseragent(ua);
        Map<String, String> capabilities = device.capabilities;
        assertNotNull(device);
        assertNotNull(capabilities);
        assertEquals("Nintendo", capabilities.get("brand_name"));
        assertEquals("Switch", capabilities.get("model_name"));
        assertEquals("false", capabilities.get("is_android"));
        assertEquals(8, capabilities.size());
        _client.setRequestedCapabilities(null);
    }

    @Test
    public void lookupUseragentEmptyUaTest() {

        boolean exc = false;
        try {
            Model.JSONDeviceData device = _client.lookupUseragent("");
            assertNotNull(device);
            assertEquals(device.capabilities.get("wurfl_id"), "generic");
        } catch (WmException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testLookupUseragentNullUa() {
        boolean exc = false;
        try {
            Model.JSONDeviceData device = _client.lookupUseragent(null);
            assertNotNull(device);
            assertEquals(device.capabilities.get("wurfl_id"), "generic");
        } catch (WmException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void lookupDeviceIdTest() throws WmException {
        Model.JSONDeviceData device = _client.lookupDeviceId("nokia_generic_series40");
        assertNotNull(device);
        Map<String, String> capabilities = device.capabilities;
        assertNotNull(capabilities);
        // num caps + num vcaps + wurfl id
        assertTrue(capabilities.size() >= 40);
        assertEquals("false", capabilities.get("is_android"));
        assertEquals("128", capabilities.get("resolution_width"));
    }

    @Test
    public void lookupDeviceIdWithSpecificCaps() throws WmException {
        String[] reqCaps = {"brand_name", "is_smarttv"};
        String[] reqvCaps = {"is_app", "is_app_webview"};
        _client.setRequestedStaticCapabilities(reqCaps);
        _client.setRequestedVirtualCapabilities(reqvCaps);
        Model.JSONDeviceData device = _client.lookupDeviceId("generic_opera_mini_version1");
        assertNotNull(device);
        Map<String, String> capabilities = device.capabilities;
        assertNotNull(capabilities);
        assertEquals("Opera", capabilities.get("brand_name"));
        assertEquals("false", capabilities.get("is_smarttv"));
        assertEquals(5, capabilities.size());
    }

    @Test
    public void lookupDeviceIdWithWrongIdTest() {

        boolean exc = false;
        try {
            _client.lookupDeviceId("nokia_generic_series40_wrong");
        } catch (WmException e) {
            exc = true;
            assertTrue(e.getMessage().contains("device is missing"));
        }
        assertTrue(exc);
    }

    @Test
    public void lookupDeviceIdWithNullIdTest() {
        boolean exc = false;
        try {
            _client.lookupDeviceId(null);
        } catch (WmException e) {
            exc = true;
            assertTrue(e.getMessage().contains("device is missing"));
        }
        assertTrue(exc);
    }

    @Test
    public void lookupDeviceIdWithEmptyIdTest() {
        boolean exc = false;
        try {
            _client.lookupDeviceId("");
        } catch (WmException e) {
            exc = true;
            assertTrue(e.getMessage().contains("device is missing"));
        }
        assertTrue(exc);


    }

    @Test
    public void LookupRequestOKTest() throws WmException {
        HttpServletRequest request = createTestRequest(true);
        Model.JSONDeviceData device = _client.lookupRequest(request);
        assertNotNull(device);
        Map<String, String> capabilities = device.capabilities;
        assertNotNull(capabilities);
        assertTrue(capabilities.size() >= 40);
        assertEquals("Smart-TV", capabilities.get("form_factor"));
        assertEquals("5.1.0.13341", capabilities.get("advertised_browser_version"));
        assertEquals("false", capabilities.get("is_app"));
        assertEquals("false", capabilities.get("is_app_webview"));
        assertEquals("Nintendo", capabilities.get("advertised_device_os"));
        assertEquals("Nintendo Switch", capabilities.get("complete_device_name"));
        assertEquals("nintendo_switch_ver1", capabilities.get("wurfl_id"));
    }

    @Test
    public void LookupHeadersOKTest() throws WmException {

        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent".toLowerCase(), "Mozilla/5.0 (Nintendo Switch; WebApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341");
        headers.put("Content-Type".toLowerCase(), "gzip, deflate");
        headers.put("Accept-Encoding".toLowerCase(), "application/json");
        headers.put("X-UCBrowser-Device-UA".toLowerCase(), "Mozilla/5.0 (Nintendo Switch; ShareApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341");
        headers.put("Device-Stock-UA".toLowerCase(), "Mozilla/5.0 (Nintendo Switch; WifiWebAuthApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341");

        Model.JSONDeviceData device = _client.lookupHeaders(headers);
        assertNotNull(device);
        Map<String, String> capabilities = device.capabilities;
        assertNotNull(capabilities);
        assertTrue(capabilities.size() >= 40);
        assertEquals("Smart-TV", capabilities.get("form_factor"));
        assertEquals("5.1.0.13341", capabilities.get("advertised_browser_version"));
        assertEquals("false", capabilities.get("is_app"));
        assertEquals("false", capabilities.get("is_app_webview"));
        assertEquals("Nintendo", capabilities.get("advertised_device_os"));
        assertEquals("Nintendo Switch", capabilities.get("complete_device_name"));
        assertEquals("nintendo_switch_ver1", capabilities.get("wurfl_id"));
    }

    @Test
    public void LookupHeadersWithMixedCaseTest() throws WmException {
        HttpServletRequest request = createTestRequest(true);
        Map<String, String> headers = new HashMap<>();
        headers.put("User-AGenT", "Mozilla/5.0 (Nintendo Switch; WebApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341");
        headers.put("Content-TYPe", "gzip, deflate");
        headers.put("Accept-EnCoding", "application/json");
        headers.put("X-UCBrowsEr-Device-UA", "Mozilla/5.0 (Nintendo Switch; ShareApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341");
        headers.put("Device-StOck-UA", "Mozilla/5.0 (Nintendo Switch; WifiWebAuthApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341");

        Model.JSONDeviceData device = _client.lookupHeaders(headers);
        assertNotNull(device);
        Map<String, String> capabilities = device.capabilities;
        assertNotNull(capabilities);
        assertTrue(capabilities.size() >= 40);
        assertEquals("Smart-TV", capabilities.get("form_factor"));
        assertEquals("5.1.0.13341", capabilities.get("advertised_browser_version"));
        assertEquals("false", capabilities.get("is_app"));
        assertEquals("false", capabilities.get("is_app_webview"));
        assertEquals("Nintendo", capabilities.get("advertised_device_os"));
        assertEquals("Nintendo Switch", capabilities.get("complete_device_name"));
        assertEquals("nintendo_switch_ver1", capabilities.get("wurfl_id"));
    }

    @Test
    public void LookupHeadersWithMixedCaseAndCachedClientTest() throws WmException {
        WmClient cachedClient = createTestCachedClient(1000);
        HttpServletRequest request = createTestRequest(true);
        Map<String, String> headers = new HashMap<>();
        headers.put("User-AGenT", "Mozilla/5.0 (Nintendo Switch; WebApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341");
        headers.put("Content-TYPe", "gzip, deflate");
        headers.put("Accept-EnCoding", "application/json");
        headers.put("X-UCBrowsEr-Device-UA", "Mozilla/5.0 (Nintendo Switch; ShareApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341");
        headers.put("Device-StOck-UA", "Mozilla/5.0 (Nintendo Switch; WifiWebAuthApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341");

        Model.JSONDeviceData device = cachedClient.lookupHeaders(headers);
        assertNotNull(device);
        Map<String, String> capabilities = device.capabilities;
        assertNotNull(capabilities);
        assertTrue(capabilities.size() >= 40);
        assertEquals("Smart-TV", capabilities.get("form_factor"));
        assertEquals("5.1.0.13341", capabilities.get("advertised_browser_version"));
        assertEquals("false", capabilities.get("is_app"));
        assertEquals("false", capabilities.get("is_app_webview"));
        assertEquals("Nintendo", capabilities.get("advertised_device_os"));
        assertEquals("Nintendo Switch", capabilities.get("complete_device_name"));
        assertEquals("nintendo_switch_ver1", capabilities.get("wurfl_id"));

        int[] cacheSize = cachedClient.getActualCacheSizes();
        assertEquals(cacheSize[1], 1);

        // Now mix headers case in a different way (we should hit the cache now)
        headers = new HashMap<>();
        headers.put("UseR-AGenT", "Mozilla/5.0 (Nintendo Switch; WebApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341");
        headers.put("ConTent-TYPe", "gzip, deflate");
        headers.put("AccEpt-EnCoding", "application/json");
        headers.put("X-UCbrowsEr-DeviCe-UA", "Mozilla/5.0 (Nintendo Switch; ShareApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341");
        headers.put("DevIce-StOck-Ua", "Mozilla/5.0 (Nintendo Switch; WifiWebAuthApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341");

        device = cachedClient.lookupHeaders(headers);
        capabilities = device.capabilities;
        assertNotNull(capabilities);
        assertEquals("nintendo_switch_ver1", capabilities.get("wurfl_id"));
        // Cache size should stay 1, which means that previously stored cache value has been hit even if header case has been changed
        cacheSize = cachedClient.getActualCacheSizes();
        assertEquals(cacheSize[1], 1);
        cachedClient.destroyConnection();



    }

    @Test
    public void lookupHeadersWithNullOrEmptyHeadersTest() throws WmException {

        Model.JSONDeviceData device = _client.lookupHeaders(null);
        assertNotNull(device);
        Map<String, String> capabilities = device.capabilities;
        assertNotNull(capabilities);
        assertEquals("generic", capabilities.get("wurfl_id"));

        device = _client.lookupHeaders(new HashMap<>());
        assertNotNull(device);
        capabilities = device.capabilities;
        assertNotNull(capabilities);
        assertEquals("generic", capabilities.get("wurfl_id"));

    }

    @Test
    public void lookupRequestOkWithSpecificCaps() throws WmException {

        String[] reqCaps = {"is_mobile", "form_factor", "is_app", "complete_device_name",
                "advertised_device_os", "brand_name"};
        _client.setRequestedCapabilities(reqCaps);
        Model.JSONDeviceData device = _client.lookupRequest(createTestRequest(true));
        Map<String, String> capabilities = device.capabilities;
        assertNotNull(capabilities);
        assertNotNull(capabilities);
        assertEquals(7, capabilities.size());
        assertEquals("false", capabilities.get("is_app"));
        assertEquals("Nintendo", capabilities.get("advertised_device_os"));
        assertEquals("Nintendo Switch", capabilities.get("complete_device_name"));
        assertEquals("nintendo_switch_ver1", capabilities.get("wurfl_id"));
        _client.setRequestedCapabilities(null);
    }


    @Test
    public void lookupRequestWithSpecificCapsAndNoHeadersTest() {
        boolean exc = false;
        try {
            String[] reqCaps = {"brand_name", "is_wireless_device", "pointing_method", "model_name"};
            _client.setRequestedCapabilities(reqCaps);
            // Create request to pass
            Model.JSONDeviceData device = _client.lookupRequest(createTestRequest(false));
            assertNotNull(device);
            assertEquals(device.capabilities.get("wurfl_id"), "generic");
        } catch (WmException e) {
            fail(e.getMessage());
        } finally {
            _client.setRequestedCapabilities(null);
        }

    }

    @Test(expectedExceptions = {WmException.class})
    public void lookupWithNullRequestTest() throws WmException {
        _client.lookupRequest(null);
    }

    @Test
    public void destroyConnectionTest() throws WmException {
        boolean exc = false;
        _client.destroyConnection();
        try {
            // since it has been closed, any further request will raise an exception
            _client.getInfo();
        } catch (WmException e) {
            exc = true;
        } finally {
            createTestClient();
        }
        assertTrue(exc);

    }

    @Test
    public void hasStaticCapabilityTest() {

        assertNotNull(_client);
        assertTrue(_client.hasStaticCapability("brand_name"));
        assertTrue(_client.hasStaticCapability("model_name"));
        assertTrue(_client.hasStaticCapability("is_smarttv"));
        // this is a virtual capability, so it shouldn't be returned
        assertFalse(_client.hasStaticCapability("is_app"));
    }

    @Test
    public void hasVirtualCapabilityTest() {
        assertTrue(_client.hasVirtualCapability("is_app"));
        assertTrue(_client.hasVirtualCapability("is_smartphone"));
        assertTrue(_client.hasVirtualCapability("form_factor"));
        assertTrue(_client.hasVirtualCapability("is_app_webview"));
        // this is a static capability, so it shouldn't be returned
        assertFalse(_client.hasVirtualCapability("brand_name"));
        assertFalse(_client.hasVirtualCapability("is_wireless_device"));
    }

    @Test
    public void lookupRequestWithCacheTest() throws WmException {
        WmClient client = createCachedTestClient(1000);
        HttpServletRequest request = createTestRequest(true);
        String url = "http://vimeo.com/api/v2/brad/info.json";

        for (int i = 0; i < 50; i++) {
            Model.JSONDeviceData jsonData = client.lookupRequest(request);
            Assert.assertNotNull(jsonData);
            Map<String, String> did = jsonData.capabilities;
            Assert.assertNotNull(did);

            Assert.assertEquals("Nintendo", did.get("brand_name"));
            Assert.assertEquals("true", did.get("is_mobile"));
            Assert.assertTrue(did.size() >= 40);

            int[] cSize = client.getActualCacheSizes();

            Assert.assertEquals(cSize[0], 0);
            Assert.assertEquals(cSize[1], 1);
        }
        client.destroyConnection();
    }

    @Test
    public void lookupWithCacheExpirationTest() throws Exception {
        WmClient client = createCachedTestClient(1000);
        Model.JSONDeviceData d1 = client.lookupDeviceId("nokia_generic_series40");
        Assert.assertNotNull(d1);
        Model.JSONDeviceData d2 = client.lookupUseragent("Mozilla/5.0 (iPhone; CPU iPhone OS 10_2_1 like Mac OS X) AppleWebKit/602.4.6 (KHTML, like Gecko) Version/10.0 Mobile/14D27 Safari/602.1");
        Assert.assertNotNull(d2);
        int[] csizes = client.getActualCacheSizes();
        Assert.assertEquals(1, csizes[0]);
        Assert.assertEquals(1, csizes[1]);
        // Date doesn't change, so cache stay full
        invokeClearCacheIfNeeded(client, d1.ltime);
        Assert.assertEquals(1, csizes[0]);
        Assert.assertEquals(1, csizes[1]);

        // Force cache expiration using reflection: now, date changes, so caches must be cleared
        invokeClearCacheIfNeeded(client, "2199-12-31");

        csizes = client.getActualCacheSizes();
        Assert.assertEquals(0, csizes[0]);
        Assert.assertEquals(0, csizes[1]);
        // Load a device again
        client.lookupDeviceId("nokia_generic_series40");
        client.lookupUseragent("Mozilla/5.0 (iPhone; CPU iPhone OS 10_2_1 like Mac OS X) AppleWebKit/602.4.6 (KHTML, like Gecko) Version/10.0 Mobile/14D27 Safari/602.1");

        // caches are filled again
        csizes = client.getActualCacheSizes();
        Assert.assertEquals(1, csizes[0]);
        Assert.assertEquals(1, csizes[1]);
        client.destroyConnection();
    }

    @Test
    public void setRequestedCapabilitiesTest() throws WmException {
        WmClient client = createCachedTestClient(1000);
        client.setRequestedStaticCapabilities(new String[]{"wrong1", "brand_name", "is_ios"});
        client.setRequestedVirtualCapabilities(new String[]{"wrong2", "brand_name", "is_ios"});

        String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 10_2_1 like Mac OS X) AppleWebKit/602.4.6 (KHTML, like Gecko) Version/10.0 Mobile/14D27 Safari/602.1";
        Model.JSONDeviceData d = client.lookupUseragent(ua);
        Assert.assertNotNull(d);
        Assert.assertEquals(d.capabilities.size(), 3);
        Assert.assertNull(d.capabilities.get("wrong1"));

        // This will reset static caps
        client.setRequestedStaticCapabilities(null);
        d = client.lookupUseragent(ua);
        Assert.assertEquals(d.capabilities.size(), 2);
        // If all required caps arrays are reset, ALL caps are returned
        client.setRequestedVirtualCapabilities(null);
        d = client.lookupUseragent(ua);
        int capsize = d.capabilities.size();
        Assert.assertTrue(capsize >= 40);
        client.destroyConnection();
    }

    @Test
    public void multiThreadedLookupTest() throws WmException, InterruptedException, ExecutionException {
        WmClient client = createCachedTestClient(1000);
        List<Callable<Boolean>> lookups = createLookupTasks(8, client);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(8);
        List<Future<Boolean>> futures = executorService.invokeAll(lookups, 60, TimeUnit.MINUTES);
        int countDone = 0;
        for (Future<Boolean> f : futures) {

            if (f.isDone()) {
                Assert.assertTrue(f.get());
                countDone++;
            }
        }
        Assert.assertEquals(countDone, futures.size());
        client.destroyConnection();
    }

    @Test
    public void getAllDeviceMakesTest() throws Exception {

        WmClient client = createTestCachedClient(1000);
        try {
            // Testing a version before 1.2.0.0
            String v = client.getInfo().getWmVersion();
            if(VersionUtils.compareVersionNumbers(v, "1.2.0.0") < 0){
                System.out.printf("Version is %s , skipping test%n", v);
                return;
            }

            String[] makes = client.getAllDeviceMakes();
            assertNotNull(makes);
            assertTrue(makes.length > 2000);

        } finally {
            client.destroyConnection();
        }
    }

    @Test(expectedExceptions = WmException.class)
    public void getAllDevicesForMakeWithWrongMakeTest() throws Exception {
        WmClient client = createTestCachedClient(1000);
        // Testing a version before 1.2.0.0
        String v = client.getInfo().getWmVersion();
        if(VersionUtils.compareVersionNumbers(v, "1.2.0.0") < 0){
            System.out.printf("Version is %s , skipping test%n", v);
            return;
        }
        try {
            client.getAllDevicesForMake("Fakething");
        } finally {
            client.destroyConnection();
        }


    }

    @Test
    public void getAllDevicesForMakeTest() throws Exception {
        WmClient client = createTestCachedClient(1000);
        // Testing a version before 1.2.0.0
        try {

            String v = client.getInfo().getWmVersion();
            if(VersionUtils.compareVersionNumbers(v, "1.2.0.0") < 0){
                System.out.printf("Version is %s , skipping test%n", v);
                return;
        }

            Model.JSONModelMktName[] modelMktNames = client.getAllDevicesForMake("Nokia");
            assertNotNull(modelMktNames);
            assertTrue(modelMktNames.length > 700);
            assertNotNull(modelMktNames[0].modelName);
            assertNotNull(modelMktNames[5].marketingName);

            for(Model.JSONModelMktName mdmk: modelMktNames){
                assertNotNull(mdmk);
            }
        }
        finally {
            client.destroyConnection();
        }

    }

    @Test
    public void getAllOsesTest() throws Exception {
        WmClient client = createTestCachedClient(1000);
        try {
        String v = client.getInfo().getWmVersion();
        if(VersionUtils.compareVersionNumbers(v, "1.2.0.0") < 0){
            System.out.printf("Version is %s , skipping test%n", v);
            return;
        }

        String[] oses = client.getAllOSes();
        assertNotNull(oses);
        assertTrue(oses.length >= 30);
        }
        finally {
            client.destroyConnection();
        }

    }

    @Test
    public void getAllVersionsForOSTest() throws Exception {
        WmClient client = createTestCachedClient(1000);
        try {
            String v = client.getInfo().getWmVersion();
            if(VersionUtils.compareVersionNumbers(v, "1.2.0.0") < 0){
                System.out.printf("Version is %s , skipping test%n", v);
                return;
            }

        String[] osVersions = client.getAllVersionsForOS("Android");
        assertNotNull(osVersions);
        assertTrue(osVersions.length > 30);
        assertNotNull(osVersions[0]);
        }
        finally {
            client.destroyConnection();
        }

    }

    @Test(expectedExceptions = WmException.class)
    public void getAllVersionsForOsWithWrongOsTest() throws Exception {
        WmClient client = createTestCachedClient(1000);
        try {
            String v = client.getInfo().getWmVersion();
            if(VersionUtils.compareVersionNumbers(v, "1.2.0.0") < 0){
                System.out.printf("Version is %s , skipping test%n", v);
                return;
            }
            client.getAllVersionsForOS("FakeOS");
        } finally {
            client.destroyConnection();
        }
    }

    @Test
    public void realCacheUsageTest() throws WmException {
        String host = "localhost";
        String port = "8080";
        String envHost = System.getenv("WM_HOST");
        String envPort = System.getenv("WM_PORT");
        if (StringUtils.isNotEmpty(envHost)){
            host = envHost;
        }
        if(StringUtils.isNotEmpty(envPort)){
            port = envPort;
        }
        WmClient client = WmClient.create("http", host, port, "");
        try {
            long start = System.nanoTime();
            for (String ua: TestData.USER_AGENTS){
                client.lookupUseragent(ua);
            }
            long elapsedNoCache = System.nanoTime() - start;

            // Now, let's add a cache layer
            client.setCacheSize(100000);

            // fill cache
            for (String ua: TestData.USER_AGENTS){
                    client.lookupUseragent(ua);
            }

            // now use it
            long nu_start = System.nanoTime();
            for (String ua: TestData.USER_AGENTS){
                client.lookupUseragent(ua);
            }
            long elapsedWithCache = System.nanoTime() - nu_start;

            // Cache must be at least an order of magnitude faster
            assertTrue(elapsedNoCache > elapsedWithCache * 10);

        }
        finally {
            client.destroyConnection();
        }
    }

    @Test
    public void realCacheUsageTest_2() throws WmException {
        String host = "localhost";
        String port = "8080";
        String envHost = System.getenv("WM_HOST");
        String envPort = System.getenv("WM_PORT");
        if (StringUtils.isNotEmpty(envHost)){
            host = envHost;
        }
        if(StringUtils.isNotEmpty(envPort)){
            port = envPort;
        }
        WmClient client = WmClient.create("http", host, port, "");
        try {
            long start = System.nanoTime();
            for (String ua: TestData.USER_AGENTS){
                client.lookupUseragent(ua);
            }
            long elapsedNoCache = System.nanoTime() - start;
            double avgNoCache = (double)elapsedNoCache/(double)TestData.USER_AGENTS.length;

            // Now, let's add a cache layer
            client.setCacheSize(100000);

            // fill cache
            for (String ua: TestData.USER_AGENTS){
                client.lookupUseragent(ua);
            }

            // now use it
            long nu_start = System.nanoTime();
            for (String ua: TestData.USER_AGENTS){
                client.lookupUseragent(ua);
            }
            long elapsedWithCache = System.nanoTime() - nu_start;
            double avgWithCache = (double)elapsedWithCache/(double)TestData.USER_AGENTS.length;

            // Cache must be at least an order of magnitude faster
            assertTrue(avgNoCache > avgWithCache * 10);

        }
        finally {
            client.destroyConnection();
        }
    }



    static List<Callable<Boolean>> createLookupTasks(int numTasks, final WmClient client) {
        final Map<String,String> testData = createExpectedValueMap(client);
        List<Callable<Boolean>> ltasks = new ArrayList<>(numTasks);
        for (int i = 0; i < numTasks; i++) {
            final int tindex = i;
            ltasks.add(() -> {
                System.out.println("Starting task#: " + tindex);
                int c = 0;
                try {
                    for (String line: testData.keySet()) {
                        Model.JSONDeviceData d = client.lookupUseragent(line);
                        assertNotNull(d);
                        assertEquals(d.capabilities.get("wurfl_id"), testData.get(line));
                        c++;
                    }
                    System.out.println("Lines read from terminated task #" + tindex + ": " + c);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            });
        }
        return ltasks;
    }

    // Creates a test client with a cache of the given size.
    public WmClient createTestCachedClient(int csize) throws WmException {
        String host = "localhost";
        String port = "8080";
        String envHost = System.getenv("WM_HOST");
        String envPort = System.getenv("WM_PORT");
        if (StringUtils.isNotEmpty(envHost)){
            host = envHost;
        }
        if(StringUtils.isNotEmpty(envPort)){
            port = envPort;
        }
        WmClient client = WmClient.create("http", host, port, "");
        client.setCacheSize(csize);
        Assert.assertNotNull(client);
        return client;
    }

    // Uses reflection to force invoke of private method clearCacheIfNeeded for testing purposes
    private void invokeClearCacheIfNeeded(WmClient client, String ltime) throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        Class clientClass = Class.forName("com.scientiamobile.wurfl.wmclient.WmClient");
        Method ms = clientClass.getDeclaredMethod("clearCachesIfNeeded", String.class);
        ms.setAccessible(true);
        ms.invoke(client, ltime);
        ms.setAccessible(false);
    }

    private HttpServletRequest createTestRequest(final boolean provideHeaders) {
        return new HttpServletRequest() {

            private Map<String, String> headers = new HashMap<>();
            private String ua = "Mozilla/5.0 (Nintendo Switch; WebApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341";
            private String xucbr = "Mozilla/5.0 (Nintendo Switch; ShareApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341";
            private String dstkUa = "Mozilla/5.0 (Nintendo Switch; WifiWebAuthApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341";


            @Override
            public String getAuthType() {
                return null;
            }

            @Override
            public Cookie[] getCookies() {
                return new Cookie[0];
            }

            @Override
            public long getDateHeader(String s) {
                return 0;
            }

            @Override
            public String getHeader(String key) {
                fillHeadersIfNeeded();
                return headers.get(key.toLowerCase());
            }

            private void fillHeadersIfNeeded() {
                // al headers are put lowercase to emulate real http servlet request behaviour
                if (MapUtils.isEmpty(headers) && provideHeaders) {
                    headers.put("User-Agent".toLowerCase(), ua);
                    headers.put("Content-Type".toLowerCase(), "gzip, deflate");
                    headers.put("Accept-Encoding".toLowerCase(), "application/json");
                    headers.put("X-UCBrowser-Device-UA".toLowerCase(), xucbr);
                    headers.put("Device-Stock-UA".toLowerCase(), dstkUa);
                }
            }

            @Override
            public Enumeration getHeaders(String s) {
                if (provideHeaders) {
                    fillHeadersIfNeeded();
                    return new IteratorEnumeration(headers.keySet().iterator());
                }
                return new IteratorEnumeration(EmptyIterator.INSTANCE);
            }

            @Override
            public Enumeration getHeaderNames() {
                fillHeadersIfNeeded();
                return new IteratorEnumeration(headers.keySet().iterator());
            }

            @Override
            public int getIntHeader(String s) {
                return 0;
            }

            @Override
            public String getMethod() {
                return null;
            }

            @Override
            public String getPathInfo() {
                return null;
            }

            @Override
            public String getPathTranslated() {
                return null;
            }

            @Override
            public String getContextPath() {
                return null;
            }

            @Override
            public String getQueryString() {
                return null;
            }

            @Override
            public String getRemoteUser() {
                return null;
            }

            @Override
            public boolean isUserInRole(String s) {
                return false;
            }

            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public String getRequestedSessionId() {
                return null;
            }

            @Override
            public String getRequestURI() {
                return null;
            }

            @Override
            public StringBuffer getRequestURL() {
                return null;
            }

            @Override
            public String getServletPath() {
                return null;
            }

            @Override
            public HttpSession getSession(boolean b) {
                return null;
            }

            @Override
            public HttpSession getSession() {
                return null;
            }

            @Override
            public boolean isRequestedSessionIdValid() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromCookie() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromURL() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromUrl() {
                return false;
            }

            @Override
            public Object getAttribute(String s) {
                return null;
            }

            @Override
            public Enumeration getAttributeNames() {
                return null;
            }

            @Override
            public String getCharacterEncoding() {
                return null;
            }

            @Override
            public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

            }

            @Override
            public int getContentLength() {
                return 0;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public ServletInputStream getInputStream() {
                return null;
            }

            @Override
            public String getParameter(String s) {
                return null;
            }

            @Override
            public Enumeration getParameterNames() {
                return null;
            }

            @Override
            public String[] getParameterValues(String s) {
                return new String[0];
            }

            @Override
            public Map getParameterMap() {
                return null;
            }

            @Override
            public String getProtocol() {
                return null;
            }

            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public String getServerName() {
                return null;
            }

            @Override
            public int getServerPort() {
                return 0;
            }

            @Override
            public BufferedReader getReader() {
                return null;
            }

            @Override
            public String getRemoteAddr() {
                return null;
            }

            @Override
            public String getRemoteHost() {
                return null;
            }

            @Override
            public void setAttribute(String s, Object o) {

            }

            @Override
            public void removeAttribute(String s) {

            }

            @Override
            public Locale getLocale() {
                return null;
            }

            @Override
            public Enumeration getLocales() {
                return null;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public RequestDispatcher getRequestDispatcher(String s) {
                return null;
            }

            @Override
            public String getRealPath(String s) {
                return null;
            }

            @Override
            public int getRemotePort() {
                return 0;
            }

            @Override
            public String getLocalName() {
                return null;
            }

            @Override
            public String getLocalAddr() {
                return null;
            }

            @Override
            public int getLocalPort() {
                return 0;
            }
        };
    }

    private static Map<String,String> createExpectedValueMap(WmClient client){
        String[] userAgentList = TestData.createTestUserAgentList();
        Map<String,String> m = new HashMap<>();
        for(String ua: userAgentList){
            try {
                m.put(ua, client.lookupUseragent(ua).capabilities.get("wurfl_id"));
            } catch (WmException e) {
                Assert.fail(e.getMessage());
            }
        }
        return m;
    }

}
