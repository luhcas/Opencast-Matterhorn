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
Recordings.lastCount = null;

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
    window.location.href = 'recordings.html?show='+state;
    return false;
  });

  // register custom table cell value parser for Date/Time column
  $.tablesorter.addParser({
    id: 'date',
    is: function(){
      return false;
    },
    format: function(s) {
      return s; // FIXME don't need a parser here anymore it seems'
    },
    type: 'numeric'
  });

  var show = '';
  show = Recordings.getURLParam('show');
  if (show == '') {
    show='upcoming';
  }
  Recordings.currentState = show;

  ocPager.init();
  Recordings.displayRecordingStats();

  // init update interval for recording stats
  Recordings.statsInterval = window.setInterval( 'Recordings.displayRecordingStats();', 3000 );

}

/** get and display recording statistics. If the number of recordings in the
 * currently displayed state changes, the table is updated via displayRecordings().
 */
Recordings.displayRecordingStats = function() {
  if (!Recordings.updateRequested) {
    Recordings.updateRequested = true;
    $.getJSON("rest/countRecordings",
      function(data) {
        Recordings.updateRequested = false;
        for (key in data) {
          if (Recordings.currentState == key) {
            if (Recordings.lastCount !== data[key]) {
              //alert(Recordings.lastCount + " " + data[key])
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
      });
  }
}

/** Request a list of recordings in a certain state and render the response as a table.
 *  The table cols are made sortable via the JQuery Tablesorter plugin.
 *  While we are waiting for a response, a a little animation is displayed.
 */
Recordings.displayRecordings = function(state, reload) {
  Recordings.currentState = state;
  $('.state-selector').removeClass('state-selector-active');
  $('.selector-'+state).addClass('state-selector-active');
  if (!reload) {        
    Recordings.injectLoadingAnimation($('#recordings-table-container'));
  }
  var page = ocPager.currentPageIdx;
  var psize = ocPager.pageSize;
  $('#recordings-table-container').xslt("rest/recordings/"+state+"?ps="+psize+"&pn="+page, "xsl/recordings_"+state+".xsl", function() {
    $('.processingStatus').each( function() {
      var items = $(this).text().split(';');
      for (key=0; key < items.length-1; key++) {
        var item = items[key];
        if (state == 'finished') {
          item = item.replace(/SUCCEEDED: /,'')
          .replace(/distribute_local/,'Distributed')
          .replace(/publish/,'Distributed');
        }
        item = item.replace(/SUCCEEDED/,'Succeeded')
        .replace(/FAILED/,'Failed')
        .replace(/RUNNING: /,'')
        .replace(/inspect/,'Inspecting media')
        .replace(/compose/,'Encoding media')
        .replace(/image/,'Creating cover image')
        .replace(/distribute_local/,'Distributing')
        .replace(/publish/,'Distributing');
        $(this).text(item);
        if ( items[key].match(/FAILED/) ) {
          return;
        }
      }
    /*for (key=0; key < items.length-1; key++) {
              $(this).empty();
              var item = items[key].split(':');
              if (item[0] == 'SUCCEEDED') {
                $(document.createElement('span')).addClass('icon icon-check').appendTo($(this));
              } else if (item[0] == 'RUNNING') {
                $(document.createElement('span')).addClass('icon icon-running').appendTo($(this));
              } else if ((item[0] == 'FAILED') || (item[0] == 'FAILING')) {
                $(document.createElement('span')).addClass('icon icon-error').appendTo($(this));
              }
              $(document.createElement('span')).css('margin-left','3px').text(item[1]).appendTo($(this));
              if ((item[0] == 'FAILED') || (item[0] == 'FAILING')) {
                return;
              }
            }*/
    });
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
      $('#recordingsTable').tablesorter({   // init tablesorter with custom parser for the date column
        cssAsc: 'sortable-asc',
        cssDesc: 'sortable-desc',
        sortList: [[3,0]],
        headers: {
          3: {
            sorter: 'date'
          }
        }
      });
    } else {  // if no date/time column is present, init tablesorter the default way
      $('#recordingsTable').tablesorter({
        cssAsc: 'sortable-asc',
        cssDesc: 'sortable-desc'
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
  var offset = $(parentRow).offset();
  $('#holdActionPanel-container').css('top', offset.top);
  $('#holdActionPanel-container').css('left', offset.left);
  $('#holdActionPanel-container').width($(parentRow).outerWidth()-2);
  $('#holdActionPanel-container').fadeIn('fast');
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
Recordings.continueWorkflow = function() {
  var workflowId = $('#holdWorkflowId').val();
  $.ajax({
    type       : 'POST',
    url        : '../workflow/rest/' + workflowId + '/resume',
    error      : function(XHR,status,e){
      alert('Could not remsume Workflow ' + status);
    },
    success    : function(data) {
      $('#holdActionPanel-container').fadeOut('fast');
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
    url        : '../workflow/rest/remove/'+workflowId,
    type       : 'GET',
    error      : function(XHR,status,e){
      alert('Could not remove Workflow ' + workflowId);
    },
    success    : function(data) {
      location.reload();
    }
  });
}
