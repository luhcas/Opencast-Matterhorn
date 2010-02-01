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

//eventDoc contains a reference to the XML document which is built through various AJAX callbacks.
var eventDoc = null;
//WorkflowID is parsed from the URL parameters and is the ID of the workflow used to gather the required metadata.
var workflowID = null;

/**
 *  handleWorkflow parses the workflow xml, grabs the URLs of the additional metadata catalogs
 *  and makes additional ajax calls to bring in the metadata.
 *
 *  @param {XML Document}
 */
function handleWorkflow(workflowDoc){
  eventDoc = createDoc();
  var dcURL = "";
  var rootEl = null;
  if(workflowDoc.documentElement){
    rootEl = $(workflowDoc.documentElement);
  }else{
    rootEl = $("ns2\\:workflow-instance");
  }
  if(rootEl){
    dcURL = rootEl.find("metadata:first > catalog[type='metadata/dublincore'] url").text();
    //TODO: get extra metadata catalog URL
    //var caURL = $("ns2\\:workflow-instance", testDoc).find("metadata:first > catalog[type='metadata/extra'] url").text();
    
    eventid = eventDoc.createElement('event-id');
    eventid.appendChild(eventDoc.createTextNode(rootEl.attr('id')));
    eventDoc.documentElement.appendChild(eventid);
    
    startdate = eventDoc.createElement('startdate');
    start = parseDateTime(rootEl.find('mediapackage').attr('start'));
    startdate.appendChild(eventDoc.createTextNode(start));
    
    eventDoc.documentElement.appendChild(startdate);
    
    duration = eventDoc.createElement('duration');
    duration.appendChild(eventDoc.createTextNode(parseDuration(parseInt(rootEl.find('mediapackage').attr('duration')))));
    eventDoc.documentElement.appendChild(duration);

    var track = rootEl.find("media>track[type='presentation/source']");
    if (track) {
      var filename = track.find("url").text().split(/\//);
      filename = filename[filename.length - 1];
      var elm = eventDoc.createElement('filename');
      elm.appendChild(eventDoc.createTextNode(filename));
      eventDoc.documentElement.appendChild(elm);
    }

    if(dcURL){
      $.get(dcURL, handleDCMetadata);
    }else{
      throw "Unable to find DC Metadata URI";
    }
  }else{
    throw "Unable to parse workflow.";
  }
  //TODO: Get the extra metadata catalog, we need the Agent's id and the inputs selected.
  //$.get(caURL, handleCatalogMetadata);
}

/**
 *  handleDCMetadata handles the parsing of Dublin Core metadata catalog, before transforming and appending
 *  the data to the page.
 *  
 *  @param {XML Document}
 */
function handleDCMetadata(metadataDoc){
  var fields = ['title', 'creator', 'contributor', 'description', 'isPartOf', 'license', 'language', 'subject'];
  //TODO: This is a fast to code, but poor method of loading our values. Refactor.
  for( var i in fields ){
    var field = fields[i];
    el = eventDoc.createElement(field);
    if(metadataDoc.getElementsByTagNameNS){
      nodeTxt = $(metadataDoc.getElementsByTagNameNS("*",field)[0]).text();
    } else {
      nodeTxt = $("dcterms\\:" + field, metadataDoc).text();
    }
    el.appendChild(eventDoc.createTextNode(nodeTxt));
    eventDoc.documentElement.appendChild(el);
  }
  
  //Hopefully we've loaded an xml document with the values we want, transform and append this.
  doc = serialize(eventDoc);
  if(doc){
    $('#stage').xslt(doc, "xsl/viewevent.xsl", callback);
  }
}

function handleCatalogMetadata(metadataDoc){
  inputs = eventDoc.createElement('inputs');
  inputs.appendChild(eventDoc.createTextNode($(metadataDoc.documentElement).attr('id')));
  eventDoc.documentElement.appendChild(inputs);
  
  agent = eventDoc.createElement('agent');
  agent.appendChild(eventDoc.createTextNode($(metadataDoc.documentElement).attr('id')));
  eventDoc.documentElement.appendChild(agent);
}

/**
 *  callback is called after view metadata is transformed and attached to the page.
 *  it does some display setup.
 */
function callback(){
  $('.folder-head').click(
                          function(){
                          $(this).children('.fl-icon').toggleClass('icon-arrow-right');
                          $(this).children('.fl-icon').toggleClass('icon-arrow-down');
                          $(this).next().toggle('fast');
                          return false;
                          }
                          );
  
}

/**
 *  Returns a new xml document for FF or IE
 *
 *  @return {XML Document}
 */
function createDoc(){
  var doc = null;
  //Create a DOM Document, methods vary between browsers, e.g. IE and Firefox
  if(document.implementation && document.implementation.createDocument){ //Firefox, Opera, Safari, Chrome, etc.
    doc = document.implementation.createDocument("", "event", null);
  }else{ // IE
    doc = new ActiveXObject('MSXML2.DOMDocument');
    doc.loadXML('<event></event>');
  }
  return doc;
}

/**
 *  Returns a string of a serialized XML Document
 *
 *  @param {XML Document}
 *  @return {String}
 */
function serialize(doc){
  if(typeof XMLSerializer != 'undefined'){
    return (new XMLSerializer()).serializeToString(doc);
  }else if(doc.xml){
    return doc.xml;
  }
  return false;
}

function parseDuration(dur){
  dur = dur / 1000;
  var hours = Math.floor(dur / 3600);
  var min   = Math.floor( ( dur /60 ) % 60 );
  return hours + " hours, " + min + " minutes";
}

function parseDateTime(datetime){
  if(datetime){
    //expects date in ical format of YYYY-MM-DDTHH:MM:SS e.g. 2007-12-05T13:40:00
    datetime = datetime.split("T");
    var date = datetime[0].split("-");
    var time = datetime[1].split(":");
    return new Date(date[0], (date[1] - 1), date[2], time[0], time[1], time[2]).toLocaleString();
  }
  return ""
}
