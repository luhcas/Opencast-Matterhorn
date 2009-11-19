var eventFields = null;

function initPage(){
  $('.additionalMeta').toggle();
  $('#stage').load('schedulerform.html', initSchedulerForm);
}

function initSchedulerForm(){
  $("#startTime").datepicker();
  $('.required > th').prepend('<font color="red">*</font>');
}

function SchedulerEvent(eventFields){
  this.fields = eventFields || new Array();
  this.rootNS = 'http://scheduler.opencastproject.org';
  this.rootEl = 'scheduler-event';
  this.doc    = null;
  
  if(this.doc == null){
    if(document.implementation && document.implementation.createDocument){
      this.doc = document.implementation.createDocument(this.rootNS, this.rootEl, null);
    }else{
      this.doc = new ActiveXObject('MSXML2.DOMDocument');
      this.doc.loadXML('<' + this.rootEl + ' xmlns="' + this.rootNS + '"></' + this.rootEl + '>');
    }
  }
  
  this.serialize = function() {
    for(var i in this.fields){
      el = this.doc.createElement(i);
      el.appendChild(this.doc.createTextNode(this.fields[i].getValue()));
      this.doc.documentElement.appendChild(el);
    }
    
    if(typeof XMLSerializer != 'undefined'){
      return (new XMLSerializer()).serializeToString(this.doc);
    }else if(this.doc.xml){ return this.doc.xml; }
    else{ throw "Unable to serialize SchedulerEvent."; }
  }
}

function doSubmit(){
  var o = new SchedulerEvent(eventFields);
  var eventXML = null;
  try{
    var eventXML = o.serialize();
    console.log(eventXML);
  }catch(e){
    alert(e);
  }
  if(eventXML){
    $.post('http://localhost:8080/scheduler/rest/addEvent', {event: eventXML});
  }
}

function EventField(id, required, callback){
  if(id != "" && $('#' + id)[0]){
    this.id = id;
    this.formElement = $('#' + id);
  }else{
    throw "Unable to find field " + id;
  }
  this.required = required || false;
  this.getValue = callback || function() {
    if(this.required && this.formElement.val() == ""){
      throw "Must fill in all required Fields!";
    }
    return this.formElement.val();
  }
}

function checkboxFieldHandler(){
  if(this.formElement[0].checked){
    return this.formElement.val();
  }
  return "";
}

function EventFieldGroup(idArray, required, callback){
  this.groupElements = new Array();
  if($.isArray(idArray)){
    for(var i in idArray){
      if(!$('#' + idArray)[0]){
        throw "Unable to find field " + idArray[i];
      }
      this.groupElements.push($('#' + idArray[i]));
    }
  }else{
    throw "EventFieldGroup idArray must not be empty.";
  }
  this.required = required || false;
  this.getValue = callback || function() {
    values = new Array();
    for(var el in this.groupElements){
      if(this.groupElements[el][0].checked){
        values.push(this.groupElements[el].val());
      }
    }
    if(this.required && values.length == 0){
      throw "No item in EventFieldGroup selected, but one selection is required.";
    }
    return values.toString();
  }
}
