/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.load.refocus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.util.CollectionUtils;
import com.salesforce.refocus.model.Link;
import com.salesforce.refocus.model.Sample;

import java.io.Serializable;
import java.util.List;

import static com.salesforce.pyplyn.util.CollectionUtils.immutableOrEmptyList;

/**
 * Refocus load destination model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class Refocus implements Load, Serializable {
    private static final long serialVersionUID = 7327981995594592996L;

    @JsonProperty(required = true)
    private String endpoint;

    @JsonProperty(required = true)
    private String subject;

    @JsonProperty(required = true)
    private String aspect;

    @JsonProperty
    private String defaultMessageCode;

    @JsonProperty
    private String defaultMessageBody;

    @JsonProperty
    private List<Link> relatedLinks;


    /**
     * Default constructor
     */
    @JsonCreator
    public Refocus(@JsonProperty("endpoint") String endpoint,
                   @JsonProperty("subject") String subject,
                   @JsonProperty("aspect") String aspect,
                   @JsonProperty("defaultMessageCode") String defaultMessageCode,
                   @JsonProperty("defaultMessageBody") String defaultMessageBody,
                   @JsonProperty("relatedLinks") List<Link> relatedLinks) {
        this.endpoint = endpoint;
        this.subject = subject;
        this.aspect = aspect;
        this.defaultMessageCode = defaultMessageCode;
        this.defaultMessageBody = defaultMessageBody;
        this.relatedLinks = CollectionUtils.immutableListOrNull(relatedLinks);
    }

    /**
     * Endpoint where the results should be published
     */
    public String endpoint() {
        return endpoint;
    }

    /**
     * List of related links to associate to the published {@link Sample}
     */
    public List<Link> relatedLinks() {
        return immutableOrEmptyList(relatedLinks);
    }

    /**
     * Message code to publish by default, if {@link TransformationResult#metadata()} does not contain one
     */
    public String defaultMessageCode() {
        return defaultMessageCode;
    }

    /**
     * Message body to publish by default, if {@link TransformationResult#metadata()} does not contain any messages
     */
    public String defaultMessageBody() {
        return defaultMessageBody;
    }

    /**
     * @return the Refocus standardized sample name (i.e.: SUBJECT.PATH|ASPECT)
     */
    public final String name() {
        return String.format("%s|%s", subject, aspect);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Refocus refocus = (Refocus) o;

        if (!endpoint.equals(refocus.endpoint)) {
            return false;
        }
        if (!subject.equals(refocus.subject)) {
            return false;
        }

        return aspect.equals(refocus.aspect);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * endpoint.hashCode() + subject.hashCode()) + aspect.hashCode();
    }
}
