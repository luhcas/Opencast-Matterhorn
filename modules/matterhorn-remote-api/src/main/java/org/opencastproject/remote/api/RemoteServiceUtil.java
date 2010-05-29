/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.remote.api;

import org.opencastproject.remote.api.Receipt.Status;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.bidimap.TreeBidiMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A utility for remote service proxies.
 */
public final class RemoteServiceUtil {
  /**
   * Finds the remote composer services, ordered by their load (lightest to heaviest).
   * 
   * @param receiptService The receipt service to use for looking up hosts that handle this kind of receipt
   * @param receiptType The type of receipt that must be handled by the hosts list returned
   * @return A list of hosts that handle this receipt type, in order of their running and queued job load
   */
  public static List<String> getRemoteHosts(ReceiptService receiptService, String receiptType) {
    Map<String, Long> runningComposerJobs = receiptService.getHostsCount(receiptType, new Status[] { Status.QUEUED,
            Status.RUNNING });
    List<String> hosts = receiptService.getHosts(receiptType);
    TreeBidiMap bidiMap = new TreeBidiMap(runningComposerJobs);

    LinkedList<String> sortedRemoteHosts = new LinkedList<String>();
    MapIterator iter = bidiMap.inverseOrderedBidiMap().orderedMapIterator();
    while (iter.hasNext()) {
      iter.next();
      sortedRemoteHosts.add((String) iter.getValue());
    }
    // If some of the hosts have no jobs, they are not in the list yet. Add them at the front of the list.
    for (String host : hosts) {
      if (!sortedRemoteHosts.contains(host)) {
        sortedRemoteHosts.add(0, host);
      }
    }
    return sortedRemoteHosts;
  }

}
