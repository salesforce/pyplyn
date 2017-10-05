package com.salesforce.pyplyn.duct.etl.transform.standard;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.model.ImmutableTransmutation;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.Transmutation;

/**
 * Defines tags in the {@link Transmutation}'s {@link Metadata}
 * <p/>
 * This plugin overrides any previously defined keys.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableMetadata.class)
@JsonSerialize(as = ImmutableMetadata.class)
@JsonTypeName("Metadata")
public abstract class Metadata implements Transform {
    private static final long serialVersionUID = 2563927446245611395L;

    public abstract Map<String, String> tags();

    /**
     * Processes all input {@link Transmutation} objects and appends the {@link #tags()} to each one
     */
    @Override
    public List<List<Transmutation>> apply(List<List<Transmutation>> input) {
        return input.stream()
            // for all result rows
            .map(datapoints -> datapoints.stream()
                    // for all result columns (datapoints)
                    .map(datapoint -> ImmutableTransmutation.builder()
                            .from(datapoint)
                            .metadata(ImmutableTransmutation.Metadata.builder()
                                    .from(datapoint.metadata())
                                    // append all metadata tags
                                    .putAllTags(tags())
                                    .build())
                            .build())
                    .collect(Collectors.toList())
            ).collect(Collectors.toList());
    }
}
