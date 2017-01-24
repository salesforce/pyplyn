/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.model.builder;

import com.salesforce.pyplyn.model.ETLMetadata;

import java.util.*;


/**
 * Handles creating new immutable Metadata objects
 * <p/>
 * <p/>The API of this builder class is different from all the other builders in the project
 *   as its use cases are also different and require composition more than replacement.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ETLMetadataBuilder {
    private String messageCode;
    private List<String> messages;
    private Map<String, String> tags;


    /**
     * Default constructor
     */
    public ETLMetadataBuilder() {
        messages = new ArrayList<>();
        tags = new HashMap<>();
    }

    /**
     * Class constructor used for cloning objects
     */
    public ETLMetadataBuilder(ETLMetadata metadata) {
        messageCode = metadata.messageCode();
        messages = new ArrayList<>(metadata.messages());
        tags = new HashMap<>(metadata.tags());
    }

    /**
     * Sets the message code
     */
    public ETLMetadataBuilder setMessageCode(String messageCode) {
        this.messageCode = messageCode;
        return this;
    }

    /**
     * Adds a message
     */
    public ETLMetadataBuilder addMessage(String message) {
        return addMessages(Collections.singletonList(message));
    }

    /**
     * Adds all pased messages
     */
    public ETLMetadataBuilder addMessages(List<String> messages) {
        this.messages.addAll(messages);
        return this;
    }

    /**
     * Reset messages to specified set
     */
    public ETLMetadataBuilder resetMessages() {
        this.messages.clear();
        return this;
    }

    /**
     * Creates a new tag
     * <p/>If called multiple times for the same (tag, name) pair, it will override previous values
     *
     * @param name  tag name
     * @param value
     * @return
     */
    public ETLMetadataBuilder addTag(String name, String value) {
        return addTags(Collections.singletonMap(name, value));
    }

    /**
     * Sets all passed tags
     * <p/>
     * <p/>If called multiple times for the same tag, it will override previous values.
     *
     * @param tags list of tags to set
     */
    public ETLMetadataBuilder addTags(Map<String, String> tags) {
        this.tags.putAll(tags);
        return this;
    }

    /**
     * @return an immutable ETL metadata object
     */
    public ETLMetadata build() {
        return new ETLMetadata(messageCode, messages, tags);
    }
}
