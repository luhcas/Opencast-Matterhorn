/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the 'License'); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an 'AS IS'
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

var ocRecordings = ocRecordings || {};

ocRecordings.statsInterval = null;
ocRecordings.updateRequested = false;
ocRecordings.currentState = 'upcoming';
ocRecordings.sortBy = 'startDate';
ocRecordings.sortOrder = 'Ascending';
ocRecordings.lastCount = null;
ocRecordings.tableInterval = null;
ocRecordings.changedMediaPackage = null;
ocRecordings.configuration = null;
ocRecordings.tableTemplate = null;
ocRecordings.filter = '';
ocRecordings.filterFields = '';


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
  ocUtils.internationalize(i18n, 'i18n');
  
  // get config
  $.getJSON('/info/rest/components.json', function(data) {
    ocRecordings.configuration = data;
    $('#engagelink').attr('href', data.engage + '/engage/ui');
  });

  // get 'me'
  $.getJSON('/info/rest/me.json', function(data) {
    ocRecordings.me = data;
    $('#logout').append(' "' + data.username + '"');
  });

  // Event: clicked somewhere
  //  $('body').click( function() {
  //    $('#holdActionPanelContainer').fadeOut('fast');
  //  });
  
  $('#buttonSchedule').button({
    icons:{
      primary:'ui-icon-circle-plus'
    }
  });
  $('#buttonUpload').button({
    icons:{
      primary:'ui-icon-circle-plus'
    }
  });

  /* Event: Scheduler button clicked */
  $('#buttonSchedule').click( function() {
    window.location.href = '../../admin/scheduler.html';
  });

  /* Event: Upload button clicked */
  $('#buttonUpload').click( function() {
    window.location.href = '../../admin/upload.html';
  });

  /* Event: Recording State selector clicked */
  $('.recordings-category').click( function() {
    ocRecordings.resetBulkActionPanel();
    var state = $(this).attr('state');
    if(!$(this).hasClass('recordings-category-active')){
      $('.recordings-category').removeClass('recordings-category-active');
      $(this).addClass('recordings-category-active');
    }
    ocUtils.log('state', state);
    ocRecordings.displayRecordings($(this).attr('state'));
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
    }
  });

  $('#refreshInterval').change(function() {
    ocRecordings.initTableRefresh($(this).val());
  });
  
  // Bulk Action Event Handlers

  $('.oc-ui-collapsible-widget .ui-widget-header').click(
    function() {
      $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-e');
      $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-s');
      $(this).next().toggle();
      return false;
    });
    
  $('#bulkActionSelect').change(function(){
    ocRecordings.bulkActionHandler($(this).val());
  });
  
  $('.recordings-cancel-bulk-action').click(ocRecordings.cancelBulkAction);

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
  $('.recordings-category[state=' + show + ']').addClass('recordings-category-active');
  ocRecordings.currentState = show;
  ocRecordings.displayRecordingStats();

  if (ocRecordings.currentState == 'finished') {
    $('#infoBox').css('display', 'block');
    $('#'+ ocRecordings.currentState +'TableInfoBox').css('display','block');
  }

  // init update interval for recording stats
  ocRecordings.statsInterval = window.setInterval('ocRecordings.displayRecordingStats();', 3000 );
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
  ocRecordings.tableInterval = window.setInterval('ocRecordings.displayRecordings("' + ocRecordings.currentState + '");', time*1000);
}

/** get and display recording statistics. If the number of recordings in the
 * currently displayed state changes, the table is updated via displayRecordings().
 */
