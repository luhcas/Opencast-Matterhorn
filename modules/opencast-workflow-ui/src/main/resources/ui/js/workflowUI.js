var workflowUI = workflowUI || {};

workflowUI.autoUpdateInterval = false;
workflowUI.autoUpdateDelay = 2000;
workflowUI.currentState = workflowControler.STATE.ALL;

workflowUI.init = function() {

    $('.workflowListItem-header').click(function(){
            $(this).parent().children('.workflowListItem-content').toggle('fast');
            return false;
    });

}

workflowUI.toggleAutoUpdate = function(active) {
    if (active) {
        window.clearInterval(workflowUI.autoUpdateInterval);
        workflowUI.autoUpdateInterval = false;
    } else {
        workflowUI.autoUpdateInterval = window.setInterval("workflowUI.updateList();", worklfowUI.autoUpdateDelay);
    }
}

workflowUI.updateList = function() {

}

workflowUI.generateWorkflowListItem = function(wfInstance) {

}