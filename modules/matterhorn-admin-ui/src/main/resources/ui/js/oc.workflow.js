var ocWorkflow = {};

ocWorkflow.init = function(selectElm, configContainer) {
  ocWorkflow.container = configContainer;
  ocWorkflow.selector = selectElm;
  $(ocWorkflow.selector).click( function() {
    ocWorkflow.definitionSelected($(this).val(), configContainer);
  });
  ocWorkflow.loadDefinitions(selectElm, configContainer);
}

ocWorkflow.loadDefinitions = function(selector, container) {
  $.ajax({
    async: false,
    method: 'GET',
    url: '../workflow/rest/definitions.json',
    dataType: 'json',
    success: function(data) {
      for (i in data.workflow_definitions) {
        if (data.workflow_definitions[i].id != 'error') {
          var option = document.createElement("option");
          option.setAttribute("value", data.workflow_definitions[i].id);
          option.innerHTML = data.workflow_definitions[i].title;
          if (data.workflow_definitions[i].id == "full") {
            option.setAttribute("selected", "true");
          }
          $(selector).append(option);
        }
      }
      ocWorkflow.definitionSelected($(selector).val(), container);
    }
  });
}

ocWorkflow.definitionSelected = function(defId, container, callback) {
  $(container).load(
    '../workflow/rest/configurationPanel?definitionId=' + defId,
    function() {
      $('.holdCheckbox').attr('checked', false);
      $(container).show('fast');
      if (callback) {
        callback();
      }
    }
  );
}

ocWorkflow.getConfiguration = function(container) {
  var out = new Object();
  $(container).find('.configField').each( function(idx, elm) {
    if ($(elm).is('[type=checkbox]')) {
      if ($(elm).is(':checked')) {
        out[$(elm).attr('id')] = $(elm).val();
      }
    } else {
      out[$(elm).attr('id')] = $(elm).val();
    }
  });
  return out;
}

