package com.salesforce.pyplyn.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.*;

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
        additionalJsonAnnotations = {JsonInclude.class, JsonFormat.class, JsonIgnore.class, JsonIgnoreProperties.class, JsonTypeName.class},
        defaults = @Value.Immutable(copy = false)
)
public @interface PyplynImmutableStyle {}
