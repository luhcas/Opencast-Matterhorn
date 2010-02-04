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

// Configurable Page Variables
var BASE_URL          = '';
var SCHEDULER_URL     = BASE_URL + '/scheduler/rest';
var WORKFLOW_URL      = BASE_URL + '/workflow/rest';
var CAPTURE_ADMIN_URL = BASE_URL + '/capture-admin/rest';

function initPage() {
  var d = new Date();
  d.setHours(d.getHours() + 1); //increment an hour.
  $('#startTimeHour').val(d.getHours());
  $('#startDate').datepicker({showOn: 'both', buttonImage: 'img/calendar.gif', buttonImageOnly: true});
  $('#startDate').datepicker('setDate', d);
  
  $('.required > label').prepend('<span style="color: red;">*</span>');
  
  $('.folder-head').click(function(){
                          $(this).children('.fl-icon').toggleClass('icon-arrow-right');
                          $(this).children('.fl-icon').toggleClass('icon-arrow-down');
                          $(this).next().toggle('fast');
                          return false;
                          });
  

  
  /**
   *  eventFields is an array of EventFields and EventFieldGroups, keyed on <item>'s key attribute described in scheduler
   *  service contract for the addEvent rest endpoint.
   *  @see https://wiki.opencastproject.org/confluence/display/open/Scheduler+Service
   */
  
  var eventFields = {
    "id":           new EventField('eventID'),
    "title":        new EventField('title', true),
    "creator":      new EventField('creator'),
    "contributor":  new EventField('contributor'),
    "series-id":    new EventField('series'),
    "subject":      new EventField('subject'),
    "language":     new EventField('language'),
    "abstract":     new EventField('description'),
    "channel-id":   new EventFieldGroup(new Array('distMatterhornMM'), true),
    "license":      new EventField('license'),
    "startdate":    new EventFieldGroup(new Array('startDate', 'startTimeHour', 'startTimeMin'), true, getStartDate, setStartDate, checkStartDate),
    "duration":     new EventFieldGroup(new Array('durationHour', 'durationMin'), true, getDuration, setDuration, checkDuration), //returns a date incremented by duration.
    "resources":    new EventFieldGroup(new Array('audio','audioVideo','audioScreen', 'audioVideoScreen'), true, getInputs, setInputs, checkInputs),
    "attendees":    new EventField('attendees', true, getAgent, setAgent, checkAgent),
    "device":       new EventField('attendees')
  };
  
  //Form Manager, handles saving, loading, de/serialization, and validation
  
  eventsManager = new EventManager(eventFields);
  
  $('.icon-help').click(
                        function(event) {
                        popupHelp.displayHelp($(this), event);
                        return false;
                        }
                        );
  
  $('body').click(
                  function() {
                  if ($('#helpbox').css('display') != 'none') {
                  popupHelp.resetHelp();
                  }
                  return true;
                  }
                  );
  
  $('#submitButton').click(schedulerUI.submitForm);
  $('#resetButton').click(schedulerUI.cancelForm);
  
  schedulerUI.loadKnownAgents();
  
  var eventID = schedulerUI.getURLParams('eventID');
  if(eventID && schedulerUI.getURLParams('edit')){
    $("#page-title").text("Edit Recording");
    $('#attendees').change(function(){ 
                           $("#notice-container").hide();
                           $.get(CAPTURE_ADMIN_URL + '/agents/' + $('#attendees option:selected').val(), checkAgentStatus);
                           });
  }
}

$(document).ready(initPage);

/**
 *  EventManager Object
 *  Handles form validation, serialization, and form value population
 *  @class
 */
function EventManager(eventFields){
  //Contains an array of eventFields
  this.fields         = eventFields || new Array();
  //Defines the root namespace for scheduler service
  this.rootNS         = 'http://scheduler.opencastproject.org';
  //Defines the root element of xml document used for scheudler service
  this.rootEl         = 'scheduler-event';
}

/**
 *  Create new rest document
 *  @return {DOM Object} Empty DOM document
 */
function createDoc(){
  var doc = null;
  //Create a DOM Document, methods vary between browsers, e.g. IE and Firefox
  if(document.implementation && document.implementation.createDocument){ //Firefox, Opera, Safari, Chrome, etc.
    doc = document.implementation.createDocument(this.rootNS, this.rootEl, null);
  }else{ // IE
    doc = new ActiveXObject('MSXML2.DOMDocument');
    doc.loadXML('<' + this.rootEl + ' xmlns="' + this.rootNS + '"></' + this.rootEl + '>');
  }
  return doc;
}
//Set EventManager createDoc function
EventManager.prototype.createDoc = createDoc;

/** 
 *  Validate the event form and display the notifcations box
 *  @returns {boolean} True if the form value's are valid, otherwise false;
 */
