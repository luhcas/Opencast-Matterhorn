/**
 *  Copyright 2009 The Regents of the University of California
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

package org.opencastproject.deliver.actions;

import org.opencastproject.deliver.schedule.FailedException;
import org.opencastproject.deliver.schedule.Task;

/**
 * Simple RemoveAction which always succeeds. This is used in the servlet's
 * test mode.
 */

public class RemoveDefault extends RemoveAction {

    @Override
    protected void execute() {
        Task publish_task = this.getTaskNamed(this.getPublishTask());
        if (publish_task == null)
            throw new FailedException("Not found: " + this.getPublishTask());
        status("Publication removed");
        succeed("Publication removed");
    }
}
