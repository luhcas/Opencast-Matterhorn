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

var ocRecordings = ocRecordings || {};

ocRecordings.statsInterval = null;
ocRecordings.updateRequested = false;
ocRecordings.currentState = null;
ocRecordings.sortBy = "startDate";
ocRecordings.sortOrder = "Descending";
ocRecordings.lastCount = null;
ocRecordings.tableInterval = null;
ocRecordings.tableUpdateRequested = false;
ocRecordings.changedMediaPackage = null;
ocRecordings.configuration = null;
ocRecordings.tableTemplate = null;


/** Initialize the Recordings page.
 *  Register event handlers.
 *  Set up parser for the date/time field so that tablesorter can sort this col to.
 *  Init periodical update of recording statistics.
 *  Display the recordings specified by the URL param show or upcoming recordings otherwise.
 */
ocRecordings.init = function() {
  //Do internationalization of text
  jQuery.i18n.properties({
    name:'recordings',
    path:'i18n/'
  });
  ocAdmin.internationalize(i18n, 'i18n');
  
  // get config
  $.getJSON("/info/rest/components.json", function(data) {
    ocRecordings.configuration = data;
    $('#engagelink').attr('href', data.engage + '/engage/ui');
  });

  // get 'me'
  $.getJSON("/info/rest/me.json", function(data) {
    ocRecordings.me = data;
    $('#logout').append(" '" + data.username + "'");
  });

  // Event: clicked somewhere
  //  $('body').click( function() {
  //    $('#holdActionPanelContainer').fadeOut('fast');
  //  });
  
  $("#buttonSchedule").button({icons:{primary:'ui-icon-circle-plus'}});
  $("#buttonUpload").button({icons:{primary:'ui-icon-circle-plus'}});

  /* Event: Scheduler button clicked */
  $('#buttonSchedule').click( function() {
    window.location.href = '../../admin/scheduler.html';
  });

  /* Event: Upload button clicked */
  $('#buttonUpload').click( function() {
    window.location.href = '../../admin/upload.html';
  });

  /* Event: Recording State selector clicked */
  $('.state-selector').click( function() {
    var state = $(this).attr('state');
    ocUtils.log('state', state);
    window.location.href = 'recordings.html?show='+state+'&sortBy='+ocRecordings.sortBy+'&sortOrder='+ocRecordings.sortOrder+'&pageSize='+ocPager.pageSize;
    return false;
  });

  $('#refreshEnabled').click( function() {
    if ($(this).is(':checked')) {
      $('#refreshInterval').removeAttr('disabled');
      $('.refresh-text').removeClass('refresh-text-disabled').addClass('refresh-text-enabled');
      ocRecordings.initTableRefresh($('#refreshInterval').val());
    } else {
      $('#refreshInterval').attr('disabled','true');
      $('.refresh-text').removeClass('refresh-text-enabled').addClass('refresh-text-disabled');
      window.clearInterval(ocRecordings.tableInterval);
      ocRecordings.tableUpdateRequested = false;
    }
  });

  $('#refreshInterval').change(function() {
    ocRecordings.initTableRefresh($(this).val());
  });

  var sort = ocUtils.getURLParam('sortBy');
  if (sort == '') {
    sort='StartDate';
  }
  ocRecordings.sortBy = sort;

  var order = ocUtils.getURLParam('sortOrder');
  if (order == '') {
    order='Descending';
  }
  ocRecordings.sortOrder = order;

  var psize = ocUtils.getURLParam('pageSize');
  if (psize == '') {
    psize = 10;
  }
  ocPager.pageSize = psize;
  ocPager.init();

  var show = ocUtils.getURLParam('show');
  if (show == '') {
    show='upcoming';
  }
  ocRecordings.currentState = show;
  ocRecordings.displayRecordingStats();
  ocUtils.getTemplate(show, function(template) {
    ocRecordings.tableTemplate = template;
    ocRecordings.displayRecordings(show, false);
  });

  if (ocRecordings.currentState == "upcoming" || ocRecordings.currentState == "finished") {
    $('#infoBox').css("display", "block");
    $('#'+ ocRecordings.currentState +'TableInfoBox').css("display","block");
  }

  // init update interval for recording stats
  ocRecordings.statsInterval = window.setInterval( 'ocRecordings.displayRecordingStats();', 3000 );
  if (show == 'all' || show == 'capturing' || show == 'processing') {
    $('#refreshControlsContainer').css('display','block');
    if ($('#refreshEnabled').is(':visible') && $('#refreshEnabled').is(':checked')) {
      $('.refresh-text').removeClass('refresh-text-disabled').addClass('refresh-text-enabled');
      ocRecordings.initTableRefresh($('#refreshInterval').val());
    }
  } else {
    $('#refreshControlsContainer').css('display','none');
  }
}

