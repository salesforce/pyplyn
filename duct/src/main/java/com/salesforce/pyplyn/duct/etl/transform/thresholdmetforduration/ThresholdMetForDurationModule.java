package com.salesforce.pyplyn.duct.etl.transform.thresholdmetforduration;

import com.google.inject.AbstractModule;
import com.salesforce.pyplyn.util.MultibinderFactory;

/**
 * Defines the {@link ThresholdMetForDuration} transform binding
 *
 * @author Jing Qian &lt;jqian@salesforce.com&gt;
 * @since 6.0
 */
public class ThresholdMetForDurationModule extends AbstractModule {
	@Override
	protected void configure() {
		MultibinderFactory.transformFunctions(binder()).addBinding().toInstance(ThresholdMetForDuration.class);
	}
}
