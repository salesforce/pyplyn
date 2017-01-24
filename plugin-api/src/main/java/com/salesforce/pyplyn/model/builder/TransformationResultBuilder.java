/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.model.builder;

import com.salesforce.pyplyn.model.TransformationResult;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

/**
 * Handles cloning and modifying {@link TransformationResult} objects
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class TransformationResultBuilder {
    private ZonedDateTime time;
    private String name;
    private Number value;
    private Number originalValue;
    private ETLMetadataBuilder metadata;


    /**
     * Class constructor used for cloning objects
     */
    public TransformationResultBuilder(TransformationResult result) {
        this.time = result.time();
        this.name = result.name();
        this.value = result.value();
        this.originalValue = result.originalValue();
        this.metadata = new ETLMetadataBuilder(result.metadata());
    }

    /**
     * @return a new instance of {@link TransformationResult} with the updated values
     */
    public TransformationResult build() {
        return new TransformationResult(time, name, value, originalValue, metadata.build());
    }


    /* Setters */

    public TransformationResultBuilder withTime(ZonedDateTime time) {
        this.time = time;
        return this;
    }

    public TransformationResultBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public TransformationResultBuilder withValue(Number value) {
        this.value = value;
        return this;
    }

    public TransformationResultBuilder withOriginalValue(Number originalValue) {
        this.originalValue = originalValue;
        return this;
    }

    /**
     * Using a consumer is needed to keep the builder's fluent interface
     */
    public TransformationResultBuilder metadata(Consumer<ETLMetadataBuilder> metadata) {
        metadata.accept(this.metadata);
        return this;
    }
}


