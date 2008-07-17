package org.opencastproject.core.api.model;

import java.util.Set;

/**
 * A destination to publish media.
 */
public interface DistributionChannel {

	/**
	 * The unique identifier for this {@link DistributionChannel}
	 * @return
	 */
	public String getId();

	/**
	 * The description for this {@link DistributionChannel}
	 * @return
	 */
	public String getDescription();
	
	/**
	 * The {@link DistributionChannelCapability}s for this {@link DistributionChannel}
	 * @return
	 */
	public Set<DistributionChannelCapability> getCapabilities();
}
