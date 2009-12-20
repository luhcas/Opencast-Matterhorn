/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

//setup schedulerUI "namespace"
var schedulerUI = schedulerUI || { };

/**
 *  clearForm resets all the form values to defaults.
 */
schedulerUI.clearForm = function() {
  $.each($('form'), function() { this.reset(); });
  //change duration.
  return true;
};

/**
 *  submitForm sends the scheduler form to the appropriate rest point based on
 *  URL param "edit". updating an event requires an EventID. calls
 *  eventSubmitComplete when finished.
 */
schedulerUI.submitForm = function() {
  var eventXML = null;
  try{
    var eventXML = eventsManager.serialize();
  }catch(e){
    console.log(e);
  }
  if(eventXML){
    var method  = '/addEvent';
    if(schedulerUI.getURLParams('edit')){
      method = '/updateEvent';
    }
    $.post( SCHEDULER_URL + method, {event: eventXML}, schedulerUI.eventSubmitComplete );
  }
  return true;
};

/**
 * if the submission is successful, this function displays the complete screen.
 */
schedulerUI.eventSubmitComplete = function(data) {
  $('#stage').load('schedulerform_complete.html', schedulerUI.loadCompleteValues);
};

/**
 *  loadCompleteValues fills in the completed fields with form data.
 *
 */
schedulerUI.loadCompleteValues = function(){
  for(field in eventsManager.fields){
    if(eventsManager.fields[field].getValue() != ""){
      if(field == "startdate" || field == "enddate"){
        $('#data-' + field + ' .data-value').empty().append(eventsManager.fields[field].getValue().toString());
        $('#data-' + field).toggle();
      } else {
        $('#data-' + field + ' .data-value').empty().append(eventsManager.fields[field].getValue());
        $('#data-' + field).toggle();
      }
    }
  }   
}

schedulerUI.showNotificationBox = function() {
  $('#');
}

/**
 *  loadKnownAgents calls the capture-admin service to get a list of known agents.
 *  Calls handleAgentList to populate the dropdown.
 */
schedulerUI.loadKnownAgents = function() {
  $.get(CAPTURE_ADMIN_URL + "/GetKnownAgents", handleAgentList, 'xml');
}

/**
 *  Popluates dropdown with known agents
 *
 *  @param {XML Document}
 */
function handleAgentList(data){
  $.each($("name", data),
         function(i, agent){
           $("#attendees").append($("<option></option>").val($(agent).text()).html($(agent).text())); 
         });
}

/**
 *  Function parses the URL for parameters.
 *  @param {String} Optional. If a name is passed, that parameter's value is returned.
 *  @return {String|Boolean|Array} If optional parameter is left empty, an array of all params are returned.
 */
schedulerUI.getURLParams = function(param){
  var urlParams = {};
  if(document.location.search){
    params = document.location.search.substr(1).split('&');
    for(var p in params){
      eq = params[p].indexOf("=");
      if(eq != -1){
        urlParams[params[p].substr(0, eq)] = params[p].substr(eq+1);
      }else{
        urlParams[params[p]] = true;
      }
    }
  }
  
  if(param && urlParams[param]){
    return urlParams[param];
  }else if(urlParams.length > 0){
    return urlParams;
  }
  return null;
}

/**
 *  loadEvent files out the event form fields from an exist event.
 *  @param {XML Document} Returned form /scheduler/rest/getEvent.
 */
schedulerUI.loadEvent = function(doc){
  eventsManager.populateForm(doc);
}

schedulerUI.toggleDetails = function(elSwitch, el){
  if(el.hasClass("detail-hide")){
    el.removeClass("detail-hide");
    el.addClass("detail-show");
    elSwitch.style.verticalAlign = "bottom";
    $(elSwitch).text("[less]");
  }else{
    el.removeClass("detail-show");
    el.addClass("detail-hide");
    elSwitch.style.verticalAlign = "top";
    $(elSwitch).text("[more]");
  }
}

schedulerUI.toggleDetails = function(elSwitch, el){
  if(el.hasClass("detail-hide")){
    el.removeClass("detail-hide");
    el.addClass("detail-show");
    elSwitch.style.verticalAlign = "bottom";
    $(elSwitch).text("[less]");
  }else{
    el.removeClass("detail-show");
    el.addClass("detail-hide");
    elSwitch.style.verticalAlign = "top";
    $(elSwitch).text("[more]");
  }
}
