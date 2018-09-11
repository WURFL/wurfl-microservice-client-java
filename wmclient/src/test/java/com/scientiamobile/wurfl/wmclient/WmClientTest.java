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

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.*;

import static org.testng.Assert.*;

public class WmClientTest {

    // internal instance that can be reused. Not for create tests.
    private WmClient _client;

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

    public static List<Callable<Boolean>> createLookupTasks(int numTasks, final WmClient client) {
        List<Callable<Boolean>> ltasks = new ArrayList<Callable<Boolean>>(numTasks);
        for (int i = 0; i < numTasks; i++) {
            final int tindex = i;
            ltasks.add(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    System.out.println("Starting task#: " + tindex);
                    int c = 0;
                    try {
                        InputStreamReader reader = new InputStreamReader(new FileInputStream("ua.txt"));
                        BufferedReader br = new BufferedReader(reader);
                        String line;
                        while ((line = br.readLine()) != null) {
                            client.lookupUseragent(line);
                            c++;
                        }
                        System.out.println("Lines read from terminated task #" + tindex + ": " + c);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
        }
        return ltasks;
    }

    // Creates a test client with a cache of the given size.
    public WmClient createTestCachedClient(int csize) throws WmException {
        WmClient client = WmClient.create("http", "localhost", "8080", "");
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

            private Map<String, String> headers = new HashMap<String, String>();
            private String ua = "Mozilla/5.0 (Nintendo Switch; WebApplet) AppleWebKit/601.6 (KHTML, like Gecko) NF/4.0.0.5.9 NintendoBrowser/5.1.0.13341";


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
                return headers.get(key);
            }

            private void fillHeadersIfNeeded() {
                if (MapUtils.isEmpty(headers) && provideHeaders) {
                    headers.put("User-Agent", ua);
                    headers.put("Content-Type", "gzip, deflate");
                    headers.put("Accept-Encoding", "application/json");
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
            public ServletInputStream getInputStream() throws IOException {
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
            public BufferedReader getReader() throws IOException {
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
}