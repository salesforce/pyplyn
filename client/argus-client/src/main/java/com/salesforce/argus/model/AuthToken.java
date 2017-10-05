package com.salesforce.argus.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.util.SensitiveByteArrayDeserializer;
import com.salesforce.pyplyn.util.SensitiveByteArraySerializer;


/**
 * Argus auth tokens model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableAuthToken.class)
@JsonSerialize(as = ImmutableAuthToken.class)
@JsonInclude(NON_EMPTY)
public abstract class AuthToken {
    @Value.Redacted
    @JsonProperty(access = WRITE_ONLY)
    @JsonSerialize(using=SensitiveByteArraySerializer.class)
    @JsonDeserialize(using=SensitiveByteArrayDeserializer.class)
    public abstract byte[] accessToken();

    @Value.Redacted
    @JsonSerialize(using=SensitiveByteArraySerializer.class)
    @JsonDeserialize(using=SensitiveByteArrayDeserializer.class)
    public abstract byte[] refreshToken();
}
