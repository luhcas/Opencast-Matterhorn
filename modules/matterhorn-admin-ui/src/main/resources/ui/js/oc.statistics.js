ocStatistics = new (function() {

  var SERVERS_STATS_URL = '/services/statistics.json';           // URL of server and services statistics endpoint

  var STATISTICS_DELAY = 3000;     // time interval for statistics update

  this.serversView = {};
  
  this.servicesView = {};

  // components

  var refreshing = false;      // indicates if JSONP requesting recording data is in progress
  this.refreshingStats = false; // indicates if JSONP requesting statistics data is in progress

  /**
   * The labels for the UI.  TODO: i18n
   */
  this.labels = {
    "org_opencastproject_caption"                : "Captioning",
    "org_opencastproject_textanalyzer"           : "Text analysis",
    "org_opencastproject_videosegmenter"         : "Video segmentation",
    "org_opencastproject_composer"               : "Encoding, image extraction, and trimming",
    "org_opencastproject_distribution_download"  : "Media distribution (local downloads)",
    "org_opencastproject_distribution_streaming" : "Media distribution (local streaming)",
    "org_opencastproject_distribution_itunesu"   : "Media distribution (iTunes)",
    "org_opencastproject_distribution_youtube"   : "Media distribution (YouTube)",
    "org_opencastproject_inspection"             : "Media inspection",
    "org_opencastproject_workflow"               : "Workflow"
  };

  /** Executed when directly when script is loaded: parses url parameters and
   *  returns the configuration object.
   */
  this.Configuration = new (function() {

    // default configuartion
    this.state = 'servers';
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
      var url = SERVERS_STATS_URL;
      $.ajax(
      {
        url: url,
        dataType: 'jsonp',
        jsonp: 'jsonp',
        success: function (data)
        {
          ocStatistics.render(data);
        }
      });
    }
  }

  /** JSONP callback for calls to the workflow instances list endpoint.
   */
  this.render = function(data) {
    refreshing = false;
    var state = ocStatistics.Configuration.state;
    ocStatistics.buildServersView(data);
    ocStatistics.buildServicesView(data);
    var result = TrimPath.processDOMTemplate(state + "Template", ocStatistics);
    $("#tableContainer").html(result);
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
  
  /**
   * Builds the "server view" js model.
   */
  this.buildServersView = function(data) {
    $.each(data.statistics.service, function(index, serviceInstance) {
      var reg = serviceInstance.serviceRegistration;
      var server = ocStatistics.serversView[reg.host];
      if(server == null) {
        server = {'host' : reg.host, 'online' : reg.online, 'maintenance' : reg.maintenance};
        ocStatistics.serversView[reg.host] = server;
        server.services = [];
      }
      // if the service is not a job producer, we don't show it here
      if(!reg.jobproducer) return true;
  
      // Add the service type to this server
      var singleService = {};
      server.services.push(singleService);
      var serviceTypeIdentifier = reg.type.replace(/\./g, "_");
      singleService.id = serviceTypeIdentifier;
      singleService.path = reg.path;
      singleService.running = serviceInstance.running;
      singleService.meanRunTime = serviceInstance.meanruntime;
      singleService.queued = serviceInstance.queued;
      singleService.meanQueueTime = serviceInstance.meanqueuetime;
    });
  }
  
  /**
   * Builds the "services view" js model.
   */
  this.buildServicesView = function(data) {
    $.each(data.statistics.service, function(index, serviceInstance) {
      var reg = serviceInstance.serviceRegistration;
  
      // if the service is not a job producer, we don't show it here
      if(!reg.jobproducer) return true;
  
      var serviceTypeIdentifier = reg.type.replace(/\./g, "_");
      var service = ocStatistics.servicesView[serviceTypeIdentifier];
      if(service == null) {
        service = {'id' : serviceTypeIdentifier, 'online' : reg.online, 'maintenance' : reg.maintenance};
        service.servers = [];
        ocStatistics.servicesView[serviceTypeIdentifier] = service;
      }
  
      // Add the server to this service
      var singleServer = {};
      service.servers.push(singleServer);
      singleServer.host = reg.host;
      singleServer.running = serviceInstance.running;
      singleServer.meanRunTime = serviceInstance.meanruntime;
      singleServer.queued = serviceInstance.queued;
      singleServer.meanQueueTime = serviceInstance.meanqueuetime;
    });
  }

  /** $(document).ready()
 *
 */
  this.init = function() {

    // ocStatistics state selectors
    $( '#stats-' +  ocStatistics.Configuration.state).attr('checked', true);
    $( '.state-filter-container' ).buttonset();
    $( '.state-filter-container input' ).click( function() {
      ocStatistics.Configuration.state = $(this).val();
      ocStatistics.reload();
    })

    // set up ui update
    //window.setInterval(refresh, STATISTICS_DELAY);

    refresh();    // load and render data for currently set configuration

  };
  
  $(document).ready(this.init);

  return this;
})();
