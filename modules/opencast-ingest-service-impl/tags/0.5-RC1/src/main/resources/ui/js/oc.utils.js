var ocUtils = ocUtils || {};

ocUtils.formToMap = function(form) {
  out = {};
  for (var i=0; i < form.elements.length; i++) {
    var field = form.elements[i];
    if (field.value != '') {
      out[field.name] = field.value;
    }
  }
  return out;
}

ocUtils.mergeMaps = function(map1,map2) {
  for (key in map2) {
    map1[key] = map2[key];
  }
  return map1;
}

ocUtils.xmlToString = function(doc) {
  if(typeof XMLSerializer != 'undefined'){
    return (new XMLSerializer()).serializeToString(doc);
  } else if(doc.xml) {
    return doc.xml;
  } else {
    return '';
  }
}

ocUtils.createDoc = function(rootEl, rootNS){
  var doc = null;
  //Create a DOM Document, methods vary between browsers, e.g. IE and Firefox
  if(document.implementation && document.implementation.createDocument){ //Firefox, Opera, Safari, Chrome, etc.
    doc = document.implementation.createDocument(rootNS, rootEl, null);
  }else{ // IE
    doc = new ActiveXObject('MSXML2.DOMDocument');
    doc.loadXML('<' + rootEl + ' xmlns="' + rootNS + '"></' + rootEl + '>');
  }
  return doc;
}