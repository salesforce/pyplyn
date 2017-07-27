package com.salesforce.pyplyn.duct.providers.jackson;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.inject.*;
import com.salesforce.pyplyn.duct.etl.ETLConfigDeserializer;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transform;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Initializes the {@link ObjectMapper} required by Pyplyn transforms
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
        // configure mapper returned by this module to be aware of Pyplyn's deserializers
        SimpleModule module = new SimpleModule(this.getClass().getSimpleName());
        module.addDeserializer(Extract.class, new ETLConfigDeserializer<>(Extract.class, "format", extractFormats));
        module.addDeserializer(Transform.class, new ETLConfigDeserializer<>(Transform.class, "name", transformFormats));
        module.addDeserializer(Load.class, new ETLConfigDeserializer<>(Load.class, "format", loadFormats));
        registerModule(module);

        // set default serialization/deserialization features
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
                .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .setSerializationInclusion(NON_NULL)
                .registerModule(module);

        // set default printer features
        DefaultPrettyPrinter jsonPrinter = new DefaultPrettyPrinter();
        jsonPrinter.indentArraysWith(new DefaultIndenter());
        mapper.setDefaultPrettyPrinter(jsonPrinter);

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
