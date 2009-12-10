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
    $.post( SCHEDULER_URL + '/addEvent', {event: eventXML}, schedulerUI.eventSubmitComplete, 'xml' );
  }
  return true;
};

schedulerUI.eventSubmitComplete = function(data) {
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


}
