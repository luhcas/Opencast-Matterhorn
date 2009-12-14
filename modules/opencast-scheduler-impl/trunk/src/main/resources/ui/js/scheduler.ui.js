var schedulerUI = schedulerUI || { };

schedulerUI.clearForm = function() {
  $.each($('form'), function() { this.reset(); });
  //change duration.
  return true;
};

schedulerUI.submitForm = function() {
  var eventXML = null;
  try{
    var eventXML = eventsManager.serialize();
    console.log(eventXML);
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

schedulerUI.eventSubmitComplete = function(data) {
  console.log("Event complete");
  $('#stage').load('schedulerform_complete.html', schedulerUI.loadCompleteValues);
};

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

schedulerUI.loadKnownAgents = function() {
  $.get(CAPTURE_ADMIN_URL + "/GetKnownAgents", handleAgentList, 'xml');
}

function handleAgentList(data){
  $.each($("name", data),
         function(i, agent){
           $("#attendees").append($("<option></option>").val($(agent).text()).html($(agent).text())); 
         });
}

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

schedulerUI.loadEvent = function(doc){
  eventsManager.populateForm(doc);
}
