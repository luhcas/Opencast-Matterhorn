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
      server = {'host' : serviceInstance.host};
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
      service = {'id' : serviceTypeIdentifier};
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
