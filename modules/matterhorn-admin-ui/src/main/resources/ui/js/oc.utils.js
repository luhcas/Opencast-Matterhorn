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
var ocUtils = ocUtils || {};

ocUtils.templateRoot = "jst/";

ocUtils.internationalize = function(obj, prefix){
  for(var i in obj){
    if(typeof obj[i] == 'object'){
      ocUtils.internationalize(obj[i], prefix + '_' + i);
    }else if(typeof obj[i] == 'string'){
      var id = '#' + prefix + '_' + i;
      if($(id).length){
        $(id).text(obj[i]);
      }
    }
  }
}

ocUtils.getURLParam = function(name) {
  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
  var regexS = "[\\?&]"+name+"=([^&#]*)";
  var regex = new RegExp( regexS );
  var results = regex.exec( window.location.href );
  if( results == null ) {
    return "";
  } else {
    return results[1];
  }
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

ocUtils.toICalDate = function(d){
  if(d.constructor !== Date){
    d = new Date(0);
  }
  var month = UI.padstring(d.getUTCMonth() + 1, '0', 2);
  var hours = UI.padstring(d.getUTCHours(), '0', 2);
  var minutes = UI.padstring(d.getUTCMinutes(), '0', 2);
  var seconds = UI.padstring(d.getUTCSeconds(), '0', 2);
  return '' + d.getUTCFullYear() + month + d.getUTCDate() + 'T' + hours + minutes + seconds + 'Z';
}

/** convert timestamp to locale date string
 * @param timestamp
 * @return Strng localized String representation of timestamp
 */
ocUtils.makeLocaleDateString = function(timestamp) {
  var date = new Date();
  date.setTime(timestamp);
  return date.toLocaleString();
}

ocUtils.fromUTCDateString = function(UTCDate) {
  var date = new Date(0);
  if(UTCDate[UTCDate.length - 1] === "Z") {
    var dateTime = UTCDate.slice(0,-1).split("T");
    var ymd = dateTime[0].split("-");
    var hms = dateTime[1].split(":");
    date.setUTCFullYear(ymd[0], parseInt(ymd[1]) - 1, ymd[2]);
    date.setUTCHours(hms[0], hms[1], hms[2]);
  }
  return date;
}

ocUtils.padString = function(str, pad, padlen){
  if(typeof str !== 'string'){ 
    str = str.toString();
  }
  while(str.length < padlen && pad.length > 0){
    str = pad + str;
  }
  return str;
}

ocUtils.log = function(){
  if(window.console){
    try{
      window.console && console.log.apply(console,Array.prototype.slice.call(arguments));
    }catch(e){
      console.log(e);
    }
  }
}

/** loads and prepare a JST template
 *  @param name of tempalte
 *  @param callback 
 */
ocUtils.getTemplate = function(name, callback) {
  var reqUrl = ocUtils.templateRoot + name + '.jst';
  $.ajax( {
    url : reqUrl,
    type : 'get',
    dataType : 'text',
    error : function(xhr) {
      ocUtils.log('Error: Could not get template ' + name + ' from ' + reqUrl);
      ocUtils.log(xhr.status + ' ' + xhr.responseText);
    },
    success : function(data) {
      var template = TrimPath.parseTemplate(data);
      callback(template);
    }
  });
}

/** If obj is an array just returns obj else returns Array with obj as content.
 *  If obj === undefined returns empty Array.
 *
 */
ocUtils.ensureArray = function(obj) {
  if (obj === undefined) return [];
  if ($.isArray(obj)) {
    return obj;
  } else {
    return [obj];
  }
}