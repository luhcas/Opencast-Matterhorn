var Opencast = Opencast || {};

Opencast.WorkflowInspect = (function() {

  this.WORKFLOW_INSTANCE_URL = '../workflow/rest/instance/';

  var $container;    // id of the target container
  var templateId;
  var instanceView;       // view of the workflow instance data

  /** Called by the site to obtain and render instance data to a specified container.
   *
   */
  this.renderWorkflow = function(id, container, template) {
    templateId = template;
    $container = $('#' + container);
    $.ajax({
      url : this.WORKFLOW_INSTANCE_URL + id + ".json?jsonp=?",
      dataType: 'jsonp',
      jsonp: 'jsonp',
      success: Opencast.WorkflowInspect.rx
    });
  }

  /** JSONP recieve function
   *
   */
  this.rx = function(data) {
    instanceView = buildInstanceView(data.workflow);
    render(instanceView, $container);
  }

  /** Build view of workflow instance data
   *
   */
  function buildInstanceView(workflow) {
    var out = Opencast.RenderUtils.extractScalars(workflow);
    out.config = buildConfigObject(workflow.configurations.configuration);

    var ops = Opencast.RenderUtils.ensureArray(workflow.operations.operation);
    $.each(ops, function(index, op) {
      if (op.configurations !== undefined && op.configurations.configuration !== undefined) {
        op.configurations = buildConfigObject(op.configurations.configuration);
      } else {
        op.configurations = [];
      }
    });
    out.operations = ops;

    var mp = workflow.mediapackage;
    mp.attachments = Opencast.RenderUtils.ensureArray(mp.attachments.attachment);
    mp.media.track = Opencast.RenderUtils.ensureArray(mp.media.track);
    mp.metadata.catalog = Opencast.RenderUtils.ensureArray(mp.metadata.catalog);
    out.mediapackage = mp;

    return {workflow : out};
  }

  /** Render workflow view to specified container
   *
   */
  function render(workflow, $target) {
    var result = TrimPath.processDOMTemplate(templateId, workflow);
    $target.append(result);
    $target.tabs();
    $('.unfoldable-tr').click(function() {
      var $content = $(this).find('.unfoldable-content');
      var unfolded = $content.is(':visible');
      $('.unfoldable-content').hide('fast');
      if (!unfolded) {
        $content.show('fast');
      }
    });
  }

  /** Build an object that can be rendered easily from the Configuration objects
   *  of Workflow, Operation etc. If the same key is encountered twice ore more
   *  the field is converted to an array.
   */
  function buildConfigObject(data) {
    var out = {};
    data = Opencast.RenderUtils.ensureArray(data);
    $.each(data, function(index, member) {
      if ($.isArray(out[member.key])) {
        out[member.key].push(member.value);
      } else if (out[member.key] !== undefined) {
        out[member.key] = [out[member.key], member.value];
      } else {
        out[member.key] = member['$'];
      }
    });
    return out;
  }

  return this;
}());

Opencast.RenderUtils = (function() {

  /** Returns
   *    [obj] if an object is passed
   *    obj if obj is already an Array
   *    [] if obj === undefined or something eles goes wrong
   */
  this.ensureArray = function(obj) {
    try {
      if ($.isArray(obj)) {
        return obj;
      } else {
        return [obj];
      }
    } catch (e) {
      return [];
    }
  }

  /** Returns either the value of obj if obj is a scalar or a ',' separated list
   *  of the scalar values of obj if obj is an Array
   */
  this.ensureString = function(obj) {
    if ($.isArray(obj)) {
      return obj.join(', ');
    } else {
      return '' + obj;
    }
  }

  /** Returns an object containing all scalar members of obj.
   *
   */
  this.extractScalars = function(obj) {
    var out = {};
    for (var key in obj) {
      var value = obj[key];
      if (typeof value == 'string' || typeof value == 'number') {
        out[key] = obj[key];
      }
    }
    return out;
  }

  return this;
}());