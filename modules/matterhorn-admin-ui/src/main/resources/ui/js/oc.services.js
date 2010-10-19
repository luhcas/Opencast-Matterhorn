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
  jQuery.each(data.statistics.service, function(index, serviceInstance) {
    var reg = serviceInstance.serviceRegistration;
    var server = servers[reg.host];
    if(server == null) {
      server = {'host' : reg.host};
      servers[reg.host] = server;
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
    singleService.online = reg.online;
    singleService.maintenance = reg.maintenance;
    singleService.running = serviceInstance.@running;
    singleService.meanRunTime = serviceInstance.@meanruntime;
    singleService.queued = serviceInstance.@queued;
    singleService.meanQueueTime = serviceInstance.@meanqueuetime;
  });
}

/**
 * Builds the "services view" js model.
 */
Opencast.Services.buildServicesView = function(data) {
  var services = Opencast.Services.servicesView;
  jQuery.each(data.statistics.service, function(index, serviceInstance) {
    var reg = serviceInstance.serviceRegistration;

    // if the service is not a job producer, we don't show it here
    if(!reg.jobproducer) return true;

    var serviceTypeIdentifier = reg.type.replace(/\./g, "_");
    var service = services[serviceTypeIdentifier];
    if(service == null) {
      service = {'id' : serviceTypeIdentifier, 'online' : reg.online, 'maintenance' : reg.maintenance};
      service.servers = [];
      services[serviceTypeIdentifier] = service;
    }

    // Add the server to this service
    var singleService = {};
    service.servers.push(singleService);
    singleService.host = reg.host;
    singleService.running = serviceInstance.@running;
    singleService.meanRunTime = serviceInstance.@meanruntime;
    singleService.queued = serviceInstance.@queued;
    singleService.meanQueueTime = serviceInstance.@meanqueuetime;
  });
}

/**
 * The labels for the UI.  TODO: i18n
 */
Opencast.Services.labels = {
    "org_opencastproject_caption"                : "Captioning",
    "org_opencastproject_analysis_text"          : "Text analysis",
    "org_opencastproject_analysis_segmenter"     : "Video segmentation",
    "org_opencastproject_composer"               : "Encoding, image extraction, and trimming",
    "org_opencastproject_distribution_download"  : "Media distribution (local downloads)",
    "org_opencastproject_distribution_streaming" : "Media distribution (local streaming)",
    "org_opencastproject_distribution_itunesu"   : "Media distribution (iTunes)",
    "org_opencastproject_distribution_youtube"   : "Media distribution (YouTube)",
    "org_opencastproject_inspection"             : "Media inspection"
}