function checkForm() {
  //Todo: create some functions in schedulerUI to manipulate the notification box.
  var missingFields = new Array();
  for(var i in this.fields){
    //if the field is required, and does not contain valid data notify user
    if(this.fields[i].required && !this.fields[i].checkValue()){
      missingFields.push(i);
    }
  }
  if(missingFields.length > 0){
    $('#missingFields-container li').hide();
    
    $('#missingFields-container').show('fast');
    for(var f in missingFields){
      $('#missing-' + missingFields[f]).show();
      $('#label-' + missingFields[f]).css('color', 'red');
    }
    return false;
  }else{
    $('#missingFields-container').hide('fast');
    return true;
  }
}
//Set EventManager checkForm function
EventManager.prototype.checkForm = checkForm;

/** 
 *  Serialize the form into an XML file for consumption by the
 *  scheduler service: addEvent
 */
function serialize() {
  //Todo: EventField/Group should know how to render themselves. this is bad, refactor
  if(this.checkForm()){
    var doc = this.createDoc();
    var metadata = doc.createElement('metadata');
    for(var i in this.fields){
      if(this.fields[i].getValue()){
        el = doc.createElement(i);
        if(i == "startdate"){
          el.appendChild(doc.createTextNode(this.fields[i].getValue().getTime()));
          doc.documentElement.appendChild(el);
        }else if(i == "duration"){
          el.appendChild(doc.createTextNode(this.fields[i].getValue()));
          doc.documentElement.appendChild(el);
        }else if(i == "attendees"){
          var attendee = doc.createElement("attendee");
          attendee.appendChild(doc.createTextNode(this.fields[i].getValue()));
          el.appendChild(attendee);
          doc.documentElement.appendChild(el);
        }else if(i == "id"){
          el.appendChild(doc.createTextNode(this.fields[i].getValue()));
          doc.documentElement.appendChild(el);
        }else if(i == "resources"){
          var resource = doc.createElement("resource");
          resource.appendChild(doc.createTextNode(this.fields[i].getValue()));
          el.appendChild(resource);
          doc.documentElement.appendChild(el);
        }else{
          var item = doc.createElement('item');
          var val = doc.createElement('value');
          val.appendChild(doc.createTextNode(this.fields[i].getValue()));
          item.setAttribute('key', i);
          item.appendChild(val);
          metadata.appendChild(item);
        }
      }
    }
    doc.documentElement.appendChild(metadata);
    if(typeof XMLSerializer != 'undefined'){
      return (new XMLSerializer()).serializeToString(doc);
    }else if(doc.xml){ return doc.xml; }
    else{ throw "Unable to serialize SchedulerEvent."; }
  }else{
    //error handle
  }
}
//Set EventManager serialize function
EventManager.prototype.serialize = serialize;

/**
 *  Populate form with values from an existing event.
 *  @param {DOM Object}
 *  
 *  This approach needs more work. Probably need custom deserializing and
 *  serilizing functions for each EventField/Group.
 */
function populateForm(document){
  for(var e in this.fields){
    if(e != "channel-id"){
      switch(e){
        case 'attendees':
          this.fields[e].setValue($("attendee", document).text());
          break;
        case 'startdate':
          this.fields[e].setValue(new Date(parseInt($("startdate", document).text())));
          break;
        case 'duration': //we have to know the start date before we can use the enddate to sort out duration. This is dumb, talk to rudiger about switching to just use duration.
          this.fields[e].setValue(parseInt($("duration", document).text()));
          break;
        case 'id':
          this.fields[e].setValue($("id", document).text());
          break;
        case 'resources':
          this.fields[e].setValue($("resource", document).text());
          break;
        default:
          this.fields[e].setValue($("item[key='" + e + "'] > value", document).text());
      }
    }
  }
  $('#attendees').change();
}
//Set EventManager populate form
EventManager.prototype.populateForm = populateForm;

/**
 *  EventField Object
 *  Encapsulates form fields, provides validation, get/set value,
 *  and indicating if the field is required or optional.
 *  @class
 */
function EventField(id, required, getValue, setValue, checkValue){
  if(id != "" && $('#' + id)[0]){
    this.id = id;
    this.formElement = $('#' + id);
  }else{
    throw "Unable to find field " + id;
  }
  this.required   = required || false;
  this.value      = null;

  // If getValue is specified, override the default getValue.
  if($.isFunction(getValue)){
    this.getValue = getValue;
  }
  
  // If setValue is specified, override the default setValue.
  if($.isFunction(setValue)){
    this.setValue = setValue;
  }
  
  // If checkValue is specified, override the default checkValue.
  if($.isFunction(checkValue)){
    this.checkValue = checkValue;
  }
}

/**
 *  get the value of an event form field
 *  @return {string} 
 */
