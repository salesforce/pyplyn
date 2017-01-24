/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.configuration;

import com.google.inject.Provider;

import java.util.Set;

/**
 * Contract for loading the list of configurations to process
 *   allowing the program to run in single host mode, or as part of a cluster
 * <p/>
 * <p/>The implementing class will be run by an {@link java.util.concurrent.ExecutorService} at a specified interval
 *   and ensure Pyplyn is processing the latest {@link Configuration}s
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public interface UpdatableConfigurationSetProvider<T> extends Provider<Set<T>>, Runnable {

    /**
     * Updates the configuration set from the specified configuration {@link Provider}
     */
    void updateConfigurations();
}
