/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.providers.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.salesforce.pyplyn.util.ObjectMapperWrapper;
import com.salesforce.pyplyn.util.SerializationHelper;

/**
 * Configures the modular configuration deserializers based on the Jackson library
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class JacksonSerializationInitModule extends AbstractModule {
    @Override
    protected void configure() {
        // bind the Jackson mapper
        bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);

        // bind ObjectMapperWrapper, which provides additional functions using the Jackson mapper
        bind(SerializationHelper.class).to(ObjectMapperWrapper.class).asEagerSingleton();
    }
}
