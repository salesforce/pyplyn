package com.salesforce.pyplyn.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

import javax.annotation.Nullable;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Specifies the base contract required for accessing connector methods
 * <p/>
 * <p/><strong>NOTE: </strong>Any implementing classes should delegate equals(Object) and hashCode()
 *   to this class' delegateEquals(Object) and delegateHashCode() methods, to ensure compatibility with
 *   the configuration provider
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public interface ConnectorInterface {
    /**
     * @return a unique value, used to identify the connector to use in the
     * specified {@link Configuration} (Configuration.endpoint should match id())
     */
    String id();

    /**
     * @return the URL to the API's endpoint
     */
    @Value.Auxiliary
    String endpoint();

    /**
     * @return username to authenticate against
     */
    @Nullable
    @Value.Auxiliary
    String username();

    /**
     * @return password to authenticate with
     */
    @Value.Auxiliary
    byte[] password();

    /**
     * If using an HTTP proxy, specify the hostname (without protocol information)
     * <p/> i.e.: proxy-host.example.com
     *
     * @return null if not using a proxy
     */
    @Nullable
    @Value.Auxiliary
    String proxyHost();

    /**
     * If using an HTTP proxy, specify the port
     * <p/>i.e.: 8080
     *
     * @return -1 if not using a proxy
     */
    @Value.Default
    @Value.Auxiliary
    default int proxyPort() {
        return -1;
    }

    /**
     * How long to wait for connections
     */
    @Value.Default
    @Value.Auxiliary
    default long connectTimeout() {
        return 10L;
    }

    /**
     * How long to wait for reads
     */
    @Value.Default
    @Value.Auxiliary
    default long readTimeout() {
        return 10L;
    }

    /**
     * How long to wait for writes
     */
    @Value.Default
    @Value.Auxiliary
    default long writeTimeout() {
        return 10L;
    }

    /**
     * Helper method that determines if a valid proxy configuration was specified
     *
     * @return true if the EndpointConnector has valid proxy settings
     */
    @JsonIgnore
    @Value.Auxiliary
    default boolean isProxyEnabled() {
        return !isNullOrEmpty(proxyHost()) && proxyPort() > 0;
    }

    /**
     * Object.equals implementation, used to ensure we don't have more than one connector with the same id
     */
    default boolean delegateEquals(Object another) {
        return this == another || (another instanceof Connector && id().equals(((Connector) another).id()));
    }

    /**
     * Object.hashCode implementation, used to ensure we don't have more than one connector with the same id
     */
    default int delegateHashCode() {
        int h = 5381;
        h += (h << 5) + id().hashCode();
        return h;
    }
}
