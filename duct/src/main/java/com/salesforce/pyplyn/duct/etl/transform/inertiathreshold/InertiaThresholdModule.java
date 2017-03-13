package com.salesforce.pyplyn.duct.etl.transform.inertiathreshold;

import com.google.inject.AbstractModule;
import com.salesforce.pyplyn.util.MultibinderFactory;

/**
 * Defines the {@link InertiaThreshold} transform binding
 *
 * @author Jing Qian &lt;jqian@salesforce.com&gt;
 * @since 5.1
 */
public class InertiaThresholdModule extends AbstractModule {

	@Override
	protected void configure() {
		MultibinderFactory.transformFunctions(binder()).addBinding().toInstance(InertiaThreshold.class);
	}

}
