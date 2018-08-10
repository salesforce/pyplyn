package com.salesforce.pyplyn.duct.providers.jackson;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.inject.Binder;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transform;

/**
 * Initializes the {@link ObjectMapper} required by Pyplyn transforms.
 * <p>
 * The ultimate purpose of this class is to provide a Guice-enabled mapper anywhere that one
 * is injected in Pyplyn <i>and</i> to ensure that mapper also supports proper deserialization of
 * Pyplyn ETL objects. It does this by:
 * <ul>
 * <li>In its own constructor calling the paren't class' constructor with the {@link GuiceEnabledMapper} annotation
 * specified. This ensures that anywhere an instance of {@link ObjectMapper} is injected with that annotation
 * it will be one that supports Guice injection.</li>
 * <li>Implementing a provider method for {@link ObjectMapper} that will returns a mapper instance for <b>all</b>
 * places where it is injected, not just with the binding annotation. This method takes a mapper with the Guice
 * binding annotation as an argument, adds support for deserializing Pyplyn ETL objects to the Guice-enabled
 * mapper, then returns that mapper. This ensures that all injected instances of {@link ObjectMapper} support
 * both injection and the deserialization of ETL objects.</li>
 * </ul>
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public class PyplynObjectMapperModule extends ObjectMapperModule {
    @Inject
    Set<Class<? extends Extract>> extractFormats;

    @Inject
    Set<Class<? extends Transform>> transformFormats;

    @Inject
    Set<Class<? extends Load>> loadFormats;

    /**
     * Construct module and initialize with an annotation so that we can intercept the Mapper
     *   and configure serialization/deserialization options
     */
    public PyplynObjectMapperModule() {
        super(GuiceEnabledMapper.class);
    }

    /**
     * Register a Jackson module, configuring the Pyplyn deserializers
     */
    @Override
    public void configure(Binder binder) {
        super.configure(binder);

        binder.requestInjection(this);
    }

    /**
     * Augment the Guice enabled {@link ObjectMapper} with Pyplyn's deserializers and general features
     */
    @Provides
    @Singleton
    public ObjectMapper mapper(@GuiceEnabledMapper ObjectMapper mapper) {
        // set default serialization/deserialization features
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
                .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .setSerializationInclusion(NON_NULL);

        // set default printer features
        DefaultPrettyPrinter jsonPrinter = new DefaultPrettyPrinter();
        jsonPrinter.indentArraysWith(new DefaultIndenter());
        mapper.setDefaultPrettyPrinter(jsonPrinter);

        // configure the mapper returned by this module to be aware of ETL subtypes
        mapper.registerSubtypes(extractFormats.toArray(new Class[0]));
        mapper.registerSubtypes(transformFormats.toArray(new Class[0]));
        mapper.registerSubtypes(loadFormats.toArray(new Class[0]));

        return mapper;
    }

    /**
     * Annotation used to differentiate the {@link ObjectMapper} provided
     *   via the {@link ObjectMapperModule} Guice module
     */
    @BindingAnnotation
    @Target({FIELD, PARAMETER, METHOD})
    @Retention(RUNTIME)
    @interface GuiceEnabledMapper {}
}
