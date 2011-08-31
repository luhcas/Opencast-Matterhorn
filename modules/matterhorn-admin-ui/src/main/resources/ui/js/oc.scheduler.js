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
var ocScheduler = (function() {
  var sched = {};
  // REST endpoints
  var SCHEDULER_URL     = '/recordings';
  var WORKFLOW_URL      = '/workflow';
  var CAPTURE_ADMIN_URL = '/capture-admin';
  var SERIES_URL        = '/series';
  var RECORDINGS_URL    = '/admin/recordings.html';
  
  // Constants
  var CREATE_MODE       = 1;
  var EDIT_MODE         = 2;
  var SINGLE_EVENT      = 3;
  var MULTIPLE_EVENTS   = 4;
  
  sched.mode              = CREATE_MODE;
  sched.type              = SINGLE_EVENT;
  sched.selectedInputs    = '';
  sched.conflictingEvents = false;
  sched.tzDiff            = 0;
  
  // Catalogs
  sched.dublinCore = new ocAdmin.Catalog({ //DC Metadata catalog
    name: 'dublincore',
    serializer: new ocAdmin.DublinCoreSerializer()
  });
  sched.recording = new ocAdmin.Catalog({ // Additional Recording properties
    name: 'event',
    serializer: new ocAdmin.Serializer()
  });
  sched.capture = new ocAdmin.Catalog({ //Workflow Properties
    name: 'agentparameters',
    serializer: new ocAdmin.Serializer()
  });

  sched.init = function init(){
    this.internationalize();
    this.registerCatalogs();
    this.registerEventHandlers();
      
    ocWorkflow.init($('#workflowSelector'), $('#workflowConfigContainer'));
    
    if(this.type === SINGLE_EVENT){
      this.agentList = '#agent';
      this.inputList = '#inputList';
      $('#singleRecording').click(); //Initiates Page event cycle
    }else{
      this.agentList = '#recurAgent';
      this.inputList = '#recurInputList';
      $('#multipleRecordings').click();
    }
  
    if(ocUtils.getURLParam('seriesId')){
      $('#series').val(ocUtils.getURLParam('seriesId'));
      $.get(SERIES_URL + '/series/' + ocUtils.getURLParam('seriesId'), function(doc){
        $.each($('metadata', doc), function(i, metadata){
          if($('key', metadata).text() === 'title'){
            $('#seriesSelect').val($('value',metadata).text());
            return true;
          }
        });
      });
    }
    
    //Editing setup
    var eventId = ocUtils.getURLParam('eventId');
    if(eventId && ocUtils.getURLParam('edit')){
      this.mode = EDIT_MODE;
      document.title = i18n.window.edit + " " + i18n.window.prefix;
      $('#i18n_page_title').text(i18n.page.title.edit);
      $('#eventId').val(eventId);
      this.recording.components.eventId = new ocAdmin.Component('eventId');
      $('#recordingType').hide();
      $('#agent').change(
        function() {
          $('#noticeContainer').hide();
          $.get(CAPTURE_ADMIN_URL + '/agents/' + $('#agent option:selected').val(), this.checkAgentStatus);
        });
    }
  };

  sched.internationalize = function internationalize() {
    //Do internationalization of text
    jQuery.i18n.properties({
      name:'scheduler',
      path:'i18n/'
    });
    ocUtils.internationalize(i18n, 'i18n');
    //Handle special cases like the window title.
    document.title = i18n.window.schedule + " " + i18n.window.prefix; 
    $('#i18n_page_title').text(i18n.page.title.sched);
  };

  sched.registerEventHandlers = function registrerEventHandlers(){
    var initializerDate;
    initializerDate = new Date();
    initializerDate.setHours(initializerDate.getHours() + 1); //increment an hour.
    initializerDate.setMinutes(0);
    
    //UI Functional elements
    $('#singleRecording').click(function(){
      sched.changeRecordingType(SINGLE_EVENT);
    });
    $('#multipleRecordings').click(function(){
      sched.changeRecordingType(MULTIPLE_EVENTS);
    });
    
    $('.oc-ui-collapsible-widget .form-box-head').click(
      function() {
        $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-e');
        $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-s');
        $(this).next().toggle();
        return false;
      });
    
    $('#seriesSelect').autocomplete({
      source: function(request, response) {
        $.ajax({
          url: SERIES_URL + '/series.json?q=' + request.term,
          dataType: 'json',
          type: 'GET',
          success: function(data) {
            var series_list = [];
            $.each(data, function(){
              series_list.push({value: this['http://purl.org/dc/terms/']['title'][0].value,
                                id: this['http://purl.org/dc/terms/']['identifier'][0].value});
            });
            series_list.sort(function stringComparison(a, b)	{
              a = a.value;
              a = a.toLowerCase();
              a = a.replace(/ä/g,"a");
              a = a.replace(/ö/g,"o");
              a = a.replace(/ü/g,"u");
              a = a.replace(/ß/g,"s");

              b = b.value;
              b = b.toLowerCase();
              b = b.replace(/ä/g,"a");
              b = b.replace(/ö/g,"o");
              b = b.replace(/ü/g,"u");
              b = b.replace(/ß/g,"s");

              return(a==b)?0:(a>b)?1:-1;
            });
            response(series_list);
          }, 
          error: function() {
            ocUtils.log('could not retrieve series_data');
          }
        });
      },
      select: function(event, ui){
        $('#series').val(ui.item.id);
      },
      search: function(){
        $('#series').val('');
      }
    });
    
    $('#seriesSelect').blur(function(){if($('#seriesSelect').val() === ''){ $('#series').val(''); }});
    
    $('#submitButton').button();
    $('#cancelButton').button();
    
    $('#submitButton').click(this.submitForm);
    $('#cancelButton').click(this.cancelForm);
  
    //single recording specific elements
    $('#startTimeHour').val(initializerDate.getHours());
    $('#startDate').datepicker({
      showOn: 'both',
      buttonImage: 'img/icons/calendar.gif',
      buttonImageOnly: true,
      dateFormat: 'yy-mm-dd'
    });
    $('#startDate').datepicker('setDate', initializerDate);
    $('#endDate').datepicker({
      showOn: 'both',
      buttonImage: 'img/icons/calendar.gif',
      buttonImageOnly: true,
      dateFormat: 'yy-mm-dd'
    });
    
    $('#agent').change(this.handleAgentChange);
    
    //multiple recording specific elements
    $('#recurStart').datepicker({
      showOn: 'both',
      buttonImage: 'img/icons/calendar.gif',
      buttonImageOnly: true,
      dateFormat: 'yy-mm-dd'
    });
    
    $('#recurEnd').datepicker({
      showOn: 'both',
      buttonImage: 'img/icons/calendar.gif',
      buttonImageOnly: true,
      dateFormat: 'yy-mm-dd'
    });
  
    $('#recurAgent').change(this.handleAgentChange);
    
    //Check for conflicting events.
    $('#startDate').change(this.checkForConflictingEvents);
    $('#startTimeHour').change(this.checkForConflictingEvents);
    $('#startTimeMin').change(this.checkForConflictingEvents);
    $('#durationHour').change(this.checkForConflictingEvents);
    $('#durationMin').change(this.checkForConflictingEvents);
    $('#agent').change(this.checkForConflictingEvents);
    
    $('#recurStart').change(this.checkForConflictingEvents);
    $('#recurEnd').change(this.checkForConflictingEvents);
    $('#recurStartTimeHour').change(this.checkForConflictingEvents);
    $('#recurStartTimeMin').change(this.checkForConflictingEvents);
    $('#recurDurationHour').change(this.checkForConflictingEvents);
    $('#recurDurationMin').change(this.checkForConflictingEvents);
    $('#recurAgent').change(this.checkForConflictingEvents);
    $('#daySelect :checkbox').change(this.checkForConflictingEvents);

    $('input#title,span.scheduler-instruction-text').hover(function(){
	$('li#titleNote span.scheduler-instruction-text').addClass('scheduler-instruction-text-hover');
    }, function(){
	$('li#titleNote span.scheduler-instruction-text').removeClass('scheduler-instruction-text-hover');
    });
  }

  sched.changeRecordingType = function changeRecordingType(recType){
    this.type = recType;
    
    this.registerCatalogs();
    
    $('.ui-state-error-text').removeClass('ui-state-error-text');
    $('#missingFieldsContainer').hide();
    
    var d = new Date()
    d.setHours(d.getHours() + 1); //increment an hour.
    d.setMinutes(0);
    
    if(this.type == SINGLE_EVENT){
      $('#titleNote').hide();  
      $('#recurringRecordingPanel').hide();
      $('#singleRecordingPanel').show();
      this.agentList = '#agent';
      this.inputList = '#inputList';
      $(this.inputList).empty();
      $('#seriesRequired').remove(); //Remove series required indicator.
      this.recording.components.startDate.setValue(d.getTime().toString());
    }else{
      // Multiple recordings have some differnt fields and different behaviors
      //show recurring_recording panel, hide single.
      $('#titleNote').show();
      $('#recurringRecordingPanel').show();
      $('#singleRecordingPanel').hide();
      this.agentList = '#recurAgent';
      this.inputList = '#recurInputList';
      $(this.inputList).empty();
      if(!$('#seriesRequired')[0]){
        $('#seriesContainer label').prepend('<span id="seriesRequired" class="scheduler-required-text">* </span>'); //series is required, indicate as such.
      }
      this.recording.components.recurrenceStart.setValue(d.getTime().toString());
    }
    this.loadKnownAgents();
  };

  sched.submitForm = function(){
    var payload = {};
    var error = false;
    
    if(ocScheduler.conflictingEvents) {
      $('#missingFieldsContainer').show();
      $('#errorConflict').show();
      $('#errorConflict li').show();
      return;
    }
    
    $.extend(true, sched.capture.components, ocScheduler.workflowComponents);
    
    payload[sched.dublinCore.name] = sched.dublinCore.serialize();
    payload[sched.recording.name] = sched.recording.serialize();
    payload[sched.capture.name] = sched.capture.serialize();
    
    for(var i in payload) {
      if(payload[i] === false) {
        error = true;
        //handle error
      }
    }
    
    if(!error) {
      /*$('#submitButton').attr('disabled', 'disabled');
      $('#submitModal').dialog({
        modal: true,
        resizable: false,
        draggable: false,
        close: function() { 
          document.location = RECORDINGS_URL;
        },
        create: function (event, ui)
        {
          $('.ui-dialog-titlebar-close').hide();
        }
      });*/
      if(ocUtils.getURLParam('edit')) {
        $.ajax({type: 'PUT',
                url: SCHEDULER_URL + '/' + $('#eventId').val(),
                data: payload,
                dataType: 'text',
                complete: ocScheduler.eventSubmitComplete
                });
      } else {
        ocUtils.log(payload);
        $.ajax({type: 'POST',
                url: SCHEDULER_URL + '/',
                data: payload,
                dataType: 'text',
                complete: ocScheduler.eventSubmitComplete
               });
      }
    }
    return true;
  };

  sched.cancelForm = function() {
    document.location = 'recordings.html';
  };

/*
ocScheduler.DeleteForm = function(){
  var title, series, creator;
  if(confirm(i18n.del.confirm)){
    $.ajax(SCHEDULER_URL + '/removeEvent/' + $('#eventId').val(), function(){
      title = ocScheduler.components.title.asString() || 'No Title';
      series = ocScheduler.components.seriesId.asString() || 'No Series';
      creator = ocScheduler.components.creator.asString() || 'No Creator';
      $('#i18n_del_msg').text(i18n.del.msg(title, series, '(' + creator + ')'));
      $('#stage').hide();
      $('#deleteBox').show();
    });
  }
};*/

  sched.handleAgentChange = function(elm){
    var time;
    var agent = elm.target.value;
    $(ocScheduler.inputList).empty();
    sched.recording.components.agentTimeZone = new ocAdmin.Component(['agentTimeZone'], {key: 'agentTimeZone'});
    if(agent){
      $.get('/capture-admin/agents/' + agent + '/configuration',
      function(doc){
        var devNames = [];
        var capabilities = [];
        $.each($('item', doc), function(a, i){
          var s = $(i).attr('key');
          if(s === 'capture.device.names'){
            devNames = $(i).text().split(',');
          } else if(s.indexOf('.src') != -1) {
            var name = s.split('.');
            capabilities.push(name[2]);
          } else if(s == 'capture.device.timezone.offset') {
            var agentTz = parseInt($(i).text());
            if(agentTz !== 'NaN'){
              sched.recording.components.agentTimeZone.setValue(agentTz);
              sched.handleAgentTZ(agentTz);
            }else{
              ocUtils.log("Couldn't parse TZ");
            }
          }
        });
        if(devNames.length > 0) {
          capabilities = devNames;
        }
        if(capabilities.length){
          sched.displayCapabilities(capabilities);
        }else{
          sched.tzDiff = 0; //No agent timezone could be found, assume local time.
          $('#inputList').html('Agent defaults will be used.');
          delete sched.recording.components.agentTimeZone;
        }
      });
    } else {
      // no valid agent, change time to local form what ever it was before.
      delete sched.recording.components.agentTimeZone; //Being empty will end up defaulting to the server's Timezone.
      if(sched.type === SINGLE_EVENT){
        time = sched.recording.components.startDate.getValue();
      }else if(sched.type === MULTIPLE_EVENTS){
        time = sched.recording.components.recurrenceStart.getValue();
      }
    };
  }

  sched.displayCapabilities = function(capabilities) {
    $(this.inputList).append('<ul class="oc-ui-checkbox-list">');
    $.each(capabilities, function(i, v) {
      $(sched.inputList).append('<li><input type="checkbox" id="' + v + '" value="' + v + '">&nbsp;<label for="' + v +'">' + v.charAt(0).toUpperCase() + v.slice(1).toLowerCase() + '</label></li>');
    });
    if(this.mode === CREATE_MODE) {
      $(':input', this.inputList).attr('checked', 'checked');
    }
    $(this.inputList).append('</ul>');
    this.capture.components.resources.setFields(capabilities);
    if(this.selectedInputs && ocUtils.getURLParam('edit')) {
      this.capture.components.resources.setValue(this.selectedInputs);
    }
    // Validate if an input was chosen
    this.inputCount = $(this.inputList).children('input:checkbox').size();
    total = this.inputCount;
    $(this.inputList).each(function() {
        $(this).children('input:checkbox').click(function() {
          total = (this.checked) ? (total = (total < 3) ? total+=1 : total) : total-=1;
          if(total < 1) {
        	  var position = $('#help_input').position();
        	  $('#inputhelpBox').css('left',position.left + 100 +'px');
        	  $('#inputhelpBox').css('top',position.top);
        	  $('#inputhelpTitle').text('Please Note');
        	  $('#inputhelpText').text('You have to select at least one input in order to schedule a recording.');
        	  $('#inputhelpBox').show();
          }
          else {
        	  $('#inputhelpBox').hide();
          }
        });
      });
  };

  sched.handleAgentTZ = function(tz) {
    var agentLocalTime = null;
    var localTZ = -(new Date()).getTimezoneOffset(); //offsets in minutes
    sched.tzDiff = 0;
    if(tz != localTZ){
      //Display note of agent TZ difference, all times local to capture agent.
      //update time picker to agent time
      sched.tzDiff = tz - localTZ;
      if(this.type == SINGLE_EVENT) {
        agentLocalTime = this.recording.components.startDate.getValue() + (sched.tzDiff * 60 * 1000);
        this.recording.components.startDate.setValue(agentLocalTime);
      }else if(this.type == MULTIPLE_EVENTS){
        agentLocalTime = this.recording.components.recurrenceStart.getValue() + (sched.tzDiff * 60 * 1000);
        this.recording.components.recurrenceStart.setValue(agentLocalTime);
      }
      diff = Math.round((sched.tzDiff/60)*100)/100;
      if(diff < 0) {
        postfix = " hours earlier";
      }else if(diff > 0) {
        postfix = " hours later"; 
      }
      $('#noticeContainer').show();
      $('#noticeTzDiff').show();
      $('#tzdiff').replaceWith(Math.abs(diff) + postfix);
    }
  };

  sched.checkAgentStatus = function(doc) {
    var state = $('state', doc).text();
    if(state == '' || state == 'unknown' || state == 'offline') {
      $('#noticeContainer').show();
      $('#noticeOffline').show();
    }
  };

/**
 *  loadKnownAgents calls the capture-admin service to get a list of known agents.
 *  Calls handleAgentList to populate the dropdown.
 */
  sched.loadKnownAgents = function() {
    $(this.agentList).empty();
    $(this.agentList).append($('<option></option>').val('').html('Choose one:'));
    $.get(CAPTURE_ADMIN_URL + '/agents', this.handleAgentList, 'xml');
  };

/**
 *  Popluates dropdown with known agents
 *
 *  @param {XML Document}
 */
  sched.handleAgentList = function(data) {
    $.each($('name', data),
      function(i, agent) {
        $(sched.agentList).append($('<option></option>').val($(agent).text()).html($(agent).text())); 
      });
    sched.loadEvent();
  };

  sched.loadEvent = function(){
    var eventId = ocUtils.getURLParam('eventId');
    if(eventId && ocUtils.getURLParam('edit')) {
      $.ajax({
        type: "GET",
        url: SCHEDULER_URL + '/' + eventId + '.json',
        success: function(data) { 
          sched.dublinCore.deserialize(data);
        },
        cache: false
      });
      $.ajax({
        type: "GET",
        url: SCHEDULER_URL + '/' + eventId + '/agent.properties',
        success: function(data) { 
          sched.capture.deserialize(data);
        },
        cache: false
      });
    }
    /*
    var event = doc.event;
    var workflowProperties = {};
    var additionalMetadata;
    if(typeof event.additionalMetadata.metadata != 'undefined') {
     additionalMetadata = event.additionalMetadata.metadata;
      for(var i in additionalMetadata){
        var item = additionalMetadata[i];
        if(item.key.indexOf('org.opencastproject.workflow') > -1) {
          workflowProperties[item.key] = item.value
        } else { //flatten additional metadata into event
          event[item.key] = item.value
        }
      }
    }
    if(event['resources']){
      //store the selected inputs for use when getting the capabilities.
      sched.selectedInputs = event['resources'];
    }
    if(event['seriesId']){
      $.get(SERIES_URL + '/' + event['seriesId'] + '.json', function(data){
        var series = {id: data.series.id};
        if(data.series.additionalMetadata){
          if(!$.isArray(data.series.additionalMetadata.metadata)){
            md = [data.series.additionalMetadata.metadata];
          } else {
            md = data.series.additionalMetadata.metadata;
          }
          for(var i in md){
            if(typeof md[i].key != 'undefined' && md[i].key === 'title') {
              series.label = md[i].value;
            }
          }
        }
        ocScheduler.components.seriesId.setValue(series);
      });
      delete event.seriesId;
    }
    if(workflowProperties['org.opencastproject.workflow.definition']) {
      ocScheduler.additionalMetadataComponents.workflowDefinition.setValue(workflowProperties['org.opencastproject.workflow.definition']);
      ocWorkflow.definitionSelected(workflowProperties['org.opencastproject.workflow.definition'],
        $('#workflowConfigContainer'),
        function(){
          if(ocWorkflowPanel && ocWorkflowPanel.registerComponents && ocWorkflowPanel.setComponentValues) {
            ocWorkflowPanel.registerComponents(ocScheduler.FormManager.workflowComponents);
            ocWorkflowPanel.setComponentValues(workflowProperties, ocScheduler.FormManager.workflowComponents);
          } else {
            ocUtils.log("Couldn't register workflow panel components");
          }
        });
    }
    ocScheduler.FormManager.populate(event)
    $('#agent').change(); //update the selected agent's capabilities
    */
  }

  sched.eventSubmitComplete = function(xhr, status) {
    ocUtils.log(status);
    if(status == "success") {
      document.location = RECORDINGS_URL;
    }
  }

  sched.checkForConflictingEvents = function() {
    var start, end;
    var data = {
      device: '',
      start: 0,
      end: 0,
      duration: 0,
      rrule: ''
    };
    this.conflictingEvents = false;
    $('#missingFieldsContainer').hide();
    $('#missingFieldsContainer li').hide();
    $('#errorConflict').hide();
    $('#conflictingEvents').empty();
    if(sched.dublinCore.components.device.validate()) {
      data.device = sched.dublinCore.components.device.getValue()
    }else{
      return false;
    }
    if(this.type === SINGLE_EVENT) {
      if(sched.recording.components.startDate.validate() && sched.recording.components.duration.validate()) {
        data.start = sched.recording.components.startDate.getValue();
        data.duration = sched.recording.components.duration.getValue();
        data.end = data.start + data.duration;
      } else {
        return false;
      }
    } else if(sched.recording.type === MULTIPLE_EVENTS) {
      if(sched.recording.components.recurrenceStart.validate() && sched.recording.components.recurrenceEnd.validate() &&
          sched.recording.components.recurrence.validate() && sched.recording.components.recurrenceDuration.validate()){
        data.start = sched.recording.components.recurrenceStart.getValue();
        data.end = sched.recording.components.recurrenceEnd.getValue();
        data.duration = sched.recording.components.recurrenceDuration.getValue();
        data.rrule = sched.recording.components.recurrence.getValue();
      } else {
        return false;
      }
    }
    $.post(SCHEDULER_URL + "/conflict.json", data, function(data) {
      var events = [];
      if(data != '') {
        if(!$.isArray(data.events.event)) {
          if(sched.mode === CREATE_MODE || (sched.mode === EDIT_MODE && $('#eventId').val() !== data.events.event.id)) {
            events.push(data.events.event);
          }
          if(events.length == 0) {
            return;
          }
        } else {
          events = data.events.event;
        }
        for(i in events) {
          curId = $('#eventId').val();
          eid = events[i].id;
          if(sched.mode === CREATE_MODE || (sched.mode === EDIT_MODE && curId !== eid)) {
            sched.conflictingEvents = true;
            $('#conflictingEvents').append('<li><a href="scheduler.html?eventId=' + events[i].id + '&edit=true" target="_new">' + events[i].title + '</a></li>');
          }
        }
        if(sched.conflictingEvents) {
          $('#missingFieldsContainer').show();
          $('#errorConflict').show();
        }
      }
    });
  }

  sched.registerCatalogs = function registerCatalogs() {
    var recComps = {};
    var dcComps = {};
    var extraComps = {};
    
    dcComps.title = new ocAdmin.Component(['title'], { key: 'title', required: true });
    dcComps.creator = new ocAdmin.Component(['creator'], { key: 'creator' });
    dcComps.contributor = new ocAdmin.Component(['contributor'], { key: 'contributor' });
    dcComps.seriesId = new ocAdmin.Component(['series', 'seriesSelect'],
      { required: true, key: 'ispartof' },
      { getValue: function() { 
          if(this.fields.series) {
            this.value = this.fields.series.val();
          }
          return this.value;
        },
        setValue: function(value) {
          this.fields.series.val(value.id);
          this.fields.seriesSelect.val(value.label)
        },
        asString: function() {
          if(this.fields.seriesSelect) {
            return this.fields.seriesSelect.val();
          }
          return this.getValue() + '';
        },
        validate: function() {
          if(this.fields.seriesSelect.val() !== '' && this.fields.series.val() === '') { //have text and no id
            return this.createSeriesFromSearchText();
          }
          return true; //nothing, or we have an id.
        },
        toNode: function(parent) {
          if(parent) {
            doc = parent.ownerDocument;
          } else {
            doc = document;
          }
          if(this.getValue() != "" && this.asString() != "") { //only add series if we have both id and name.
            seriesId = doc.createElement(this.key[0]);
            seriesId.appendChild(doc.createTextNode(this.getValue()));
            seriesName = doc.createElement(this.key[1]);
            seriesName.appendChild(doc.createTextNode(this.asString()));
            if(parent && parent.nodeType){
              parent.appendChild(seriesId);
              parent.appendChild(seriesName);
            }else{
              ocUtils.log('Unable to append node to document. ', parent, seriesId, seriesName);
            }
          }
        },
        createSeriesFromSearchText: function(){
          var series, seriesComponent, seriesId;
          var creationSucceeded = false;
          if(this.fields.seriesSelect !== ''){
            series = '<series><additionalMetadata><metadata><key>title</key><value>' + this.fields.seriesSelect.val() + '</value></metadata></additionalMetadata></series>';
            seriesComponent = this;
            $.ajax({
              async: false,
              type: 'PUT',
              url: SERIES_URL + '/',
              data: { series: series },
              dataType: 'json',
              success: function(data){
                creationSucceeded = true;
                seriesComponent.fields.series.val(data.series.id);
              }
            });
          }
          return creationSucceeded;
        }
      });
    dcComps.subject = new ocAdmin.Component(['subject'], { key: 'subject' });
    dcComps.language = new ocAdmin.Component(['language'], { key: 'language' });
    dcComps.description = new ocAdmin.Component(['description'], { key: 'description' });
    extraComps.resources = new ocAdmin.Component([],
      { key: 'capture.device.names' },
      { getValue: function() {
          var selected = [];
          for(var el in this.fields){
            var e = this.fields[el];
            if(e[0] && e[0].checked){
              selected.push(e.val());
            }
          }
          this.value = selected.toString();
          return this.value;
        },
        setValue: function(value) {
          if(typeof value == 'string') {
            value = { resources: value };
          }
          for(var el in this.fields) {
            var e = this.fields[el];
            if(e[0] && value.resources.toLowerCase().indexOf(e.val().toLowerCase()) != -1){
              e[0].checked = true;
            }else{
              e[0].checked = false;
            }
          }
        },
        validate: function() {
          var checked = false;
          for(var el in this.fields) {
            if(this.fields[el][0].checked) {
              checked = true;
              break;
            }
          }
          return checked;
        }
      });

    extraComps.workflowDefinition = new ocAdmin.Component(['workflowSelector'], {key: 'org.opencastproject.workflow.definition'});
    
    if(sched.type === MULTIPLE_EVENTS){
      //Series validation override for recurring events.
      dcComps.seriesId.validate = function() {
        if(this.fields.series.val() !== '') { //Already have an id
          return true;
        } else if(this.fields.seriesSelect.val() !== '') { //have text but no id
          return this.createSeriesFromSearchText();
        }
        return false; //nothing
      };
      
      dcComps.temporal = new ocAdmin.Component(['recurDurationHour', 'recurDurationMin', 'recurStart', 'recurStartTimeHour', 'recurStartTimeMin'],
        { key: 'temporal'},
        { getValue: function() {
            var date = this.fields.recurStart.datepicker('getDate');
            if(date && date.constructor == Date) {
              var start = date / 1000; // Get date in milliseconds, convert to seconds.
              start += this.fields.recurStartTimeHour.val() * 3600; // convert hour to seconds, add to date.
              start += this.fields.recurStartTimeMin.val() * 60; //convert minutes to seconds, add to date.
              start -= sched.tzDiff * 60; //Agent TZ offset
              start = start * 1000; //back to milliseconds
            }
            var duration = this.fields.recurDurationHour.val() * 3600; // seconds per hour
            duration += this.fields.recurDurationMin.val() * 60; // seconds per min
            ocUtils.log(start, duration);
          }
        });
      
      recComps.recurrenceStart = new ocAdmin.Component(['recurStart', 'recurStartTimeHour', 'recurStartTimeMin'],
        { required: true, key: 'startDate' },
        { getValue: function() {
            var date, start;
            date = this.fields.recurStart.datepicker('getDate');
            if(date && date.constructor == Date) {
              start = date / 1000; // Get date in milliseconds, convert to seconds.
              start += this.fields.recurStartTimeHour.val() * 3600; // convert hour to seconds, add to date.
              start += this.fields.recurStartTimeMin.val() * 60; //convert minutes to seconds, add to date.
              start -= sched.tzDiff * 60; //Agent TZ offset
              start = start * 1000; //back to milliseconds
              return start;
            }
          },
          setValue: function(value) {
            var date;
            date = parseInt(value);
            
            if(date != 'NaN') {
              date = new Date(date + (sched.tzDiff * 60 * 1000));
            } else {
              ocUtils.log('Could not parse date.');
            }
            if(this.fields.recurStart && this.fields.recurStartTimeHour && this.fields.recurStartTimeMin) {
              this.fields.recurStartTimeHour.val(date.getHours());
              this.fields.recurStartTimeMin.val(date.getMinutes());
              this.fields.recurStart.datepicker('setDate', date); //datepicker modifies the date object removing the time.
            }
          },
          validate: function() {
            var date, now, startdatetime;
            if(this.fields.recurStart.datepicker) {
              date = this.fields.recurStart.datepicker('getDate');
              now = (new Date()).getTime();
              now += sched.tzDiff  * 60 * 1000; //Offset by the difference between local and client.
              now = new Date(now);
              if(date && this.fields.recurStartTimeHour && this.fields.recurStartTimeMin) {
                startdatetime = new Date(date.getFullYear(), date.getMonth(), date.getDate(), this.fields.recurStartTimeHour.val(), this.fields.recurStartTimeMin.val());
                if(startdatetime.getTime() >= now.getTime()) {
                  return true;
                }
              }
            }
            return false;
          },
          asString: function() {
            return (new Date(this.getValue())).toLocaleString();
          }
        });
      
      recComps.recurrenceDuration = new ocAdmin.Component(['recurDurationHour', 'recurDurationMin'],
        { required: true, key: 'duration' },
        { getValue: function() {
            if(this.validate()) {
              duration = this.fields.recurDurationHour.val() * 3600; // seconds per hour
              duration += this.fields.recurDurationMin.val() * 60; // seconds per min
              this.value = (duration * 1000);
            }
            return this.value;
          },
          setValue: function(value) {
            var val, hour, min;
            if(typeof value === 'string') {
              value = { duration: value };
            }
            val = parseInt(value.duration);
            if(val === 'NaN') {
              ocUtils.log('Could not parse duration.');
            }
            if(this.fields.recurDurationHour && this.fields.recurDurationMin) {
              val   = val/1000; //milliseconds -> seconds
              hour  = Math.floor(val/3600);
              min   = Math.floor((val/60) % 60);
              this.fields.recurDurationHour.val(hour);
              this.fields.recurDurationMin.val(min);
            }
          },
          validate: function() {
            if(this.fields.recurDurationHour && this.fields.recurDurationMin && (this.fields.recurDurationHour.val() != '0' || this.fields.recurDurationMin.val() != '0')) {
              return true;
            }
            return false;
          },
          asString: function() {
            var dur = this.getValue() / 1000;
            var hours = Math.floor(dur / 3600);
            var min   = Math.floor( ( dur /60 ) % 60 );
            return hours + ' hours, ' + min + ' minutes';
          }
        });
  
      recComps.recurrenceEnd = new ocAdmin.Component(['recurEnd', 'recurStart', 'recurStartTimeHour', 'recurStartTimeMin'],
        { required: true, key: 'endDate' },
        { getValue: function() {
            var date, end;
            if(this.validate()) {
              date = this.fields.recurEnd.datepicker('getDate');
              if(date && date.constructor === Date) {
                end = date.getTime() / 1000; // Get date in milliseconds, convert to seconds.
                end += this.fields.recurStartTimeHour.val() * 3600; // convert hour to seconds, add to date.
                end += this.fields.recurStartTimeMin.val() * 60; //convert minutes to seconds, add to date.
                end -= sched.tzDiff * 60; //Agent TZ offset
                end = end * 1000; //back to milliseconds
                end += sched.recording.components.recurrenceDuration.getValue(); //Add to duration start time for end time.
                this.value = end;
              }
            }
            return this.value;
          },
          setValue: function(value) {
            var val = parseInt(value);
            if(val == 'NaN') {
              this.fields.recurEnd.datepicker('setDate', new Date(val));
            }
          },
          validate: function() {
            if(this.fields.recurEnd.datepicker && this.fields.recurStart.datepicker &&    // ocScheduler.components.recurrenceDuration.validate() &&
               this.fields.recurStartTimeHour && this.fields.recurStartTimeMin &&
               this.fields.recurEnd.datepicker('getDate') > this.fields.recurStart.datepicker('getDate')) {
              return true;
            }
            return false;
          },
          asString: function() {
            return (new Date(this.getValue())).toLocaleString();
          }
        });
  
      dcComps.device = new ocAdmin.Component(['recurAgent'],
        { required: true, key: 'spatial' },
        { getValue: function(){
            if(this.fields.recurAgent) {
              this.value = this.fields.recurAgent.val();
            }
            return this.value;
          },
          setValue: function(value) {
            var opts, agentId, found;
            if(typeof value === 'string') {
              value = { agent: value };
            }
            opts = this.fields.recurAgent.children();
            agentId = value.agent;
            if(opts.length > 0) {
              found = false;
              for(var i = 0; i < opts.length; i++) {
                if(opts[i].value == agentId) {
                  found = true;
                  opts[i].selected = true;
                  break;
                }
              }
              if(!found) { //Couldn't find the previsouly selected agent, add to list and notifiy user.
                this.fields.recurAgent.append($('<option selected="selected">' + agentId + '</option>').val(agentId));
                $('#recurAgent').change();
              }
              this.fields.recurAgent.val(agentId);
            }
          },
          validate: function() {
            if(this.getValue()) {
              return true;
            }
            return false;
          }
        });
  
      recComps.recurrence = new ocAdmin.Component(['scheduleRepeat', 'repeatSun', 'repeatMon', 'repeatTue', 'repeatWed', 'repeatThu', 'repeatFri', 'repeatSat'],
        { required: true, key: 'recurrencePattern' },
        { getValue: function() {
            var rrule, dotw, days, date, hour, min, dayOffset;
            if(this.validate()) {
              if(this.fields.scheduleRepeat.val() == 'weekly') {
                rrule     = "FREQ=WEEKLY;BYDAY=";
                dotw      = ['SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA'];
                days      = [];
                date      = new Date(ocScheduler.components.recurrenceStart.getValue());
                hour      = date.getUTCHours();
                min       = date.getUTCMinutes();
                dayOffset = 0;
                if(date.getDay() != date.getUTCDay()) {
                  dayOffset = date.getDay() < date.getUTCDay() ? 1 : -1;
                }
                if(this.fields.repeatSun[0].checked) {
                  days.push(dotw[(0 + dayOffset) % 7]);
                }
                if(this.fields.repeatMon[0].checked) {
                  days.push(dotw[(1 + dayOffset) % 7]);
                }
                if(this.fields.repeatTue[0].checked) {
                  days.push(dotw[(2 + dayOffset) % 7]);
                }
                if(this.fields.repeatWed[0].checked) {
                  days.push(dotw[(3 + dayOffset) % 7]);
                }
                if(this.fields.repeatThu[0].checked) {
                  days.push(dotw[(4 + dayOffset) % 7]);
                }
                if(this.fields.repeatFri[0].checked) {
                  days.push(dotw[(5 + dayOffset) % 7]);
                }
                if(this.fields.repeatSat[0].checked) {
                  days.push(dotw[(6 + dayOffset) % 7]);
                }
                this.value = rrule + days.toString() + ";BYHOUR=" + hour + ";BYMINUTE=" + min;
              }
            }
            return this.value;
          },
          setValue: function(value) {
            //to do, handle day offset.
            if(typeof value == 'string') {
              value = { rrule: value };
            }
            if(value.rrule.indexOf('FREQ=WEEKLY') != -1) {
              this.fields.scheduleRepeat.val('weekly');
              var days = value.rrule.split('BYDAY=');
              if(days[1].length > 0){
                days = days[1].split(',');
                this.fields.repeatSun[0].checked = this.fields.repeatMon[0].checked = this.fields.repeatTue[0].checked = this.fields.repeatWed[0].checked = this.fields.repeatThu[0].checked = this.fields.repeatFri[0].checked = this.fields.repeatSat[0].checked = false;
                for(d in days){
                  switch(days[d]){
                    case 'SU':
                      this.fields.repeatSun[0].checked = true;
                      break;
                    case 'MO':
                      this.fields.repeatMon[0].checked = true;
                      break;
                    case 'TU':
                      this.fields.repeatTue[0].checked = true;
                      break;
                    case 'WE':
                      this.fields.repeatWed[0].checked = true;
                      break;
                    case 'TH':
                      this.fields.repeatThu[0].checked = true;
                      break;
                    case 'FR':
                      this.fields.repeatFri[0].checked = true;
                      break;
                    case 'SA':
                      this.fields.repeatSat[0].checked = true;
                      break;
                  }
                }
              }
            }
          },
          validate: function() {
            if(this.fields.scheduleRepeat.val() != 'norepeat') {
              if(this.fields.repeatSun[0].checked ||
                 this.fields.repeatMon[0].checked ||
                 this.fields.repeatTue[0].checked ||
                 this.fields.repeatWed[0].checked ||
                 this.fields.repeatThu[0].checked ||
                 this.fields.repeatFri[0].checked ||
                 this.fields.repeatSat[0].checked ){
                if(ocScheduler.components.recurrenceStart.validate() &&
                   // ocScheduler.components.recurrenceDuration.validate() &&
                   ocScheduler.components.recurrenceEnd.validate()) {
                  return true;
                }
              }
            }
            return false;
          },
          toNode: function(parent) {
            for(var el in this.fields) {
              var container = parent.ownerDocument.createElement(this.nodeKey);
              container.appendChild(parent.ownerDocument.createTextNode(this.getValue()));
            }
            if(parent && parent.nodeType) {
              parent.appendChild(container);
            }
            return container;
          }
        });
                                                                          
    }else{ //Single Event
      
      dcComps.eventId = new ocAdmin.Component(['eventId'],
        { key: 'identifier' },
        { toNode: function(parent) {
            for(var el in this.fields) {
              var container = parent.ownerDocument.createElement(this.nodeKey);
              container.appendChild(parent.ownerDocument.createTextNode(this.getValue()));
            }
            if(parent && parent.nodeType) {
              parent.appendChild(container);
            }
            return container;
          }
        });
      
      dcComps.temporal = new ocAdmin.Component(['durationHour', 'durationMin', 'startDate', 'startTimeHour', 'startTimeMin'],
          { key: 'temporal'},
          { getValue: function() {
              var date = this.fields.startDate.datepicker('getDate');
              if(date && date.constructor == Date) {
                var start = date / 1000; // Get date in milliseconds, convert to seconds.
                start += this.fields.startTimeHour.val() * 3600; // convert hour to seconds, add to date.
                start += this.fields.startTimeMin.val() * 60; //convert minutes to seconds, add to date.
                start -= sched.tzDiff * 60; //Agent TZ offset
                start = start * 1000; //back to milliseconds
              }
              var end = this.fields.durationHour.val() * 3600; // seconds per hour
              end += this.fields.durationMin.val() * 60 * 1000; // milliseconds per min
              end += start;
              ocUtils.log(start, end);
              return 'start=' + ocUtils.toISODate(new Date(start)) + 
                '; end=' + ocUtils.toISODate(new Date(end)) + '; scheme=W3C-DTF;';
            }
          });
      
      recComps.startDate = new ocAdmin.Component(['startDate', 'startTimeHour', 'startTimeMin'],
        { required: true, key: 'startDate' },
        { getValue: function() {
            var date = 0;
            date = this.fields.startDate.datepicker('getDate').getTime() / 1000; // Get date in milliseconds, convert to seconds.
            date += this.fields.startTimeHour.val() * 3600; // convert hour to seconds, add to date.
            date += this.fields.startTimeMin.val() * 60; //convert minutes to seconds, add to date.
            date -= sched.tzDiff * 60; //Agent TZ offset
            date = date * 1000; //back to milliseconds
            return (new Date(date)).getTime();
          },
          setValue: function(value) {
            var date, hour;
            date = parseInt(value);
            
            if(date != 'NaN') {
              date = new Date(date + (sched.tzDiff * 60 * 1000));
            } else {
              ocUtils.log('Could not parse date.');
            }
            if(this.fields.startDate && this.fields.startTimeHour && this.fields.startTimeMin) {
              hour = date.getHours();
              this.fields.startTimeHour.val(hour);
              this.fields.startTimeMin.val(date.getMinutes());
              this.fields.startDate.datepicker('setDate', date);//datepicker modifies the date object removing the time.
            }
          },
          validate: function() {
            var date, now, startdatetime;
            date = this.fields.startDate.datepicker('getDate');
            now = (new Date()).getTime();
            now += sched.tzDiff  * 60 * 1000; //Offset by the difference between local and client.
            now = new Date(now);
            if(this.fields.startDate && date && this.fields.startTimeHour && this.fields.startTimeMin) {
              startdatetime = new Date(date.getFullYear(), 
                                       date.getMonth(),
                                       date.getDate(),
                                       this.fields.startTimeHour.val(),
                                       this.fields.startTimeMin.val());
              if(startdatetime.getTime() >= now.getTime()) {
                return true;
              }
              return false;
            }
          },
          asString: function() {
            return (new Date(this.getValue())).toLocaleString();
          }
        });
  
      recComps.duration = new ocAdmin.Component(['durationHour', 'durationMin'],
        { key: 'duration', required: true },
        { getValue: function() {
            if(this.validate()) {
              duration = this.fields.durationHour.val() * 3600; // seconds per hour
              duration += this.fields.durationMin.val() * 60; // seconds per min
              this.value = duration * 1000;
            }
            return this.value;
          },
          setValue: function(value) {
            var val, hour, min;
            if(typeof value == 'string' || typeof value == 'number') {
              value = { duration: value };
            }
            val = parseInt(value.duration);
            if(val == 'NaN') {
              ocUtils.log('Could not parse duration.');
            }
            if(this.fields.durationHour && this.fields.durationMin) {
              val = val/1000; //milliseconds -> seconds
              hour  = Math.floor(val/3600);
              min   = Math.floor((val/60) % 60);
              this.fields.durationHour.val(hour);
              this.fields.durationMin.val(min);
            }
          },
          validate: function() {
            if(this.fields.durationHour && this.fields.durationMin && (this.fields.durationHour.val() !== '0' || this.fields.durationMin.val() !== '0')){
              return true;
            }
            return false;
          },
          toNode: function(parent) {
            var duration, endDate, doc;
            if(parent){
              doc = parent.ownerDocument;
            }else{
              doc = document;
            }
            duration = doc.createElement('duration');
            duration.appendChild(doc.createTextNode(this.getValue()));
            parent.appendChild(duration);
            if(typeof ocScheduler.components.startDate != 'undefined' && ocScheduler.components.startDate.getValue() != null) {
              endDate = doc.createElement('endDate');
              endDate.appendChild(doc.createTextNode(ocScheduler.components.startDate.getValue() + this.getValue()));
              parent.appendChild(endDate);
            }
          },
          asString: function() {
            var dur = this.getValue() / 1000;
            var hours = Math.floor(dur / 3600);
            var min   = Math.floor( ( dur /60 ) % 60 );
            return hours + ' hours, ' + min + ' minutes';
          }
        });
  
      dcComps.device = new ocAdmin.Component(['agent'],
        { required: true, key: 'spatial' },
        { getValue: function() {
            if(this.fields.agent) {
              this.value = this.fields.agent.val();
            }
            return this.value;
          },
          setValue: function(value) {
            var opts, agentId, found;
            if(typeof value === 'string') {
              value = { agent: value };
            }
            opts = this.fields.agent.children();
            agentId = value.agent;
            if(opts.length > 0){
              found = false;
              for(var i = 0; i < opts.length; i++) {
                if(opts[i].value == agentId){
                  found = true;
                  opts[i].selected = true;
                  break;
                }
              }
              if(!found) { //Couldn't find the previsouly selected agent, add to list and notifiy user.
                this.fields.agent.append($('<option selected="selected">' + agentId + '</option>').val(agentId));
                $('#agent').change();
              }
              this.fields.agent.val(agentId);
            }
          },
          validate: function() {
            if(this.getValue()) {
              return true;
            }
            return false;
          }
        });
    }
    this.dublinCore.components = dcComps;
    this.recording.components = recComps;
    this.capture.components = extraComps;
  }
  
  return sched;
}());
