
var ocUpload = ocUpload || {};

ocUpload.createMediaPackage = function(callback,errorCallback) {
  $.ajax({
    url        : '../rest/createMediaPackage',
    type       : 'GET',
    dataType   : 'xml',
    error      : function(XHR,status,e){
      if (errorCallback) {
        errorCallback("error calling ../rest/ingest/createMediaPackage");
      }
    },
    success    : function(data, status) {
      callback(data);
    }
  });
}

ocUpload.createDublinCoreCatalog = function(data) {
  var dc = ocUtils.createDoc('dublincore','http://www.opencastproject.org/xsd/1.0/dublincore/');
  dc.documentElement.setAttribute('xmlns:dcterms','http://purl.org/dc/terms/');
  for (key in data) {
    var elm = dc.createElement('dcterms:' + key);
    elm.appendChild(dc.createTextNode(data[key]));    // FIXME get rid of xmlns="" attribute
    dc.documentElement.appendChild(elm);
  }
  return dc;
}

ocUpload.addCatalog = function(mediaPackage, dcCatalog, callback) {
  $('#dcsubmit #mediaPackage').text(ocUtils.xmlToString(mediaPackage));
  $('#dcsubmit #file').text(ocUtils.xmlToString(dcCatalog));

  // upload the form
  $('#dcSubmit').ajaxSubmit({
    url : '../rest/addCatalogFromFile',
    method: 'post',
    iframe : true,
    dataType : 'xml',
    success : function(responseXML,status) {
      callback(responseXML);
    }
  });
}

ocUpload.addTrack = function(mediaPackage, uploadForm, successCallback, beforeUploadCallback) {
  $(uploadForm).children('.mediaPackage').text(ocUtils.xmlToString(mediaPackage));
  $(uploadForm).ajaxSubmit({
    url : '../rest/addTrackMonitored',
    method: 'post',
    iframe : true,
    beforeSubmit : function() {
      var filename = $(uploadForm).children('#track').val();
      // IE gives full path, extract file name
      if (filename.lastIndexOf('\\') != -1) {
        filename = filename.substr(filename.lastIndexOf('\\')+1);
      }
      var mpId = mediaPackage.documentElement.getAttribute('id');
      beforeUploadCallback(mpId, filename);
    },
    dataType : 'xml',
    success : function(responseXML) {
      successCallback(responseXML);
    }
  });
}