function getEventFieldValue() {
  if(this.formElement){
    this.value = this.formElement.val()
  }
  return this.value;
}
//Set EventField getValue function
EventField.prototype.getValue = getEventFieldValue;

/**
 *  set the value of an event form field
 *  @param {string}
 */
function setEventFieldValue(val) {
  this.value = val;
  this.formElement.val(val);
}
//Set EventField setValue function
EventField.prototype.setValue = setEventFieldValue;

/**
 *  validate the value of an event form field
 *  @return {boolean} True if the field is valid, otherwise false.
 */
function checkEventFieldValue() {
  if(this.getValue()){
    return true;
  }
  return false;
}
//Set EventField checkValue function
EventField.prototype.checkValue = checkEventFieldValue;


/**
 *  Overrides the default getValue function of EventField for checkboxes
 *  @return {string} Value of a checkbox that is checked.
 */
function getCheckboxValue() {
  if(this.formElement[0].checked){
    return this.formElement.val();
  }
  return "";
}

/**
 * Overrides getValue for EventField for agent field
 * @return {String} agent id
 */
function getAgent() {
  if(this.formElement){
    this.value = this.formElement.val()
  }
  return this.value;
}

/**
 * Overrides setValue for EventField for agent field
 * @param {String} agent id
 */
function setAgent(agentId) {
  var opts = this.formElement.children();
  if(opts.length > 0){
    var found = false;
    for(var i = 0; i < opts.length; i++){
      if(opts[i].value == agentId){
        found = true;
        break;
      }
    }
    if(!found){ //Couldn't find the previsouly selected agent, add to list and notifiy user.
      this.formElement.append($("<option>" + agentId + "</option>").val(agentId));
      $('#attendees').change();
    }
    this.formElement.val(agentId);
  }
}

/**
 * Overrides getValue for EventField for agent field
 * @return {Boolean} true if an agent is selected
 */
function checkAgent() {
  if(this.getValue()){
    return true;
  }
  return false;
}

/**
 *  EventFieldGroup Object
 *  Container for a group of EventFields that need to be evaluated
 *  together.
 *  @class
 */
function EventFieldGroup(idArray, required, getValue, setValue, checkValue) {
  this.groupElements = new Array();
  if($.isArray(idArray)){
    for(var i in idArray){
      if(!$('#' + idArray)[0]){
        throw "Unable to find field " + idArray[i];
      }
      this.groupElements[idArray[i]] = $('#' + idArray[i]);
    }
  }else{
    throw "EventFieldGroup idArray must not be empty.";
  }
  
  // Is this field required for form validation?
  this.required = required || false;
  
  // If getValue is specified, override the default getValue.
  if($.isFunction(getValue)){
    this.getValue = getValue;
  }
  
  // If setValue is specified, override the default setValue.
  if($.isFunction(setValue)){
    this.setValue = setValue;
  }
  
  // If checkValue is specified, override the default checkValue.
  if($.isFunction(checkValue)){
    this.checkValue = checkValue;
  }
}

/**
 *  get an string of each field's value.
 *  @return {string} a string of values, seperated by ','
 */
function getEventFieldGroupValue() {
  
  values = new Array();
  for(var el in this.groupElements){
    if(this.groupElements[el][0].checked){
      values.push(this.groupElements[el].val());
    }
  }
  if(values.length > 0){
    this.value = values.toString();
  }
  return this.value;
}
//Set EventFieldGroup getValue function
EventFieldGroup.prototype.getValue = getEventFieldGroupValue;

/**
 *  set the values of each field in the group
 *  @param {Object} Object of key/value pairs, where the key is the id of an EventFieldGroup groupElement
 */
function setEventFieldGroupValue(values) {
  for(var el in values){
    if(this.groupElements[el]){
      this.groupElements[el][0].checked = values[el];
    }
  }
  
}//Set EventFieldGroup setValue function
EventFieldGroup.prototype.setValue = setEventFieldGroupValue;

/**
 *  validate an EventFieldGroup
 *  @return {boolean} True if EventFieldGroup valid, otherwise false
 */
function checkEventFieldGroupValue() {
  oneIsChecked = false;
  for(var el in this.groupElements){
    oneIsChecked = this.groupElements[el][0].checked || oneIsChecked;
  }
  if(this.required && oneIsChecked){
    return true;
  }
  return false;
}
//Set EventFieldGroup checkValue function
EventFieldGroup.prototype.checkValue = checkEventFieldGroupValue;

// ===== Custom get/set/check functions for EventFieldGroup =====

/**
 *  Overrides getValue of EventFieldGroup for duration fields
 *  @return {Date Object} Date object, start date, incremented by duration.
 */
function getDuration() {
  if(this.checkValue()){
    duration = this.groupElements['durationHour'].val() * 3600; // seconds per hour
    duration += this.groupElements['durationMin'].val() * 60; // seconds per min
    this.value = (duration * 1000);
  }
  return this.value;
}

