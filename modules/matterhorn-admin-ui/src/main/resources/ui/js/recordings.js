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

var Recordings = Recordings || {};

Recordings.statsInterval = null;
Recordings.updateRequested = false;
Recordings.currentState = null;
Recordings.currentXSL = null;
Recordings.sortBy = "startDate";
Recordings.sortOrder = "Descending";
Recordings.lastCount = null;
Recordings.tableInterval = null;
Recordings.tableUpdateRequested = false;
Recordings.changedMediaPackage = null;
Recordings.configuration = null;

/** Initialize the Recordings page.
 *  Register event handlers.
 *  Set up parser for the date/time field so that tablesorter can sort this col to.
 *  Init periodical update of recording statistics.
 *  Display the recordings specified by the URL param show or upcoming recordings otherwise.
 */
Recordings.init = function() {
  //Do internationalization of text
  jQuery.i18n.properties({
    name:'recordings',
    path:'i18n/'
  });
  AdminUI.internationalize(i18n, 'i18n');
  
  // get config
  $.getJSON("/info/rest/components.json", function(data) {
    Recordings.configuration = data;
    $('#engagelink').attr('href', data.engage + '/engage/ui');
  });

  // get 'me'
  $.getJSON("/info/rest/me.json", function(data) {
    Recordings.me = data;
    $('#logout').append(" '" + data.username + "'");
  });

  // Event: clicked somewhere
  //  $('body').click( function() {
  //    $('#holdActionPanel-container').fadeOut('fast');
  //  });

  // Buttons style
  $("button").hover(function(){
    $(this).css({
      'background-color': 'white',
      'border-top': '1px solid #ccc',
      'border-left': '1px solid #ccc'
    });
  },
  function(){
    $(this).css({
      'background-color': '',
      'border-top': '',
      'border-left': ''
    })
  });

  /* Event: Scheduler button clicked */
  $('#button_schedule').click( function() {
    window.location.href = '../../admin/scheduler.html';
  });

  /* Event: Upload button clicked */
  $('#button_upload').click( function() {
    window.location.href = '../../admin/upload.html';
  });

  /* Event: Recording State selector clicked */
  $('.state-selector').click( function() {
    var state = $(this).attr('state');
    window.location.href = 'recordings.html?show='+state+'&sortBy='+Recordings.sortBy+'&sortOrder='+Recordings.sortOrder+'&pageSize='+ocPager.pageSize;
    return false;
  });

  $('#refresh-enabled').click( function() {
    if ($(this).is(':checked')) {
      $('#refresh-interval').removeAttr('disabled');
      $('.refresh-text').removeClass('refresh-text-disabled').addClass('refresh-text-enabled');
      Recordings.initTableRefresh($('#refresh-interval').val());
    } else {
      $('#refresh-interval').attr('disabled','true');
      $('.refresh-text').removeClass('refresh-text-enabled').addClass('refresh-text-disabled');
      window.clearInterval(Recordings.tableInterval);
      Recordings.tableUpdateRequested = false;
    }
  });

  $('#refresh-interval').change(function() {
    Recordings.initTableRefresh($(this).val());
  });

  var sort = Recordings.getURLParam('sortBy');
  if (sort == '') {
    sort='StartDate';
  }
  Recordings.sortBy = sort;

  var order = Recordings.getURLParam('sortOrder');
  if (order == '') {
    order='Descending';
  }
  Recordings.sortOrder = order;

  var psize = Recordings.getURLParam('pageSize');
  if (psize == '') {
    psize = 10;
  }
  ocPager.pageSize = psize;
  ocPager.init();

  var show = Recordings.getURLParam('show');
  if (show == '') {
    show='upcoming';
  }
  Recordings.currentState = show;
  Recordings.displayRecordingStats();

  if (Recordings.currentState == "upcoming" || Recordings.currentState == "finished") {
    $('#info-box').css("display", "block");
    $("#table-info-box-"+Recordings.currentState).css("display","block");
  }

  // init update interval for recording stats
  Recordings.statsInterval = window.setInterval( 'Recordings.displayRecordingStats();', 3000 );
  if (show == 'all' || show == 'capturing' || show == 'processing') {
    $('#refresh-controls-container').css('display','block');
    if ($('#refresh-enabled').is(':visible') && $('#refresh-enabled').is(':checked')) {
      $('.refresh-text').removeClass('refresh-text-disabled').addClass('refresh-text-enabled');
      Recordings.initTableRefresh($('#refresh-interval').val());
    }
  } else {
    $('#refresh-controls-container').css('display','none');
  }
}

