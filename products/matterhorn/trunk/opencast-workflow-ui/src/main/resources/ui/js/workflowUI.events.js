var workflowUIevent = workflowUIevent || {};

workflowUIevent.stateSelected = function(state, elm) {
    workflowUI.currentState = state;
    if (!workflowUI.autoUpdateInterval) {
        workflowUI.updateList();
    }
    $(".selectButton-active").removeClass("selectButton-active");
    $(elm).addClass("selectButton-active");
}

workflowUIevent.toggleAutoUpdate = function(active) {
    workflowUI.toggleAutoUpdate(active);
}

workflowUIevent.startWorkflow = function() {
}

workflowUIevent.stopWorkflow = function() {
}

workflowUIevent.pauseWorkflow = function() {
}

workflowUIevent.resumeWorkflow = function() {
}

