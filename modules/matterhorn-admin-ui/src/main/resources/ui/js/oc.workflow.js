var ocWorkflow = {};

ocWorklfow.init = function(selectElm, configContainer) {
  ocWorkflow.container = configContainer;
  ocWorkflow.selector = selectElm;
  $(ocWorkflow.selector).click( function() {
    ocWorkflow.definitionSelected($(this).val(), configContainer);
  });
  ocWorkflow.loadDefinitions(ocWorkflow.selector);
}

ocWorkflow.loadDefinitions = function(selector) {

}

ocWorkflow.definitionSelected = function(defId, container) {

}




