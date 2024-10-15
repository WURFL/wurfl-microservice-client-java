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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Created by Andrea Castello on 12/09/2017.
 */
public class LRUCacheTest {

    @Test
    public void multithreadAddAndCheckTest() throws Exception {
        LRUCache<String, Object> cache = new LRUCache<String, Object>(1000);
        List<CallableTestTask<Boolean>> tasks = createAddAndCheckTasks(32, cache);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(32);
        List<Future<Boolean>> futures = executorService.invokeAll(tasks, 5, TimeUnit.MINUTES);
        int countDone = 0;
        for (Future<Boolean> f : futures) {

            if (f.isDone()) {
                Assert.assertTrue(f.get());
                countDone++;
            }
        }
        Assert.assertEquals(countDone, futures.size());

        int linesRead = -1;
        for (int i = 0; i < tasks.size(); i++) {
            if (i > 0) {
                // Check that some of the thread have not exited abruptly for some reason
                Assert.assertEquals(linesRead, tasks.get(i).readLines);
            }
            linesRead = tasks.get(i).readLines;
        }
        Assert.assertEquals(cache.size(), 100);
    }

    @Test
    public void multithreadGetAndClearCacheTest() throws Exception {
        LRUCache<String, Object> cache = new LRUCache<String, Object>(1000);
        List<CallableTestTask<Boolean>> tasks = createGetClearOrPutTasks(32, cache);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(32);
        List<Future<Boolean>> futures = executorService.invokeAll(tasks, 5, TimeUnit.MINUTES);
        int countDone = 0;
        for (Future<Boolean> f : futures) {

            if (f.isDone()) {
                Assert.assertTrue(f.get());
                countDone++;
            }
        }
        Assert.assertEquals(countDone, futures.size());

        int linesRead = -1;
        for (int i = 0; i < tasks.size(); i++) {
            if (i > 0) {
                // Check that some of the thread have not exited abruptly for some reason
                Assert.assertEquals(linesRead, tasks.get(i).readLines);
            }
            linesRead = tasks.get(i).readLines;
        }
        Assert.assertTrue(cache.size() == 0 || cache.size() == 100);
    }

    @Test
    public void removeOnMaxSizeTest() {
        LRUCache<String, Integer> cache = new LRUCache<String, Integer>(5);
        for (int i = 0; i < 6; i++) {
            cache.putEntry(String.valueOf(i), i);
        }
        Assert.assertEquals(cache.size(), 5);
        // "0" entry has been removed when inserting "6"
        Assert.assertNull(cache.getEntry("0"));
    }

    @Test
    public void replaceOnMultiAddTest() {
        LRUCache<String, Integer> cache = new LRUCache<String, Integer>(5);
        for (int i = 0; i < 6; i++) {
            cache.putEntry(String.valueOf(i), i);
        }

        // re-add element with different value
        cache.putEntry("3", 7);
        Assert.assertEquals(cache.size(), 5);
        // "0" entry has been removed when inserting "6"
        Assert.assertEquals(cache.getEntry("3"), new Integer(7));
    }

    @Test
    public void consistencyTest() {
        Random random = new Random();
        int cacheFinds = 0;
        Integer j;
        Integer[] values = new Integer[10000];
        LRUCache<Integer, Integer> cache = new LRUCache<Integer, Integer>(values.length / 10);

        for (Integer i : values) {
            j = random.nextInt() % values.length;
            Integer cvalue = cache.getEntry(j);
            if (cvalue == null) {
                cache.putEntry(j, j);
            } else {
                cacheFinds++;
                Assert.assertEquals(cvalue, j);
            }
        }
        System.out.println("Cache finds: " + cacheFinds);
    }

    @Test
    public void replaceExistingItemTest() {
        LRUCache<String, Integer> cache = new LRUCache<String, Integer>(5);
        for (int i = 0; i < 5; i++) {
            cache.putEntry(String.valueOf(i), i);
        }
        Assert.assertEquals(cache.size(), 5);
        // Now put an element with the same key and a different value
        cache.putEntry("2", 159);
        // previous value has been overwritten
        Assert.assertEquals(cache.getEntry("2"), new Integer(159));
    }

    private List<CallableTestTask<Boolean>> createAddAndCheckTasks(int numTasks, final LRUCache<String, Object> cache) {
        final String[] userAgentList = TestData.createTestUserAgentList();
        List<CallableTestTask<Boolean>> tasks = new ArrayList<CallableTestTask<Boolean>>(numTasks);
        for (int i = 0; i < numTasks; i++) {
            final int tindex = i;
            tasks.add(new CallableTestTask<Boolean>(i, userAgentList) {
                @Override
                public Boolean call() throws Exception {
                    System.out.println("Starting task#: " + tindex);
                    try {
                        for (String line : userAgentList) {
                            cache.putEntry(line, new Object());
                            Object val = cache.getEntry(line);
                            Assert.assertNotNull(val);
                            readLines++;
                        }
                        System.out.println("Lines read from terminated task #" + tindex + ": " + readLines);
                        success = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        success = false;
                    }
                    return success;
                }
            });
        }
        return tasks;
    }

    private List<CallableTestTask<Boolean>> createGetClearOrPutTasks(int numTasks, final LRUCache<String, Object> cache) {
        List<CallableTestTask<Boolean>> tasks = new ArrayList<CallableTestTask<Boolean>>(numTasks);
        final String[] userAgentList = TestData.createTestUserAgentList();
        for (int i = 0; i < numTasks; i++) {
            final int tindex = i;
            tasks.add(new CallableTestTask<Boolean>(i, userAgentList) {
                @Override
                public Boolean call() throws Exception {
                    System.out.println("Starting task#: " + tindex);
                    try {
                        readLines = 0;
                        for (String line : userAgentList) {

                            cache.getEntry(line);
                            // every 300 detections and only on even threads we clear cache to check it does not
                            readLines++;

                            // every 300 and only on even threads, we clear cache, to see if getEntry handles it without raising errors
                            if (readLines % 300 == 0 && tindex % 2 == 0) {
                                cache.clear();
                            } else if (tindex % 2 != 0) {
                                cache.putEntry(line, new Object());
                            }
                        }
                        System.out.println("Lines read from terminated task #" + tindex + ": " + readLines);
                        success = true;

                    } catch (Exception e) {
                        Assert.fail("Test failed due to exception :" + e.getMessage(), e);
                        success = false;
                    }
                    return success;
                }
            });
        }
        return tasks;
    }

    abstract class CallableTestTask<V> implements Callable<V> {
        int taskIndex;
        String[] userAgents;
        boolean success = false;
        int readLines;

        public CallableTestTask(int taskIndex, String[] userAgents) {
            this.taskIndex = taskIndex;
            this.userAgents = userAgents;
        }
    }
}