/** (re-)initialize reloading of recordings table
 *
 */
Recordings.initTableRefresh = function(time) {
  Recordings.tableInterval = window.setInterval('Recordings.displayRecordings("' + Recordings.currentState + '",true);', time*1000);
}

/** get and display recording statistics. If the number of recordings in the
 * currently displayed state changes, the table is updated via displayRecordings().
 */
Recordings.displayRecordingStats = function() {
  if (!Recordings.updateRequested) {
    Recordings.updateRequested = true;
    $.ajax({
      url: "rest/countRecordings",
      type: "GET",
      cache: false,
      success: function(data) {
        Recordings.updateRequested = false;
        for (key in data) {
          if (Recordings.currentState == key) {
            if (Recordings.lastCount !== data[key]) {
              Recordings.lastCount = data[key];
              ocPager.update(ocPager.pageSize, ocPager.currentPageIdx);
              Recordings.displayRecordings(Recordings.currentState, true);
            } else {
              Recordings.lastCount = data[key];
            }
          }
          var elm = $('#count-' + key);
          if (elm) {
            elm.text('(' + data[key] + ')');
          }
        }
      }
    });
  }
}

/** Request a list of recordings in a certain state and render the response as a table.
 *  While we are waiting for a response, a a little animation is displayed.
 */
Recordings.displayRecordings = function(state, reload) {
  if (!Recordings.tableUpdateRequested) {
    Recordings.tableUpdateRequested = true;
    Recordings.currentState = state;
    $('.state-selector').removeClass('state-selector-active');
    $('.selector-'+state).addClass('state-selector-active');
    if (!reload) {
      Recordings.injectLoadingAnimation($('#recordings-table-container'));
    }
    if (Recordings.currentXML == null) {
      $.ajax({
        type       : 'GET',
        url        : 'xsl/recordings_'+Recordings.currentState+'.xsl',
        cache      : false,
        dataType   : 'text',
        error      : function(XHR,status,e){
          //alert('Not able to load XSL for ' + Recordings.currentState);
        },
        success    : function(data) {
          Recordings.currentXSL = data;
          Recordings.loadRecordingsXML();
        }
      });
    } else {
      Recordings.loadRecordingsXML();
    }
  }
}

Recordings.loadRecordingsXML = function() {
  var page = ocPager.currentPageIdx;
  var psize = ocPager.pageSize;
  var sort = Recordings.sortBy;
  var order = Recordings.sortOrder;
  var url = "rest/recordings/"+Recordings.currentState+"?ps="+psize+"&pn="+page+"&sb="+sort+"&so="+order;
  $.ajax({
    type       : 'GET',
    url        : url,
    cache      : false,
    dataType   : 'text',
    error      : function(XHR,status,e){
      //alert('Not able to load recordings list for state ' + Recordings.currentState);
    },
    success    : function(data) {
      Recordings.renderTable(data, Recordings.currentXSL);
    }
  });
}

Recordings.renderTable = function(xml, xsl) {
  $('#recordings-table-container').xslt(xml, xsl, function() {
    Recordings.tableUpdateRequested = false;
    // prepare table heads
    $('.recording-Table-head').removeClass('sortable-Ascending').removeClass('sortable-Descending');
    $('#th-'+Recordings.sortBy).addClass('sortable-'+Recordings.sortOrder);
    $('.recording-Table-head').click(function(){
      Recordings.sortBy = $(this).attr('id').substr(3);
      if ($(this).is('.sortable-Descending')) {
        Recordings.sortOrder = 'Ascending';
      } else {
        Recordings.sortOrder = 'Descending';
      }
      ocPager.currentPageIdx = 0;
      Recordings.displayRecordings(Recordings.currentState, true);
    });
    // format dates
    if ($('.date-column').length > 0) {
      // if date date/time column is present
      $('.td-TimeDate').each( function() {     // format date/time
        var startTime = $(this).children(".date-start").text();
        var endTime = $(this).children(".date-end").text();
        //alert(startTime + " - " + endTime);
        if (startTime) {
          var sd = new Date();
          sd.setTime(startTime);

          var sday  = sd.getDate();
          var smon  = sd.getMonth()+1;

          if (sday < 10) sday = "0" + sday;
          if (smon < 10) smon = "0" + smon;

          startTime = sd.getFullYear() + '-' + smon + '-' + sday + ' ' + sd.getHours() + ':' + Recordings.ensureTwoDigits(sd.getMinutes());
        } else {
          startTime = "NA";
        }
        if (endTime) {
          var ed = new Date();
          ed.setTime(endTime);
          endTime = ' - ' + ed.getHours() + ':' + Recordings.ensureTwoDigits(ed.getMinutes());
        } else {
          endTime = "";
        }
        $(this).append($(document.createElement('span')).text(startTime + endTime));
      });
    }
    //header underline
    $(".header").hover(function(){
      $(this).css('text-decoration', 'underline');
    }, function(){
      $(this).css('text-decoration', 'none')
    });
  });
}