/** (re-)initialize reloading of recordings table
 *
 */
ocRecordings.initTableRefresh = function(time) {
  if (ocRecordings.tableInterval != null) {
    window.clearInterval(ocRecordings.tableInterval);
  }
  ocRecordings.tableInterval = window.setInterval('ocRecordings.displayRecordings("' + ocRecordings.currentState + '",true);', time*1000);
}

/** get and display recording statistics. If the number of recordings in the
 * currently displayed state changes, the table is updated via displayRecordings().
 */
ocRecordings.displayRecordingStats = function() {
  if (!ocRecordings.updateRequested) {
    ocRecordings.updateRequested = true;
    $.ajax({
      url: "rest/countRecordings",
      type: "GET",
      cache: false,
      success: function(data) {
        ocRecordings.updateRequested = false;
        for (key in data) {
          if (ocRecordings.currentState == key) {
            if (ocRecordings.lastCount !== data[key]) {
              ocRecordings.lastCount = data[key];
              ocPager.update(ocPager.pageSize, ocPager.currentPageIdx);
              ocRecordings.displayRecordings(ocRecordings.currentState, true);
            } else {
              ocRecordings.lastCount = data[key];
            }
          }
          var elm = $('#' + key + 'Count');
          if (elm) {
            elm.text('(' + data[key] + ')');
          }
        }
      }
    });
  }
}

/** Request a list of recordings in a certain state and render the response as a table.
 *  While we are waiting for a response, a little animation is displayed.
 */
ocRecordings.displayRecordings = function(state, reload) {
  if (!ocRecordings.tableUpdateRequested && ocRecordings.tableTemplate != null) {
    ocRecordings.tableUpdateRequested = true;
    var page = ocPager.currentPageIdx;
    var psize = ocPager.pageSize;
    var sort = ocRecordings.sortBy;
    var order = ocRecordings.sortOrder;
    var recordingsUrl = "rest/recordings/"+ocRecordings.currentState+".json?ps="+psize+"&pn="+page+"&sb="+sort+"&so="+order;
    $.ajax({
      url : recordingsUrl,
      type : 'get',
      dataType : 'json',
      error : function(xhr) {
        ocUtils.log('Error: Could not get Recordings');   // TODO more detailed debug info
      },
      success : function(data) {
        var container = document.getElementById('recordingsTableContainer');
        container.innerHTML = ocRecordings.tableTemplate.process(data);
      }
    });
  }
}

/** Displays Hold Operation UI
 * @param URL of the hold action UI
 * @param wfId Id of the hold operations workflow
 * @param callerElm HTML element that invoked the UI (so that information from the recordings table row can be gathered
 */
