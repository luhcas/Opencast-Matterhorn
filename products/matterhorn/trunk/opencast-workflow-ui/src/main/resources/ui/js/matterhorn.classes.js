/* This file contains javascript classes that wrap around the various
 * types of XML entities that are returned by the Matterhorn REST endpoints
 */


/*
 * MediaPackage
 */
function MediaPackage(xml) {
    this.root = xml;
}

MediaPackage.getCatalogs = function() {
    
}

/* TODO: During development it will become clear which of these methods are
 *       needed, they will be implemented when we them
 */
MediaPackage.prototype.setID = function (id) {}
MediaPackage.prototype.getID = function () {}
MediaPackage.prototype.setStart = function (start) {}
MediaPackage.prototype.getStart = function () {}
MediaPackage.prototype.setDuration = function (duration) {}
MediaPackage.prototype.getDuration = function () {}

/*
 * WorkflowDefinition
 */
function WorkflowDefinition(xml) {
    this.root = xml;
}

/* TODO implement getters and setters as needed */

WorkflowDefinition.prototype.getTitle = function() {
    elm = this.root.getElementsByTagName("title")[0];
    return elm.childNodes[0].nodeValue;
}

WorkflowDefinition.prototype.getDescription = function() {
    elm = this.root.getElementsByTagName("description")[0];
    return elm.childNodes[0].nodeValue;
}

/*
 * WorkflowInstance
 */
function WorkflowInstance(xml) {
    this.root = xml;
}

WorkflowInstance.prototype.setID = function(id) {
    this.root.setAttribute("id", id);
}

WorkflowInstance.prototype.getID = function() {
    return this.root.getAttribute("id");
}

WorkflowInstance.prototype.setState = function(state) {
    this.root.setAttribute("state", state);
}

WorkflowInstance.prototype.getState = function() {
    return this.root.getAttribute("state");
}

WorkflowInstance.prototype.setTitle = function(title) {
    elm = this.root.getElementsByTagName("title")[0];
    elm.deleteData();
    elm.appendData(title);      // (not supported by IE 5.5)
}

WorkflowInstance.prototype.getTitle = function() {
    elm = this.root.getElementsByTagName("title")[0];
    return elm.childNodes[0].nodeValue;
}

/* TODO to be implemented when needed */
WorkflowInstance.prototype.setDescription = function(description) {}
WorkflowInstance.prototype.getDescription = function() {}
WorkflowInstance.prototype.setProperty = function(key, value) {}
WorkflowInstance.prototype.getProperty = function(key) {}
WorkflowInstance.prototype.getProperties = function() {}
WorkflowInstance.prototype.setMediaPackage = function(mediaPackage) {}
WorkflowInstance.prototype.getMediaPackage = function() {}