Recordings.ensureTwoDigits = function(number) {
  if (number < 10) {
    return '0' + number;
  } else {
    return number;
  }
}

/** convert timestamp to locale date string
 * @param timestamp
 * @return Strng localized String representation of timestamp
 */
Recordings.makeLocaleDateString = function(timestamp) {
  var date = new Date();
  date.setTime(timestamp);
  return date.toLocaleString();
}

/** inject a 'loading' animation in the specified element
 * @param elm elmement into which the animation should be injected
 */
Recordings.injectLoadingAnimation = function(elm) {
  var anim = document.createElement('div');
  $(anim).addClass('loadingAnimation');
  $(elm).empty().append(anim);
}

/** Get URL parameter
 * @param name key in URL parameters
 * @return String value for the first occurance of the key or empty string if key was not found
 */
Recordings.getURLParam = function(name) {
  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
  var regexS = "[\\?&]"+name+"=([^&#]*)";
  var regex = new RegExp( regexS );
  var results = regex.exec( window.location.href );
  if( results == null )
    return "";
  else
    return results[1];
}

/** Displays Hold Operation UI
 * @param URL of the hold action UI
 * @param wfId Id of the hold operations workflow
 * @param callerElm HTML element that invoked the UI (so that information from the recordings table row can be gathered
 */
Recordings.displayHoldActionPanel = function(URL, wfId, callerElm) {
  $('#holdActionPanel-container iframe').attr('src', URL);
  $('#holdWorkflowId').val(wfId);
  var parentRow = $(callerElm).parent().parent();
  $('#holdStateHeadRow-title').html($($(parentRow).children().get(0)).html());
  $('#holdStateHeadRow-presenter').html($($(parentRow).children().get(1)).html());
  $('#holdStateHeadRow-series').html($($(parentRow).children().get(2)).html());
  $('#holdStateHeadRow-date').html($($(parentRow).children().get(3)).html());
  $('#holdStateHeadRow-status').html($($(parentRow).children().get(4)).html());
  $('#holdActionPanel-container').toggle();
  $('#recordings-table-container').hide();
  $('#category-selector-container').parent().hide();
  $('#oc_recordingmenu').hide();
  $('.paging-nav-container').hide();
  $('#refresh-controls-container').hide();
}

/** Adjusts the height of the panel holding the Hold Operation UI
 *
 */
Recordings.adjustHoldActionPanelHeight = function() {
  var height = $("#holdActionPanel-iframe").contents().find("html").height();
  $('#holdActionPanel-iframe').height(height+10);
//alert("Hold action panel height: " + height);
}

/** Calls workflow endpoint to end hold operation and continue the workflow
 *
 */
Recordings.continueWorkflow = function(postData) {
  var workflowId = $('#holdWorkflowId').val();
  if(postData===null) {
    postData = {id : workflowId};
  }
  if (Recordings.changedMediaPackage != null) {
    postData['mediapackage'] = Recordings.changedMediaPackage;
    Recordings.changedMediaPackage = null;
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
      $('#holdActionPanel-container').toggle();
      $('#recordings-table-container').toggle();
      $('#oc_recordingmenu').toggle();
      $('.paging-nav-container').toggle();
      $('#refresh-controls-container').toggle();
      location.reload();
    }
  });
}

/** Show the recording editor
 *
 */
Recordings.retryRecording = function(workflowId) {
  location.href = "upload.html?retry=" + workflowId;
}

Recordings.removeRecording = function(workflowId) {
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
      location.reload();
    }
  });
}

Recordings.removeScheduledEvent = function(eventId, title) {
  if(confirm('Are you sure you wish to delete ' + title + '?')){
    $.ajax({
      url        : '/scheduler/rest/event/'+eventId,
      type       : 'DELETE',
      error      : function(XHR,status,e){
        alert('Could not remove Scheduler Event ' + workflowId);
      },
      success    : function(data) {
        location.reload();
      }
    });
  }
}
