/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.cache;

/**
 * Classes whose instances can be cached should implement this interface and provide a endpoint() implementation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public interface Cacheable {

    /**
     * @return unique cache identifier (key)
     */
    String cacheKey();
}
