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

/**
 * WmException is a general purpouse exception throws whenever an unrecoverable error occurs during device detection (ie: no connection available to WM server,
 * wrong url or port configurations, etc.
 */
public class WmException extends Exception {

    /**
     * Creates a WmClientException with the given error message
     *
     * @param message the error message to set
     */
    public WmException(String message) {
        super(message);
    }

    /**
     * Creates a WmException with the given error message and
     * the exception that caused the current one. (ie: if uri is invalid, innerException could be an UriSyntaxException)
     *
     * @param message error message
     * @param cause   exception that causes the current one
     */
    public WmException(String message, Throwable cause) {
        super(message, cause);
    }

}
