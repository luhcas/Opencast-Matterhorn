log = function(msg) {
    if (workflowUI.debug) {
        var elm = document.getElementById('debug_output');
        elm.value = msg + "\n" + elm.value;
    }
}

var workflowUI = workflowUI || {};

workflowUI.debug = false;

workflowUI.instances = {};
workflowUI.instancesDetailed = {};
workflowUI.selected_item = null;

workflowUI.init = function() {
    /* if debug is set to true inject the debug output into the page */
    if (workflowUI.debug) {
        $('body').append('<textarea id="debug_output"></textarea>');
        $('#debug_output').css( {'position' : 'absolute',
                                 'top' : '10px',
                                 'left': '900px',
                                 'width':'500px',
                                 'height':'300px',
                                 'float':'left'} );
    }
    log('UI.init: UI init done');
    workflowEvents.updateTriggered();
}

workflowUI.updateInstanceList = function(instances) {
    workflowUI.instances = instances;
    var list = $('#workflow-list');
    log('UI.updateInstaceList: updating instances');
    workflowUI.resetSelectedItem();
    list.empty();
    for (key in instances) {
        var instance = instances[key];
        list.append(workflowUI.constructListItem(instance));
    }
}

workflowUI.updateWorkflowInstance = function(id) {
    log('updating instance ' + id);
    workflowController.getInstance(id, function(data) {
        workflowUI.instancesDetailed[data.workflow_id] = data;

        var dialog = $('#dialog-' + data.workflow_id);

        // fill in data
        workflowUI.fillinBasicInfo(data, dialog.find('.workflow-basic-info'));
        workflowUI.fillinLogTable(data.workflow_operations, dialog.find('.log-table'));
    });
}

workflowUI.resetSelectedItem = function() {
    log('UI.resetSelectedItem: reseting controls');
    var elm = $('.workflow-list-item-focus');
    elm.removeClass('workflow-list-item-focus');
    elm.children('.workflow-item-inner-border').removeClass('workflow-item-inner-border-invisible');
    var controls = $('#workflow-controls');
    controls.addClass('ui-helper-hidden');
    controls.removeClass('workflow-item-bg-running');
    controls.removeClass('workflow-item-bg-paused');
    controls.removeClass('workflow-item-bg-stopped');
    controls.removeClass('workflow-item-bg-succeeded');
    controls.removeClass('workflow-item-bg-failing');
    controls.children('.workflow-control').addClass('ui-helper-hidden');
}

workflowUI.constructListItem = function(instance) {
    log("constructing item for " + instance.workflow_id);
    var item = $('.workflow-list-item-template').clone(true);
    item.removeClass('workflow-list-item-template');
    item.css('display','block');
    item.find('.icon').addClass('icon-' + instance.workflow_state);
    item.find('.workflow-item-wftitle').text(instance.workflow_title);
    item.find('.workflow-item-wfid').text(instance.workflow_id);
    item.find('.workflow-item-mptitle').text(instance.mediapackage_title);
    item.find('.workflow-item-operation').text(instance.workflow_current_operation);
    item.addClass('workflow-item-bg-' + instance.workflow_state);
    item.attr('id','item-' + instance.workflow_id);
    workflowUI.selected_item = instance.workflow_id;
    return item;
}

workflowUI.fillinBasicInfo = function(instance, element) {
    element.find('.icon').addClass('icon-' + instance.workflow_state);
    element.find('.workflow-item-wftitle').text(instance.workflow_title);
    element.find('.workflow-item-wfid').text(instance.workflow_id);
    element.find('.workflow-item-mptitle').text(instance.mediapackage_title);
    element.find('.workflow-item-operation').text(instance.workflow_current_operation);
    element.addClass('workflow-item-bg-' + instance.workflow_state);
}

workflowUI.fillinPropertiesTable = function(data, element) {
    log('constructing properties table');
    // get the template
    var template = $('#kv-table-data-row-template').clone(true);
    template.removeAttr('id');
    template.removeClass('ui-helper-hidden');

    element.empty();

    var item, kv_pair, key;
    for (i in data) {
        item = template.clone(true);
        kv_pair = data[i];

        // get the key from the pair
        for (key in kv_pair) {}

        item.children('.kv-table-data-key').append(key);
        item.children('.kv-table-data-value').append(kv_pair[key]);
        element.append(item);
    }
}

workflowUI.fillinLogTable = function(data, element) {
    log('constructing log');
    // get the template
    var template = $('#log-table-data-row-template').clone(true);
    template.removeAttr('id');
    template.removeClass('ui-helper-hidden');

    element.empty();

    var item, operation;
    for (key in data) {
        item = template.clone(true);
        operation = data[key];
        item.children('.log-table-data-operation').append(operation.name);
        item.children('.log-table-data-description').append(operation.description);
        item.children('.log-table-data-state').children('.icon').addClass('icon-' + operation.state);
        //log('adding operation: ' + operation.name + ' ' + operation.description + ' ' + operation.state);
        element.append(item);
    }
}

workflowUI.fillinMediaPackage = function(data, element) {
    log('constructing MediaPackage table');
    // get the template
    var template = $('#kv-table-data-row-template').clone(true);
    template.removeAttr('id');
    template.removeClass('ui-helper-hidden');

    element.empty();

    var item, key, value;
    for (key in data) {
        if (key.match(/mediapackage_/)) {
            item = template.clone(true);
            value = data[key];
            if (value == null ) value = 'null';
            key = key.substr(13);
            item.children('.kv-table-data-key').append(key);
            item.children('.kv-table-data-value').append(value);
            element.append(item);
        }
    }
}

workflowUI.displayWorkflowDialog = function(instance) {
    log('UI.displayWorkflowDialog: constructing detailed view for ' + instance.workflow_id);

    workflowController.getInstance(instance.workflow_id, function(data) {
        workflowUI.instancesDetailed[data.workflow_id] = data;
    
        var dialog = $('#workflow-dialog-template').clone(true);
        dialog.attr('id', 'dialog-' + instance.workflow_id);

        // set up the tabs
        var tabs = dialog.children('#tabs');
        tabs.attr('id', 'tabs-' + instance.workflow_id );

        // fill in data
        workflowUI.fillinBasicInfo(instance, dialog.find('.workflow-basic-info'));
        workflowUI.fillinLogTable(data.workflow_operations, dialog.find('.log-table'));
        workflowUI.fillinPropertiesTable(data.workflow_properties, dialog.find('.prop-table'));
        workflowUI.fillinMediaPackage(data, dialog.find('.mp-table'));

        dialog.removeClass('ui-helper-hidden');
        $('#workflow-list').append(dialog);
        dialog.dialog({width:640, 
                       resizable:false,
                       close: function(){
                           var id = $(this).attr('id').substr(7);
                           log('removing ' + id + ' from update list');
                           delete workflowUI.instancesDetailed[id];
                       }});
    });
}

