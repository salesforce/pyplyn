/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstracts metadata that is passed in {@link TransformationResult} objects
 * <p/>
 * <p/>This class is immutable, use ETLMetadata.Builder to construct new objects by composition.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ETLMetadata {
    private final String messageCode;
    private final List<String> messages;
    private final Map<String, String> tags;

    /**
     * Class constructor
     */
    public ETLMetadata(String messageCode, List<String> messages, Map<String, String> tags) {
        this.messageCode = messageCode;
        this.messages = Collections.unmodifiableList(messages);
        this.tags = Collections.unmodifiableMap(tags);
    }

    /**
     * @return message code passed
     */
    public String messageCode() {
        return messageCode;
    }

    /**
     * @return all defined messages
     */
    public List<String> messages() {
        return messages;
    }

    /**
     * @return all defined tags
     */
    public Map<String, String> tags() {
        return tags;
    }
}
