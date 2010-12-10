var ocRecordings = ocRecordings || {};

Recordings = new (function() {

  var WORKFLOW_LIST_URL = '../workflow/rest/instances.json';
  var WORKFLOW_INSTANCE_URL = '';
  var WORKFLOW_STATISTICS_URL = '';

  // components
  this.searchbox = null;
  this.pager = null;

  var refreshing = false;

  this.data = null;

  this.Configuration = new (function() {

    // default configuartion
    this.state = 'all';
    this.pageSize = 10;
    this.page = 1;
    this.refresh = 5000;

    // parse url parameters
    try {
      var p = document.location.href.split('?', 2)[1] || false;
      if (p !== false) {
        p = p.split('&');
        for (i in p) {
          var param = p[i].split('=');
          if (this[param[0]]) {
            this[param[0]] = unescape(param[1]);
          }
        }
      }
    } catch (e) {
      alert('Unable to parse url parameters:\n' + e.toString());
    }

    return this;
  })();

  function refresh() {
    if (!refreshing) {
      refreshing = true;
      var params = [];
      //params.push('count=' + Recordings.Configuration.pageSize);
      //params.push('startPage=' + Recordings.Configuration.page);
      var state = Recordings.Configuration.state;
      if (state == 'upcoming') {
        params.push('state=paused');
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
      params.push('jsonp=Recordings.render');
      var url = WORKFLOW_LIST_URL + '?' + params.join('&');
      $('<script></script>').attr('src', url).appendTo('head');
    }
  }

  function makeRecording(wf) {
    var rec = {       // TODO create prototype so we can write : new Rec() or sth. here
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

    // Creator(s)
    if (wf.mediapackage.creators)
      rec.creators = wf.mediapackage.creators.creator;    // TODO update when there can be more than one

    // Start Time
    if (wf.mediapackage.start) {
      var t = wf.mediapackage.start.split('T');
      rec.start = t[1] + ' ' + t[0];
    }

    // Status
    // current operation : search first operation with state that matches workflow state
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
        break;
      }
    }

    ocUtils.log('Workflow operation', wf.template);
    // Actions
    if(wf.template === 'scheduling') {
      rec.actions = ['view', 'edit', 'delete'];
    }else{
      rec.actions = ['view', 'delete'];
    }

    return rec;
  }

  function makeRenderData(data) {
    var tdata = {
      recording:[]
    };
    if (data.workflows.workflow) {
      if (data.workflows.workflow instanceof Array) {
        for (i in data.workflows.workflow) {
          tdata.recording.push(makeRecording(data.workflows.workflow[i]));
        }
      } else {
        tdata.recording.push(makeRecording(data.workflows.workflow));
      }
    }
    return tdata;
  }

  this.render = function(data) {
    $('#tableContainer').empty();
    $.tmpl( "table-all", makeRenderData(data) ).appendTo( "#tableContainer" );
    $('#recordingsTable th')
    .mouseenter( function() {
      $(this).addClass('ui-state-hover');
    })
    .mouseleave( function() {
      $(this).removeClass('ui-state-hover');
    })
    .click( function() {
      var sortDesc = $(this).find('.sort-icon').hasClass('ui-icon-circle-triangle-s');
      $( "#recordingsTable th .sort-icon" )
      .removeClass('ui-icon-circle-triangle-s')
      .removeClass('ui-icon-circle-triangle-n')
      .addClass('ui-icon-triangle-2-n-s');
      if (sortDesc) {
        $(this).find('.sort-icon').addClass('ui-icon-circle-triangle-n');
      } else {
        $(this).find('.sort-icon').addClass('ui-icon-circle-triangle-s');
      }
    });
    $('.status-column-cell').click(function() {
      $(this).find('.fold-icon')
      .toggleClass('ui-icon-triangle-1-e')
      .toggleClass('ui-icon-triangle-1-s');
      $(this).find('.status-column-operation-details').toggle('fast');
    })
  }

  this.reload = function() {
    var url = document.location.href.split('?', 2)[0];
    var pa = [];
    for (p in this.Configuration) {
      pa.push(p + '=' + escape(this.Configuration[p]));
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
    $('#pageSize').spinner();
    $('#autoUpdate').button();
    $('#updateInterval').spinner();

    // set up statistics update


    refresh();

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




// Useful stuff
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
      ocUtils.log(actions[i]);
      if(actions[i] === 'view') {
        links.push('<a href="view.html?id=' + id + '">View</a>');
      } else if(actions[i] === 'edit') {
        links.push('<a href="scheduler.html?eventId=' + id + '&edit=true">Edit</a>');
      } else if(actions[i] === 'delete') {
        links.push('<a href="javascript:Recordings.removeRecording(\'' + id + '\',\'' + title + '\')">Delete</a>');
      }
    }
    ocUtils.log(links.join('<br />\n'));
    return links.join('<br />\n');
  }

  this.getTime = function(timestamp) {
    var d = new Date();
    d.setTime(timestamp);
    return d.getHours() + ':' + d.getMinutes() + ':' + d.getSeconds();
  }
})();