ocRecordings.displayHoldActionPanel = function(URL, wfId, callerElm) {
  $('#holdActionPanelContainer iframe').attr('src', URL);
  $('#holdWorkflowId').val(wfId);
  var parentRow = $(callerElm).parent().parent();
  $('#holdStateHeadRowTitle').html($($(parentRow).children().get(0)).html());
  $('#holdStateHeadRowPresenter').html($($(parentRow).children().get(1)).html());
  $('#holdStateHeadRowSeries').html($($(parentRow).children().get(2)).html());
  $('#holdStateHeadRowDate').html($($(parentRow).children().get(3)).html());
  $('#holdStateHeadRowStatus').html($($(parentRow).children().get(4)).html());
  $('#holdActionPanelContainer').toggle();
  $('#recordingsTableContainer').hide();
  $('#categorySelectorContainer').parent().hide();
  $('#oc_recordingmenu').hide();
  $('.pagingNavContainer').hide();
  $('#refreshControlsContainer').hide();
}

/** Adjusts the height of the panel holding the Hold Operation UI
 *
 */
ocRecordings.adjustHoldActionPanelHeight = function() {
  var height = $("#holdActionPanelIframe").contents().find("html").height();
  $('#holdActionPanelIframe').height(height+10);
//alert("Hold action panel height: " + height);
}

/** Calls workflow endpoint to end hold operation and continue the workflow
 *
 */
ocRecordings.continueWorkflow = function(postData) {
  var workflowId = $('#holdWorkflowId').val();
  if(postData===null) {
    postData = {
      id : workflowId
    };
  }
  if (ocRecordings.changedMediaPackage != null) {
    postData['mediapackage'] = ocRecordings.changedMediaPackage;
    ocRecordings.changedMediaPackage = null;
  }
  
  // resume expects a "properties" form field with one key=value pair per line
  var props = {};
  props.id = workflowId;
  props.properties = "";
  $.each(postData, function(key, value) {
    if(key != 'id') {
      props.properties = props.properties + key + "=" + value + "\n"; 
    }
  });
  
  $.ajax({
    type       : 'POST',
    url        : '../workflow/rest/resume/',
    data       : props,
    error      : function(XHR,status,e){
      alert('Could not resume Workflow: ' + status);
    },
    success    : function(data) {
      $('#holdActionPanelContainer').toggle();
      $('#recordingsTableContainer').toggle();
      $('#oc_recordingmenu').toggle();
      $('.pagingNavContainer').toggle();
      $('#refreshControlsContainer').toggle();
      location.reload();
    }
  });
}

/** Show the recording editor
 *
 */
ocRecordings.retryRecording = function(workflowId) {
  location.href = "upload.html?retry=" + workflowId;
}

ocRecordings.removeRecording = function(workflowId) {
  $.ajax({
    url        : '../workflow/rest/stop',
    data       : {
      id: workflowId
    },
    type       : 'POST',
    error      : function(XHR,status,e){
      alert('Could not remove Workflow ' + workflowId);
    },
    success    : function(data) {
      ocRecordings.loadRecordingsXML();
    }
  });
}

ocRecordings.removeScheduledRecording = function(eventId, title) {
  if(confirm('Are you sure you wish to delete ' + title + '?')){
    $.ajax({
      url        : '/scheduler/rest/event/' + eventId,
      type       : 'DELETE',
      error      : function(XHR,status,e){
        alert('Could not remove Scheduler Event ' + workflowId);
      },
      success    : function(data) {
        ocRecordings.loadRecordingsXML();
      }
    });
  }
}

ocRecordings.formatRecordingDates = function(startTime, endTime) {
  var startDate = new Date(); startDate.setTime(startTime);
  var endDate = new Date(); endDate.setTime(endTime);
  var out = startDate.getFullYear() + '-' +
    ocUtils.padString(startDate.getMonth()+1, "0", 2) + '-' +
    ocUtils.padString(startDate.getDate(), "0", 2) + ' ' +
    startDate.getHours() + ":" + ocUtils.padString(startDate.getMinutes(), "0", 2) + ' - ' +
    endDate.getHours() + ":" + ocUtils.padString(endDate.getMinutes(), "0", 2);
  return out;
}
