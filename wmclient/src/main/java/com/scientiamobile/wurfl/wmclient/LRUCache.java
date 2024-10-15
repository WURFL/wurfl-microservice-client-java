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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches JSONDeviceData using string keys.<br>
 * The implementation is internally synchronized, as it is backed by a SynchronizedMap.<br>
 * Created by Andrea Castello on 11/09/2017.
 */
class LRUCache<K, E> {

    private final static int DEFAULT_SIZE = 20000;
    // We'll use this object to lock
    private final Object mutex;

    private final int size;

    private final ConcurrentHashMap<K, Node> cache;
    private Node head;
    private Node tail;

    /**
     * Created an instance of LRUCache with the given maximum size.<br>
     *
     * @param maxSize The cache's maximum size
     */
    LRUCache(int maxSize) {
        if (maxSize > 0) {
            this.size = maxSize;
        } else {
            this.size = DEFAULT_SIZE;
        }
        this.cache = new ConcurrentHashMap<>(maxSize, 0.75f, 64);
        this.mutex = this;
    }

    /**
     * Created an instance of LRUCache with the default maximum size.<br>
     */
    LRUCache() {
        this(DEFAULT_SIZE);
    }

    /**
     * Returns the element mapped to the given key, or null if key does not exist in cache
     *
     * @param key the cache key
     * @return the cache entry
     */
    E getEntry(K key) {
        synchronized (mutex) {
            Node entry = cache.get(key);
            if (entry == null) {
                return null;
            }

            // Since it has been used now, we send the entry to the head
            moveToHead(entry);
            return entry.value;
        }
    }

    /**
     * Removes all elements from cache.
     */
    void clear() {
        synchronized (mutex) {
            cache.clear();
            head = null;
            tail = null;
        }
    }

    /**
     * Puts the entry device in cache.
     *
     * @param key   the cache key
     * @param value the value to be cached
     */
    void putEntry(K key, E value) {
        synchronized (mutex) {
            Node entry = cache.get(key);

            if (entry == null) {
                entry = new Node(key, value);
                if (size() == this.size) {

                    cache.remove(tail.key);

                    tail = tail.previous;
                    if (tail != null) tail.next = null;
                }
                cache.put(key, entry);
            }

            entry.value = value;
            moveToHead(entry);
            if (tail == null) tail = head;
        }
    }

    int size() {
        synchronized (mutex) {
            return cache.size();
        }
    }

    // moves the given entry to the head of the cache
    private void moveToHead(Node entry) {
        if (entry == head || entry == null) return;

        Node next = entry.next;
        Node previous = entry.previous;

        if (next != null) next.previous = entry.previous;
        if (previous != null) previous.next = entry.next;

        entry.previous = null;


        entry.next = head;
        if (head != null) head.previous = entry;
        head = entry;

        if (tail == entry) tail = previous;
    }

    // represents a node in the internal cache, holding references to its previous and next elements
    private class Node {
        Node(K key, E value) {
            this.key = key;
            this.value = value;
        }

        private Node next;
        private Node previous;
        private K key;
        private E value;
    }
}