
var workflowController = workflowController || {};

workflowController.rest_base = '../rest';

workflowController.selected_state = 'all';

workflowController.getInstances = function(wstate, callback) {
    log('Controller.getInstance: GET ' + workflowController.rest_base + "/instances.json");
    var opts;
    if (wstate != 'all') {
        opts = {state:wstate};
    }
    $.getJSON( workflowController.rest_base + "/instances.json", opts,
        function(data) {
            var out = {};
            if (!data) {
                log('Controller.getInstances: response provides not data');
            } else {
                for (key in data) {
                    out[data[key]['workflow_id']] = data[key];
                    log('loaded ' + data[key]['workflow_id']);
                }
            }
            callback(out);
        }
    );
};

workflowController.getInstance = function(id, callback) {
    var url =  workflowController.rest_base + "/instance/"+id+".json";
    log('Controller.getInstance: GET ' + url);
    $.get(url, function(data) {
        log('parsing JSON..');
        //data = data.replace(/\n/g, "");
        var obj = $.json.decode(data);
        
        callback(obj);
    });
};
