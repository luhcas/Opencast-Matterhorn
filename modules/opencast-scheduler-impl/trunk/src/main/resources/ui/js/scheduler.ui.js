var schedulerUI = schedulerUI || { };

schedulerUI.clearForm = function() {
  $.each($('form'), function() { this.reset(); });
  //change duration.
};

schedulerUI.submitForm = function() {
  var eventXML = null;
  try{
    var eventXML = eventsManager.serialize();
    console.log(eventXML);
  }catch(e){
    alert(e);
  }
  if(eventXML){
    $.post( SERVICE_URL + '/addEvent', {event: eventXML}, schedulerUI.eventSubmitComplete, 'xml' );
  }
};

schedulerUI.eventSubmitComplete = function(data) {
  $('#stage').load('schedulerform_complete.html',
                   function(){
                     for(field in eventsManager.fields){
                       if(eventsManager.fields[field].value != ""){
                         $('#data-' + field + ' .data-value').empty().append(eventsManager.fields[field].value);
                         $('#data-' + field).toggle();
                       }
                     }
                   });
};

schedulerUI.showNotificationBox = function() {
  $('#');
}
