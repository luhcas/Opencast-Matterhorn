var matterhornVersion = matterhornVersion || { };

matterhornVersion.versionServiceUrl = "/admin";

matterhornVersion.getCurrent = function(elm) {
  if(typeof(elm) == "string"){
    elm = "#" + elm;
  }
  $.get(this.versionServiceUrl + "/version.txt", function(data) { $(elm).text("current version " + data.version); });
}

matterhornVersion.getNewest = function(elm) {
  if(typeof(elm) == "string"){
    elm = "#" + elm;
  }
  $.get(this.versionServiceUrl + "/newest", function(data) { $(elm).text("latest version " + data.version); });
}

$(document).ready(function() {
  matterhornVersion.getCurrent("currentVersion");
  matterhornVersion.getNewest("newestVersion");
});