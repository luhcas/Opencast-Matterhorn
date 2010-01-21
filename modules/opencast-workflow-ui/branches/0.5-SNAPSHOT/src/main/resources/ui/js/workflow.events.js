
/* -------------- Workflow Events -------------- */

var workflowEvents = workflowEvents || {};

workflowEvents.updateInterval = false;

workflowEvents.stateSelected = function(state) {
    log('Events.stateSelected: state ' + state + ' was selected');
    workflowController.selected_state = state;
    workflowController.getInstances(state, workflowUI.updateInstanceList);
}

workflowEvents.autoUpdateClicked = function() {
    if (document.getElementById('auto-update-switch').checked) {
        log('Events.autoUpdateClicked: setting up auto update interval');
        workflowEvents.updateInterval = window.setInterval('workflowEvents.updateTriggered();', 3000);
    } else {
        log('Events.autoUpdateClicked: clearing auto update interval');
        window.clearInterval(workflowEvents.updateInterval);
    }
}

workflowEvents.updateTriggered = function() {
    log('Events.updateTriggered: fired');
    workflowController.getInstances(workflowController.selected_state, workflowUI.updateInstanceList);
    for (key in workflowUI.instancesDetailed) {
        workflowUI.updateWorkflowInstance(key);
    }
}
