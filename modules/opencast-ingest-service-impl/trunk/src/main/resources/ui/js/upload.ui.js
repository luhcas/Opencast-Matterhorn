var uploadUI = uploadUI || { };

/* Warns the user that something is not right.
 *
 */
uploadUI.warn = function(message) {
    alert(message);
    // TODO: display error message
}

uploadUI.showProgressOverlay = function() {
    UI.$("grayOut").style.display = "block";
    UI.$("uploadProgressOverlay").style.left = (window.innerWidth / 2) - 214;
    UI.$("uploadProgressOverlay").style.top = (window.innerHeight / 2) - 50;
    UI.$("uploadProgressOverlay").style.display = "inline-block";
}

uploadUI.setProgressBar = function(percentage) {
    percentage = percentage + "%";
    UI.$("progressIndicator").style.width = percentage;
    UI.$("progressLabel").innerHTML = percentage;
}

uploadUI.setFilename = function(name) {
    UI.$("filename").innerHTML = name;
    UI.$("filename").style.color = "black";
}

uploadUI.resetFilename = function() {
    UI.$("filename").innerHTML = "No file selected";
    UI.$("filename").style.color = "gray";
}

uploadUI.hideProgressOverlay = function() {
    UI.$("grayOut").style.display = "none";
    UI.$("uploadProgressOverlay").style.display = "none";
}

uploadUI.log = function(message) {
    debugConsole = UI.$("SWFUpload_Console");
    
    if (debugConsole) {
        debugConsole.value += "UI  DEBUG: " + message + "\n";
        //debugConsole.scrollTop = console.scrollHeight;
    }
}

/* Implementing some prototypeish stuff here to make life easyer
 * TODO: make this a separate js lib later
 */

var UI = UI || { };

UI.load = function(uiname, elementname) {
    $("#"+elementname).load(uiname+".html");
}

UI.$ = function(element) {
    return document.getElementById(element);
}
