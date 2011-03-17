/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.plugtree.training.jbpm.handler;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

/**
 *
 * @author esteban
 */
public class HumanTaskMockHandler implements WorkItemHandler {

    private WorkItemManager workItemManager;
    private Long workItemId;

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        this.workItemId = workItem.getId();
        this.workItemManager = manager;
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    }

    public void completeWorkItem() {
        this.workItemManager.completeWorkItem(workItemId, null);
    }

}
