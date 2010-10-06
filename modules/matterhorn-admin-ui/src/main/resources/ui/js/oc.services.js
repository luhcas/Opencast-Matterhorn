/**  JS Library for massaging the server-side service statistics to a format that's useful for display */
var Opencast = Opencast || {};
Opencast.Services = Opencast.Services || {};
Opencast.Services.serversView = {};
Opencast.Services.servicesView = {};

/**
 * Builds the "server view" js model.
 */
Opencast.Services.buildServersView = function(data) {
  var servers = Opencast.Services.serversView;
  jQuery.each(data, function(index, serviceInstance) {
    var server = servers[serviceInstance.host];
    if(server == null) {
      server = {'host' : serviceInstance.host, 'online' : serviceInstance.online, 'maintenance' : serviceInstance.maintenance};
      servers[serviceInstance.host] = server;
      server.services = [];
    }
    // Add the service type to this server
    var singleService = {};
    server.services.push(singleService);
    var serviceTypeIdentifier = serviceInstance.type.replace(/\./g, "_");
    singleService.id = serviceTypeIdentifier;
    singleService.running = serviceInstance.running;
    singleService.meanRunTime = serviceInstance.meanRunTime;
    singleService.queued = serviceInstance.queued;
    singleService.meanQueueTime = serviceInstance.meanQueueTime;
  });
}

/**
 * Builds the "services view" js model.
 */
Opencast.Services.buildServicesView = function(data) {
  var services = Opencast.Services.servicesView;
  jQuery.each(data, function(index, serviceInstance) {
    var serviceTypeIdentifier = serviceInstance.type.replace(/\./g, "_");
    var service = services[serviceTypeIdentifier];
    if(service == null) {
      service = {'id' : serviceTypeIdentifier, 'online' : serviceInstance.online, 'maintenance' : serviceInstance.maintenance};
      service.servers = [];
      services[serviceTypeIdentifier] = service;
    }
    // Add the server to this service
    var singleService = {};
    service.servers.push(singleService);
    singleService.host = serviceInstance.host;
    singleService.running = serviceInstance.running;
    singleService.meanRunTime = serviceInstance.meanRunTime;
    singleService.queued = serviceInstance.queued;
    singleService.meanQueueTime = serviceInstance.meanQueueTime;
  });
}

/**
 * The labels for the UI.  TODO: i18n
 */
Opencast.Services.labels = {
    "org_opencastproject_analysis_text"          : "Text analysis",
    "org_opencastproject_analysis_vsegmenter"    : "Video segmentation",
    "org_opencastproject_composer"               : "Encoding, image extraction, and trimming",
    "org_opencastproject_distribution_download"  : "Media distribution (local downloads)",
    "org_opencastproject_distribution_streaming" : "Media distribution (local streaming)",
    "org_opencastproject_distribution_itunesu"   : "Media distribution (iTunes)",
    "org_opencastproject_distribution_youtube"   : "Media distribution (YouTube)",
    "org_opencastproject_inspection"             : "Media inspection",
    "org_opencastproject_search"                 : "Search",
    "workingfiles"                               : "Working files repository"
}