package org.opencastproject.core.api.model;

/**
 * Describes capabilities (what this means is still to be determined) for {@link DistributionChannel}s
 */
public interface DistributionChannelCapability {
	/**
	 * The unique identifier for this {@link DistributionChannelCapability}
	 * @return
	 */
	public String getId();

	/**
	 * The description for this {@link DistributionChannelCapability}
	 * @return
	 */
	public String getDescription();
}
