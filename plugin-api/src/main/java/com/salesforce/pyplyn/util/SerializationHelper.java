/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.*;

import static java.util.Objects.nonNull;

/**
 * Provides a common contract used by classes that perform serialization/deserialization operations
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public interface SerializationHelper {
    /**
     * Should serialize the passed <b>object</b> to a String
     */
    String serializeJson(Object object) throws JsonProcessingException;

    /**
     * Should deserialize a JSON from a {@link File} as an object of the specified class type
     */
    <T> T deserializeJsonFile(String filename, Class<T> cls) throws IOException;

    /**
     * Should deserialize a JSON from a {@link String} as an object of the specified class type
     */
    <T> T deserializeJsonString(String jsonData, Class<T> cls) throws IOException;

    /**
     * Should deserialize a JSON from an {@link InputStream} as an object of the specified class type
     */
    <T> T deserializeJsonStream(InputStream is, Class<T> cls) throws IOException;

    /**
     * Loads packaged resources or files from disk
     * <p/>
     * <p/><b>USE WITH CARE, as this method can read any path/file from the file system (as long as it has the rights to do so).</b>
     * <p/>
     * <p/>This method is used in the pyplyn project to load files or paths specified in the program's AppConfig.
     * <p/>Besides the file-system, it can read any resource that was packaged in the containing jar file.
     *
     * @param name Path to resource or file
     * @return InputStream representing the data
     * @throws FileNotFoundException if neither a readable resource or path were found
     */
    static InputStream loadResourceInsecure(String name) throws FileNotFoundException {
        // attempt to load from class path first
        InputStream is = SerializationHelper.class.getResourceAsStream(name);
        if (nonNull(is)) {
            return is;
        }

        // load from the filesystem, if the resource was not found
        return new FileInputStream(name);
    }

    /**
     * Returns true the specified resource can be opened
     *   or false if for whatever reason it is not accessible
     *   or {@link InputStream#close()} could not be called
     *
     * @param name Path to filename or package resource
     */
    static boolean canRead(String name) {
        try (InputStream is = loadResourceInsecure(name)) {
            return true;

        } catch (IOException e) {
            return false;
        }
    }
}