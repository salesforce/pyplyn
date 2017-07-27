package com.salesforce.pyplyn.annotations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Style to apply to all immutables
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@Value.Style(
        allParameters = true,
        jdkOnly = true,
        overshadowImplementation = true,
        redactedMask = "####",
        additionalJsonAnnotations = {JsonInclude.class, JsonIgnore.class, JsonIgnoreProperties.class},
        defaults = @Value.Immutable(copy = false)
)
public @interface PyplynImmutableStyle {}
