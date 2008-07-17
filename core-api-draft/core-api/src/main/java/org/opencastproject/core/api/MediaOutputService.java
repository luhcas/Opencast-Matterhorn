package org.opencastproject.core.api;

import java.util.Set;

import org.opencastproject.core.api.model.DistributionChannel;

/**
 * This service provides a means to query and manage the {@link DistributionChannel}s
 * registered with the core.
 */
public interface MediaOutputService {
	/**
	 * Gets all currently registered {@link DistributionChannel}s
	 * @return
	 */
	public Set<DistributionChannel> getDistributionChannels();
	
	/**
	 * Adds a new {@link DistributionChannel}
	 * @param description The description for the new {@link DistributionChannel}
	 * @return The ID of the newly registered {@link DistributionChannel}
	 */
	public String addDistributionChannel(String description);
	
	/**
	 * Removes the registration for an existing {@link DistributionChannel}
	 * @param distributionChannelId The ID for the {@link DistributionChannel}
	 * @return Whether the channel existed, and was therefore removed, or not
	 */
	public boolean removeDistributionChannel(String distributionChannelId);
}
