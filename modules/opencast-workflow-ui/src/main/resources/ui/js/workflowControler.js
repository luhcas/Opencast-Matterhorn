/*
 *       ------- WorkflowControler -------
 */
var workflowControler = workflowControler || {};

// enum of states a workflow can be in
workflowControler.STATE = { INSTANTIATED : 'INSTANTIATED',
                            RUNNING      : 'RUNNING',
                            STOPPED      : 'STOPPED',
                            PAUSED       : 'PAUSED',
                            SUCCEEDED    : 'SUCCEEDED',
                            FAILED       : 'FAILED',
                            ALL          : 'ALL' };

workflowControler.getWorkflowDefinitions = function(callback) {
    $.ajax({
        type     : "GET",
        url      : "../../conductor/rest/definitions",
        dataType : "xml",
        success  : function(data, status) {
             out = Array();
             root = data.getElementsByTagName("ns2:workflow-definition-list")[0];
             if (!root) callback(out);
             defs  = root.getElementsByTagName("workflow-definition");
             if (!defs) callback(out);
             for (def=0;  def < defs.length; def++) {
                 out.push(new WorkflowDefinition(defs[def]));
             }
             callback(out);
        }
    });
}

workflowControler.getInstances = function(state, callback) {
    // yea, it's jq's low-level-implementation, I like low-level...
    $.ajax({
        type     : "GET",
        url      : "../rest/instances/" + state,
        dataType : "xml",
        success  : function(data, status) {
             root = data.getElementsByTagName("ns2:workflow-instances")[0];
             if (!root) callback(null);
             workflows  = root.getElementsByTagName("workflow-instance");
             out = new Array();
             if (workflows.length > 0) {
                 for (w=0; w < workflows.length; w++) {
                    out.push(new WorkflowInstance(workflows[w]));
                }
             }
             callback(out);
        }
    });
}

workflowControler.getInstance = function(ID, callback) {
    $.ajax({
        type     : "GET",
        url      : "../rest/instance/" + ID,
        dataType : "xml",
        success  : function(data, status) {
            root = data.getElementsByTagName("ns2:workflow-instance");  // because of the ns2: prefix everything has to be cloned
            newroot = data.createElement("workflow-instance");
            newroot.setAttribute("state", root.getAttribute("state"));
            newroot.setAttribute("id", root.getAttribute("id"));
            for (child in root.childNodes) {
                newchild = child.cloneNode(true);
                newroot.appendChild(newchild);
            }
            root.removeChild(root);
            callback(new WorkflowInstance(data));
        }
    });
}

workflowControler.start = function (definition, mediapackageID, properties) {

}

workflowControler.stop = function(ID) {
     $.ajax({
        type     : "GET",
        url      : "../rest/stop/" + ID,
        dataType : "xml",
        success  : function(data, status) {
            alert(status);
        }
     });
}

workflowControler.suspend = function(ID) {
    $.ajax({
        type     : "GET",
        url      : "../rest/suspend/" + ID,
        dataType : "xml",
        success  : function(data, status) {
            alert(status);
        }
     });
}

workflowControler.resume = function(ID) {
    $.ajax({
        type     : "GET",
        url      : "../rest/resume/" + state,
        dataType : "xml",
        success  : function(data, status) {
            alert(status);
        }
     });
}






