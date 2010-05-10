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
  // Event: clicked somewhere
//  $('body').click( function() {
//    $('#holdActionPanel-container').fadeOut('fast');
//  });

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
  })

  // register custom table cell value parser for Date/Time column
  // deactivated (for the moment?)
  $.tablesorter.addParser({
    id: 'date',
    is: function(){
      return false;
    },
    format: function(s) {
      var elm = document.createElement('div');
      elm.innerHTML = s;
      return $(elm).children('.time-raw').text();
    },
    type: 'numeric'
  });
  
  $.tablesorter.addParser({
    id: 'lastname',
    is: function(){return false;},
    format: function(s) {
      var ln = s.split(' ');
      if(ln.length != 0){
        return ln[ln.length - 1];
      }else{
        return "";
      }
    },
    type: 'text'
  });

  // request and display statistics
  Recordings.displayRecordingStats();

  // init update interval for recording stats
  Recordings.statsInterval = window.setInterval( 'Recordings.displayRecordingStats();', 3000 );

  var show = '';
  show = Recordings.getURLParam('show');
  if (show == '') {
    show='upcoming';
  }
  Recordings.displayRecordings(show);

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
            if ((Recordings.lastCount !== null) && (Recordings.lastCount !== data[key])) {
              Recordings.displayRecordings(Recordings.currentState, true);
            }
            Recordings.lastCount = data[key];
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
  Recordings.lastCount = null;
  $('.state-selector').removeClass('state-selector-active');
  $('.selector-'+state).addClass('state-selector-active');
  if (!reload) {
    Recordings.injectLoadingAnimation($('#recordings-table-container'));
  }
  $('#recordings-table-container').xslt("rest/recordings/"+state, "xsl/recordings_"+state+".xsl", function() {
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
      $('.date-start').each( function() {     // format date/time to locale string
        var ts = $(this).children('.time-raw').text();
        var time = document.createElement('span');
        if (ts != '') {
          $(time).css('color','black').text(Recordings.makeLocaleDateString(ts));
        } else {
          $(time).css('color','gray').text('NA');  // display a gray 'NA' if no timestamp available
        }
        $(this).append(time);
      });
      $('#recordingsTable').tablesorter({   // init tablesorter with custom parser for the date column
        cssAsc: 'sortable-asc',
        cssDesc: 'sortable-desc',
        sortList: [[3,0]],
        headers: {
          1: {sorter: 'lastname'},
          3: {sorter: 'date'}
        }
      });
    } else {  // if no date/time column is present, init tablesorter the default way
      $('#recordingsTable').tablesorter({
        cssAsc: 'sortable-asc',
        cssDesc: 'sortable-desc',
        headers: {
          1: {sorter: 'lastname'}
        }
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

Recordings.displayHoldActionPanel = function(URL, wfId, callerElm) {
  $('#holdActionPanel-container iframe').attr('src', URL);
  $('#holdWorkflowId').val(wfId);
  var parentRow = $(callerElm).parent().parent();
  $('#holdActionPanel-container').fadeIn('fast');
  $('#holdActionPanel-container').offset($(parentRow).offset());
  $('#holdActionPanel-container').width($(parentRow).outerWidth()-2);
}

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

Recordings.retryRecording = function(workflowId) {
  // TDOD call the workflow rest endpoint to start the workflow again
}

Recordings.editRetryRecording = function(workflowId) {
  location.href = "retry.html?id=" + workflowId;
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