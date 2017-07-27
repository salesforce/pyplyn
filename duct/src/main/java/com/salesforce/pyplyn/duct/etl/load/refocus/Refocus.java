/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.load.refocus;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transmutation;
import com.salesforce.refocus.model.Link;
import com.salesforce.refocus.model.Sample;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Refocus load destination model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableRefocus.class)
@JsonSerialize(as = ImmutableRefocus.class)
public abstract class Refocus implements Load {
    private static final long serialVersionUID = 7327981995594592996L;

    /**
     * Endpoint where the results should be published
     */
    public abstract String endpoint();

    /**
     * Subject to use for the sample
     */
    public abstract String subject();

    /**
     * Aspect to use for the sample
     */
    public abstract String aspect();

    /**
     * Message code to publish by default, if {@link Transmutation#metadata()} does not contain one
     */
    @Nullable
    public abstract String defaultMessageCode();

    /**
     * Message body to publish by default, if {@link Transmutation#metadata()} does not contain any messages
     */
    @Nullable
    public abstract String defaultMessageBody();

    /**
     * List of related links to associate to the published {@link Sample}
     */
    public abstract List<Link> relatedLinks();

    /**
     * @return the Refocus standardized sample name (i.e.: SUBJECT.PATH|ASPECT)
     */
    @Value.Auxiliary
    public final String name() {
        return String.format("%s|%s", subject(), aspect());
    }
}
