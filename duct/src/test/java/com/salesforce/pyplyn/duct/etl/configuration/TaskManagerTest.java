/*
 * Copyright, 1999-2018, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */
package com.salesforce.pyplyn.duct.etl.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;


/**
 * TaskManagerTest
 *
 * @author Chris Coraggio &lt;chris.coraggio@salesforce.com&gt;
 * @since 11.0.0
 */
public class TaskManagerTest {

    private AppBootstrapFixtures fixtures;
    private ArgumentCaptor<Configuration> configurationCaptor;

    @BeforeMethod
    public void setUp() throws Exception {
        fixtures = new AppBootstrapFixtures();
        configurationCaptor = ArgumentCaptor.forClass(Configuration.class);
    }

    /**
     * Ensures the same task is passed into the delay function and the task creation function
     */
    @Test
    public void testDelayOnTaskCreation() throws InterruptedException {
        // ARRANGE
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfiguration()
                .initializeFixtures();

        ConfigurationUpdateManager manager = fixtures.configurationManager();

        // ACT
        manager.run();
        manager.awaitUntilConfigured();

        // ASSERT
        verify(fixtures.configurationManager()).initialize();
        verify(fixtures.taskManager(), times(1)).createTask(configurationCaptor.capture());
        Configuration originalTask = configurationCaptor.getValue();
        verify(fixtures.taskManager(), times(1)).getTaskDelayMillis(configurationCaptor.capture());
        Configuration delayedTask = configurationCaptor.getValue();
        assertThat("Task created should be the same as the task delayed", originalTask.equals(delayedTask));

    }

}
