package com.salesforce.pyplyn.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.testng.annotations.Test;

import java.nio.charset.Charset;

/**
 * Test that validation of mutual authentication configuration validation works and that
 * determination of whether it is configured is accurate.
 *
 * @author thomas.harris
 * @since 10.0
 */
public class MutualAuthConfigurationTest {
    
    @Test
    public void testValidConfiguration() {
        
        // ARRANGE
        
        Connector connector = ImmutableConnector.builder()
                .id("valid-connector")
                .endpoint("https://mutual-auth.com")
                .username("trusted.user")
                .password("test1234".getBytes())
                .keystorePath("/path/to/keystore")
                .keystorePassword("changeit".getBytes(Charset.defaultCharset()))
                .build();
        
        // ASSERT
        
        assertThat(connector.isMutualAuthEnabled(), is(true));
    }
    
    @Test
    public void testNotConfigured() {
        // ARRANGE
        
        Connector connector = ImmutableConnector.builder()
                .id("valid-connector")
                .endpoint("https://mutual-auth.com")
                .username("trusted.user")
                .password("test1234".getBytes()).build();
        
        // ASSERT
        
        assertThat(connector.isMutualAuthEnabled(), is(false));
    }
    
    @Test(expectedExceptions = IllegalStateException.class)
    public void testPartiallyConfigured() {
        // ARRANGE
        
        ImmutableConnector.builder()
                .id("valid-connector")
                .endpoint("https://mutual-auth.com")
                .username("trusted.user")
                .password("test1234".getBytes())
                .keystorePath("/path/to/keystore").build();
    }
    
}