ocRecordings.displayRecordingStats = function() {
  if (!ocRecordings.updateRequested) {
    ocRecordings.updateRequested = true;
    $.ajax({
      url: 'rest/countRecordings',
      type: 'GET',
      cache: false,
      success: function(data) {
        ocRecordings.updateRequested = false;
        for (key in data) {
          if (ocRecordings.currentState == key) {
            if (ocRecordings.lastCount !== data[key]) {
              ocRecordings.lastCount = data[key];
              ocPager.update(ocPager.pageSize, ocPager.currentPageIdx);
              ocRecordings.displayRecordings(ocRecordings.currentState);
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
ocRecordings.displayRecordings = function(state) {
  var page = ocPager.currentPageIdx;
  var psize = ocPager.pageSize;
  var sort = ocRecordings.sortBy;
  var order = ocRecordings.sortOrder;
  var recordingsUrl = '';
  if(state == 'bulkaction'){
    recordingsUrl = 'rest/recordings/upcoming.json';
  } else {
    recordingsUrl = 'rest/recordings/' + state + '.json';
  }
  recordingsUrl += '?ps=' + psize + '&pn=' + page + '&sb=' + sort + '&so=' + order;
  if((state === 'upcoming' || state === 'bulkaction') && ocRecordings.filter !== '' && ocRecordings.filterField !== '') {
    recordingsUrl += '&filter=' + ocRecordings.filter + '&' + ocRecordings.filterField + '=true';
  }
  if(ocRecordings.currentState !== state || ocRecordings.tableTemplate == null) {
    ocUtils.getTemplate(state, function(template) {
      ocRecordings.tableTemplate = template;
      ocRecordings.displayRecordings(state);
    });
    ocRecordings.currentState = state;
    return;
  }
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
  var height = $('#holdActionPanelIframe').contents().find('html').height();
  $('#holdActionPanelIframe').height(height+10);
//alert('Hold action panel height: ' + height);
}

/** Calls workflow endpoint to end hold operation and continue the workflow
 *
 */
ocRecordings.continueWorkflow = function(postData) {
  // data must include workflow id
  var data = {
    id : $('#holdWorkflowId').val()
  };

  // add updated MP to data, if hold operation changed the MP
  if (ocRecordings.changedMediaPackage != null) {
    data['mediapackage'] = ocRecordings.changedMediaPackage;
    ocRecordings.changedMediaPackage = null;
  }

  // add properties for workflow resum if provided by hold operation
  if (postData) {
    data.properties = {};
    $.each(postData, function(key, value) {
      if(key != 'id') {
        data.properties += key + '=' + value + '\n';
      }
    });
  }
  
  $.ajax({
    type       : 'POST',
    url        : '../workflow/rest/replaceAndresume/',
    data       : data,
    error      : function(XHR,status,e){
      if (XHR.status == '204') {
        $('#holdActionPanelContainer').toggle();
        $('#recordingsTableContainer').toggle();
        $('#oc_recordingmenu').toggle();
        $('.pagingNavContainer').toggle();
        $('#refreshControlsContainer').toggle();
        location.reload();
      } else {
        alert('Could not resume Workflow: ' + status);
      }
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
  location.href = 'upload.html?retry=' + workflowId;
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
      url        : '/scheduler/rest/' + eventId,
      type       : 'DELETE',
      error      : function(XHR,status,e){
        alert('Could not remove Scheduler Event ' + workflowId);
      },
      success    : function(data) {
        ocRecordings.displayRecordings(ocRecordings.currentState);
      }
    });
  }
}

ocRecordings.formatRecordingDates = function(startTime, endTime) {
  var startDate = new Date();
  startDate.setTime(startTime);
  var endDate = new Date();
  endDate.setTime(endTime);
  var out = startDate.getFullYear() + '-' +
  ocUtils.padString(startDate.getMonth()+1, '0', 2) + '-' +
  ocUtils.padString(startDate.getDate(), '0', 2) + ' ' +
  startDate.getHours() + ':' + ocUtils.padString(startDate.getMinutes(), '0', 2) + ' - ' +
  endDate.getHours() + ':' + ocUtils.padString(endDate.getMinutes(), '0', 2);
  return out;
}

ocRecordings.filterRecordings = function(state) {
  if(!state){
    state = ocRecordings.currentState;
  }
  ocRecordings.filter = $('#filter').val();
  ocRecordings.filterField = $('#filterField').val();
  ocRecordings.displayRecordings(state);
}

ocRecordings.displayBulkAction = function(filter) {
  $('#bulkEditPanel').hide();
  $('#bulkDeletePanel').hide();
  ocRecordings.filterRecordings('bulkaction');
  $('#bulkActionPanel').show();
}

ocRecordings.cancelBulkAction = function() {
  ocRecordings.resetBulkActionPanel();
  ocRecordings.filterRecordings('upcoming');
}

ocRecordings.resetBulkActionPanel = function() {
  $('#bulkActionPanel').hide();
  $('#bulkActionSelect').val('select');
  $('#bulkActionSelect').change();
}

ocRecordings.bulkActionHandler = function(action) {
  if(action === 'edit'){
    $('#bulkEditPanel').show();
    $('#bulkDeletePanel').hide();
    $('#cancelBulkAction').hide();
  } else if (action === 'delete') {
    $('#bulkEditPanel').hide();
    $('#bulkDeletePanel').show();
    $('#cancelBulkAction').hide();
  } else if (action === 'select') {
    $('#bulkEditPanel').hide();
    $('#bulkDeletePanel').hide();
    $('#cancelBulkAction').show();
  }
}
