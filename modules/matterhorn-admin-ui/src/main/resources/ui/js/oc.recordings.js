Recordings = new (function() {

  var WORKFLOW_LIST_URL = '../workflow/rest/instances.json';          // URL of workflow instances list endpoint
  var WORKFLOW_INSTANCE_URL = '';                                     // URL of workflow instance endpoint
  var WORKFLOW_STATISTICS_URL = '../workflow/rest/statistics.json';   // URL of workflow instances statistics endpoint

  var STATISTICS_DELAY = 3000;     // time interval for statistics update

  var SORT_FIELDS = {
    'Title' : 'TITLE',
    'Presenter' : 'CREATOR',
    'Series' : 'SERIES_TITLE',
    'Date' : 'DATE_CREATED'
  }

  // components
  this.searchbox = null;
  this.pager = null;

  this.data = null;     // currently displayed recording data
  this.statistics = null;

  var refreshing = false;      // indicates if JSONP requesting recording data is in progress
  this.refreshingStats = false; // indicates if JSONP requesting statistics data is in progress

  /** Executed when directly when script is loaded: parses url parameters and
   *  returns the configuration object.
   */
  this.Configuration = new (function() {

    // default configuartion
    this.state = 'all';
    this.pageSize = 10;
    this.page = 1;
    this.refresh = 5000;
    this.sortField = null;
    this.sortOrder = null;

    // parse url parameters
    try {
      var p = document.location.href.split('?', 2)[1] || false;
      if (p !== false) {
        p = p.split('&');
        for (i in p) {
          var param = p[i].split('=');
          if (this[param[0]] !== undefined) {
            this[param[0]] = unescape(param[1]);
          }
        }
      }
    } catch (e) {
      alert('Unable to parse url parameters:\n' + e.toString());
    }

    return this;
  })();

  /** Initiate new JSONP call to workflow instances list endpoint
   */
  function refresh() {
    if (!refreshing) {
      refreshing = true;
      var params = [];
      //params.push('count=' + Recordings.Configuration.pageSize);
      //params.push('startPage=' + Recordings.Configuration.page);
      // 'state' to display
      var state = Recordings.Configuration.state;
      if (state == 'upcoming') {
        params.push('state=paused');
        params.push('state=running');
        params.push('op=schedule');
      }
      else if (state == 'capturing') {
        params.push('state=running');
        params.push('op=capture');
      }
      else if (state == 'processing') {
        params.push('state=running');
        params.push('op=-schedule');
        params.push('op=-capture');
      }
      else if (state == 'finished') {
        params.push('state=succeeded');
        params.push('op=-schedule');
        params.push('op=-capture');
      }
      else if (state == 'hold') {
        params.push('state=paused');
        params.push('op=-schedule');
        params.push('op=-capture');
      }
      else if (state == 'failed') {
        params.push('state=failed');
      }
      // sorting if specified
      if (Recordings.Configuration.sortField != null) {
        var sort = SORT_FIELDS[Recordings.Configuration.sortField];
        if (Recordings.Configuration.sortOrder == 'DESC') {
          sort += "_DESC";
        }
        params.push('sort=' + sort);
      }
      params.push('jsonp=?');
      var url = WORKFLOW_LIST_URL + '?' + params.join('&');
      $.ajax(
      {
        url: url,
        dataType: 'jsonp',
        jsonp: 'jsonp',
        success: function (data)
        {
          Recordings.render(data);
        }
      });
    }
  }

  function refreshStatistics() {
    if (!Recordings.refreshingStats) {
      Recordings.refreshingStats = true;
      $.ajax(
      {
        url: WORKFLOW_STATISTICS_URL,
        dataType: 'jsonp',
        jsonp: 'jsonp',
        success: Recordings.updateStatistics
      });
    }
  }

  /** JSPON callback for statistics data requests. Translate numbers delivered
   *   by the statistics endpoint:
   *
   *  - upcoming : definition=scheduling, state=running + state=paused + state=instantiated
   *  - capturing: definition=capturing, state=running
   *  - processing: definition:all other than scheduling,capture, state=running
   *  - finished: definition:all other than scheduling,capture, state=succeeded
   *  - on hold: definition:all other than scheduling,capture, state=paused
   *  - failed: from summary -> failed  (assuming that scheduling goes into
   *      FAILED when recording was not started, capture goes into FAILED when
   *      the capture error occured etc.)
   *  - all: sum of the above
   */
  this.updateStatistics = function(data) {
    Recordings.refreshingStats = false;
    var stats = {
      all: 0,
      upcoming:0,
      capturing:0,
      processing:0,
      finished:0,
      hold:0,
      failed:0
    };

    if (data.statistics.definitions.definition !== undefined) {
      if ($.isArray(data.statistics.definitions.definition)) {
        $.each(data.statistics.definitions.definition, function(index, definition) {
          addStatistics(definition, stats)
        });
      } else {
        addStatistics(data.statistics.definitions.definition, stats);
      }
    }
    
    stats.all = stats.upcoming + stats.capturing + stats.processing + stats.finished + stats.failed + stats.hold;
    if (Recordings.statistics != null
      && Recordings.statistics[Recordings.Configuration.state] != stats[Recordings.Configuration.state]) {
      refresh();
    }
    Recordings.statistics = stats;
    displayStatistics();
  }

  /** Called by updateStatistics to add the numbers form one definition statistic
   *  to the statistics summary
   */
  function addStatistics(definition, stats) {
    if (definition.id == 'scheduling') {
      stats.upcoming = parseInt(definition.running) + parseInt(definition.paused) + parseInt(definition.instantiated);
    } else if (definition.id == 'capture') {
      stats.capturing = parseInt(definition.running);
    } else {
      stats.processing += parseInt(definition.running);
      stats.finished += parseInt(definition.finished);
      stats.hold += parseInt(definition.paused);
      stats.failed += parseInt(definition.failed) + parseInt(definition.failing);
    }
  }

  function displayStatistics() {
    $.each(Recordings.statistics, function(key, value) {
      $('#stats-' + key).text(' (' + value + ')');
    });
  }

  /** Make an object representing a row in the recording table from a workflow
   *  instance object delivered by the workflow endpoint
   *
   *  TODO make this a constructor
   */
  function makeRecording(wf) {
    var rec = {       // TODO create prototype so we can write : new Rec()
      id: '',
      title: '',
      creators : [],
      start : '',
      end : '',
      operation : {
        state:'',
        heading : '',
        details : '',
        time: false,
        properties : false
      },
      error : false,
      actions : []
    };
    // Id
    rec.id = wf.id;
    
    // Title
    rec.title = wf.mediapackage.title;
    
    // Series id and title
    rec.series = wf.mediapackage.series;
    rec.seriesTitle = wf.mediapackage.seriestitle;

    // Creator(s)
    if (wf.mediapackage.creators) { // TODO update when there can be more than one
      rec.creators = wf.mediapackage.creators.creator;
    } 

    // Start Time
    if (wf.mediapackage.start) {
      var t = wf.mediapackage.start.split('T');
      rec.start = t[1] + ' ' + t[0];
    }

    // Status
    // current operation : search last operation with state that matches workflow state
    if(!$.isArray(wf.operations.operation)) {
    //If there is a single operation, then operation is an object, otherwise it's an array
      wf.operations = [wf.operations];
    }
    for (var j in wf.operations.operation) {
      if (wf.operations.operation[j].state == wf.state) {
        var op = wf.operations.operation[j];
        rec.operation.state = op.state;
        rec.operation.heading = op.id;
        rec.operation.details = op.description;
        if (op.configurations) {
          rec.operation.properties = [];
          for (var k in op.configurations.configuration) {
            var c = op.configurations.configuration[k];
            rec.operation.properties.push({
              key : c.key,
              value : c.$
            });
          }
        }
      }
    }

    // Actions
    if(rec.operation.heading === 'schedule') {
      rec.actions = ['view', 'edit', 'delete'];
    }else{
      rec.actions = ['view', 'delete'];
    }

    return rec;
  }

  /** Prepare data delivered by workflow instances list endpoint for template
   *  rendering.
   */
  function makeRenderData(data) {
    var tdata = {
      recording:[]
    };
    if (data.workflows.workflow) {
      if (data.workflows.workflow instanceof Array) {
        for (var i in data.workflows.workflow) {
          tdata.recording.push(makeRecording(data.workflows.workflow[i]));
        }
      } else {
        tdata.recording.push(makeRecording(data.workflows.workflow));
      }
    }
    return tdata;
  }

  /** JSONP callback for calls to the workflow instances list endpoint.
   */
  this.render = function(data) {
    refreshing = false;
    $('#tableContainer').empty();
    $.tmpl( "table-all", makeRenderData(data) ).appendTo( "#tableContainer" );

    // When table is ready, attach event handlers to its children
    $('#recordingsTable thead .sortable')
    .mouseenter( function() {
      $(this).addClass('ui-state-hover');
    })
    .mouseleave( function() {
      $(this).removeClass('ui-state-hover');
    })
    .click( function() {
      var sortDesc = $(this).find('.sort-icon').hasClass('ui-icon-circle-triangle-s');
      var sortField = ($(this).attr('id')).substr(4);
      $( "#recordingsTable th .sort-icon" )
      .removeClass('ui-icon-circle-triangle-s')
      .removeClass('ui-icon-circle-triangle-n')
      .addClass('ui-icon-triangle-2-n-s');
      if (sortDesc) {
        Recordings.Configuration.sortField = sortField;
        Recordings.Configuration.sortOrder = 'ASC';
        Recordings.reload();
      } else {
        Recordings.Configuration.sortField = sortField;
        Recordings.Configuration.sortOrder = 'DESC';
        Recordings.reload();
      }
    });
    // if results are sorted, display icon indicating sort order in respective table header cell
    if (Recordings.Configuration.sortField != null) {
      var th = $('#sort' + Recordings.Configuration.sortField);
      $(th).find('.sort-icon').removeClass('ui-icon-triangle-2-n-s');
      if (Recordings.Configuration.sortOrder == 'ASC') {
        $(th).find('.sort-icon').addClass('ui-icon-circle-triangle-n');
      } else if (Recordings.Configuration.sortOrder == 'DESC') {
        $(th).find('.sort-icon').addClass('ui-icon-circle-triangle-s');
      }
    }
    $('.status-column-cell').click(function() {
      $(this).find('.fold-icon')
      .toggleClass('ui-icon-triangle-1-e')
      .toggleClass('ui-icon-triangle-1-s');
      $(this).find('.status-column-operation-details').toggle('fast');
    })
  }

  /** Make the page reload with the currently set configuration
   */
  this.reload = function() {
    var url = document.location.href.split('?', 2)[0];
    var pa = [];
    for (p in this.Configuration) {
      if (this.Configuration[p] != null) {
        pa.push(p + '=' + escape(this.Configuration[p]));
      }
    }
    url += '?' + pa.join('&');
    document.location.href = url;
  }

  /** $(document).ready()
   *
   */
  this.init = function() {

    $.template( 'table-all', $('#tableTemplate').val() );
    $.template( 'operation', $('#operationTemplate').val() );
    $.template( 'errormessage', $('#errorTemplate').val() );

    // upload/schedule button
    $('#uploadButton').button({
      icons:{
        primary:'ui-icon-circle-plus'
      }
    })
    .click( function() {
      window.location.href = '../../admin/upload.html';
    });
    $('#scheduleButton').button({
      icons:{
        primary:'ui-icon-circle-plus'
      }
    })
    .click( function() {
      window.location.href = '../../admin/scheduler.html';
    });

    // recordings state selectors
    $( '#state-' +  Recordings.Configuration.state).attr('checked', true);
    $( '.state-filter-container' ).buttonset();
    $( '.state-filter-container input' ).click( function() {
      Recordings.Configuration.state = $(this).val();
      Recordings.reload();
    })

    // search box
    this.searchbox = $( '#searchBox' ).searchbox({
      search : function(text) {
        alert(text)
      }
    });

    // recordings table

    // pager
    this.pager = $( '#pagerBottom' ).spinner({
      decIcon : 'ui-icon-circle-triangle-w',
      incIcon : 'ui-icon-circle-triangle-e'
    });

    // button to open the config dialog
    $( '#configButton' ).button()
    .click( function() {
      $('#configDialog').dialog({
        title : 'Configure View',
        resizable: false,
        buttons : {
          Ok : function() {
            // TODO save settings
            $(this).dialog('close');
          }
        }
      });
    });

    // set up config dialog
    // FIXME doesn work really good (components look strange, size of dialog doesn't fit content
    $('#pageSize').spinner();
    $('#autoUpdate').button();
    $('#updateInterval').spinner();

    // set up statistics update
    refreshStatistics();
    window.setInterval(refreshStatistics, STATISTICS_DELAY);

    refresh();    // load and render data for currently set configuration

  };
  
  this.removeRecording = function(id, title) { //TODO Delete the scheduled event too. Don't just stop the workflow.
    if(confirm('Are you sure you wish to delete ' + title + '?')){
      $.ajax({
        url        : '../workflow/rest/stop',
        data       : {
          id: id
        },
        type       : 'POST',
        error      : function(XHR,status,e){
          alert('Could not remove Workflow ' + title);
        },
        success    : function(data) {
          Recordings.reload();
        }
      });
    }
  }
  
  $(document).ready(this.init);

  return this;
})();

// Useful stuff that is called from JSONP callbacks / within table templates
RenderUtils = new (function() {

  this.makeList = function(a) {
    try {
      return a.join(', ');
    } catch(e) {
      return '';
    }
  }

  this.getCurrentOperation = function(wf) {
    var current = null;
    for (i in wf.operations.operation) {
      var op = wf.operations.operation[i];

    }
  }

  this.makeActions = function(id, title, actions) {
    links = [];
    for(i in actions){
      if(actions[i] === 'view') {
        links.push('<a href="view.html?id=' + id + '">View</a>');
      } else if(actions[i] === 'edit') {
        links.push('<a href="scheduler.html?eventId=' + id + '&edit=true">Edit</a>');
      } else if(actions[i] === 'delete') {
        links.push('<a href="javascript:Recordings.removeRecording(\'' + id + '\',\'' + title + '\')">Delete</a>');
      }
    }
    return links.join(' \n');
  }

  this.getTime = function(timestamp) {
    var d = new Date();
    d.setTime(timestamp);
    return d.getHours() + ':' + d.getMinutes() + ':' + d.getSeconds();
  }
})();