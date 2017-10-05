package com.salesforce.pyplyn.configuration;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;

/**
 * Specifies the base contract required for accessing connector methods
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public abstract class EndpointConnector {
    /**
     * @return a unique value, used to identify the connector to use in the
     * specified {@link Configuration} (Configuration.endpoint should match id())
     */
    public abstract String id();

    /**
     * @return the URL to the API's endpoint
     */
    @Value.Auxiliary
    public abstract String endpoint();

    /**
     * @return username to authenticate against
     */
    @Nullable
    @Value.Auxiliary
    public abstract String username();

    /**
     * @return password to authenticate with
     */
    @Value.Auxiliary
    public abstract byte[] password();

    /**
     * If using an HTTP proxy, specify the hostname (without protocol information)
     * <p/> i.e.: proxy-host.example.com
     *
     * @return null if not using a proxy
     */
    @Nullable
    @Value.Auxiliary
    public abstract String proxyHost();

    /**
     * If using an HTTP proxy, specify the port
     * <p/>i.e.: 8080
     *
     * @return -1 if not using a proxy
     */
    @Value.Default
    @Value.Auxiliary
    public int proxyPort() {
        return -1;
    }

    /**
     * How long to wait for connections
     */
    @Value.Default
    @Value.Auxiliary
    public long connectTimeout() {
        return 10L;
    }

    /**
     * How long to wait for reads
     */
    @Value.Default
    @Value.Auxiliary
    public long readTimeout() {
        return 10L;
    }

    /**
     * How long to wait for writes
     */
    @Value.Default
    @Value.Auxiliary
    public long writeTimeout() {
        return 10L;
    }
    
    /**
     * Path to the keystore containing certificate to use for mutual authentication.
     */
    @Nullable
    public abstract String keystorePath();
    
    /**
     * Password for the keystore to use for mutual authentication.
     */
    @Nullable
    public abstract byte[] keystorePassword();

    /**
     * Specifies the {@link javax.net.ssl.SSLContext} algorithm to use for opening connections to this endpoint
     *
     * @return a value that corresponds to the
     *   <a href="http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext">Standard Algorithm Names for JDK 8</a>
     *   reference
     */
    @Value.Default
    public String sslContextAlgorithm() {
        return "TLSv1.2";
    }

    /**
     * Helper method that determines if a valid proxy configuration was specified
     *
     * @return true if the EndpointConnector has valid proxy settings
     */
    @JsonIgnore
    @Value.Auxiliary
    public final boolean isProxyEnabled() {
        return !isNullOrEmpty(proxyHost()) && proxyPort() > 0;
    }
    
    /**
     * Helper method to determine if valid TLS mutual authentication configuration was specified
     * 
     * @return true if a all keystore information was supplied, false otherwise
     */
    @JsonIgnore
    @Value.Auxiliary
    public final boolean isMutualAuthEnabled() {
        return !isNullOrEmpty(keystorePath()) && nonNull(keystorePassword()) && keystorePassword().length > 0;
    }
    
    @Value.Check
    protected void check() {
        Preconditions.checkState(isNull(keystorePath()) == isNull(keystorePassword()),
                "Keystore path (%s) and password (%s) for '%s' should be either both specified or none at all",
                keystorePath(),
                keystorePassword(),
                id()
        );
    }

    /**
     * Object.equals implementation, used to ensure we don't have more than one connector with the same id
     */
    @Override
    public boolean equals(Object another) {
        return this == another || (another instanceof EndpointConnector && id().equals(((EndpointConnector) another).id()));
    }

    /**
     * Object.hashCode implementation, used to ensure we don't have more than one connector with the same id
     */
    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + id().hashCode();
        return h;
    }
}