/**
 *  Overrides setValue for EventFieldGroup for duration fields
 *  @param {integer} Duration in milliseconds
 */
function setDuration(val) {
  if(this.groupElements['durationHour'] && this.groupElements['durationMin']){
    val = val/1000; //milliseconds -> seconds
    var hour  = Math.floor(val/3600);
    var min   = Math.floor((val/60) % 60);
    this.groupElements['durationHour'].val(hour);
    this.groupElements['durationMin'].val(min);
  }
}

/**
 *  Overrides checkValue for EventFieldGroup for duration fields
 *  @return {boolean} True if the duration is valid, otherwise false.
 */
function checkDuration(){
  if(this.groupElements['durationHour'] && this.groupElements['durationMin'] &&
     (this.groupElements['durationHour'].val() !== "0" || this.groupElements['durationMin'].val() !== "0")){
    return true;
  }
  return false;
}

/**
 *  Overrides getValue for EventFieldGroup for startDate fields
 *  @return {Date Object}
 */
function getStartDate(){
  var date = 0;
  if(this.checkValue()){
    date = this.groupElements['startDate'].datepicker('getDate').getTime() / 1000; // Get date in milliseconds, convert to seconds.
    date += this.groupElements['startTimeHour'].val() * 3600; // convert hour to seconds, add to date.
    date += this.groupElements['startTimeMin'].val() * 60; //convert minutes to seconds, add to date.
    date = date * 1000; //back to milliseconds
    this.value = new Date(date);
  }
  
  return this.value; //TODO: set seconds/milliseconds to 0.
}

/**
 *  Overrides setValue for EventFieldGroup for startDate fields
 *  @param {Date Object}
 */
function setStartDate(date){
  if(this.groupElements['startDate'] && this.groupElements['startTimeHour'] && this.groupElements['startTimeMin']){
    var hour = date.getHours();
    
    this.groupElements['startTimeHour'].val(hour);
    this.groupElements['startTimeMin'].val(date.getMinutes());
    
    //datepicker modifies the date object removing the time.
    this.groupElements['startDate'].datepicker('setDate', date);
  }
}

/**
 *  Overrides checkValue for EventFieldGroup for startDate fields
 *  @return {boolean} True if the startDate is valid, otherwise false.
 */
function checkStartDate(){
  var date = this.groupElements['startDate'].datepicker('getDate');
  var now = new Date();
  if( this.groupElements['startDate'] &&
      date &&
      this.groupElements['startTimeHour'] &&
      this.groupElements['startTimeMin']){
    var startdatetime = new Date(date.getFullYear(), 
                             date.getMonth(), 
                             date.getDate(), 
                             this.groupElements['startTimeHour'].val(),
                             this.groupElements['startTimeMin'].val());
    if(startdatetime.getTime() >= now.getTime()){
      return true;
    }
    return false;
  }
}

/**
 *  Overrides getValue for EventFieldGroup for input field
 *  @return {string} Comma seperated string of inputs
 */
function getInputs(){
  var selected = false;
  for(var el in this.groupElements){
    if($("#" + el)[0] && $("#" + el)[0].checked){
      selected = $("#" + el);
      break;
    }
  }
  if(selected){
    switch(selected.val()){
      case "1":
        this.value = "AUDIO";
        break;
      case "3":
        this.value = "AUDIO,VIDEO";
        break;
      case "5":
        this.value = "AUDIO,SCREEN";
        break;
      case "7":
        this.value = "AUDIO,VIDEO,SCREEN";
        break;
      default:
        throw "Unable to get selected input.";
    }
  }
  return this.value;
}

/**
 *  Overrides setValue for EventFieldGroup for input field
 *  @param {string} Comma seperated string of inputs
 */
function setInputs(inputs){
  //TODO: modify to be id independent.
  switch(inputs){
    case "AUDIO":
      $("#audio")[0].checked = true;
      break;
    case "AUDIO,VIDEO":
      $("#audioVideo")[0].checked = true;
      break;
    case "AUDIO,SCREEN":
      $("#audioScreen")[0].checked = true;
      break;
    case "AUDIO,VIDEO,SCREEN":
      $("#audioVideoScreen")[0].checked = true;
      break;
    default:
      throw "Unable to set selected input.";
  }
}

/**
 *  Overrides checkValue for EventFieldGroup for input field
 *  @return {boolean} True if one of the radios are checked
 */
function checkInputs(){
  var checked = false;
  for(var el in this.groupElements){
    if($("#" + el)[0].checked){
      checked = true;
      break;
    }
  }
  return checked;
}

function checkAgentStatus(doc){
  var state = $("state", doc).text();
  if(state == "unknown" || state == "offline"){
    $("#notice-container").show();
  }
}