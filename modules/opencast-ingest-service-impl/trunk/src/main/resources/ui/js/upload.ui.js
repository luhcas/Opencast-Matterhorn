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


var uploadUI = uploadUI || { };

/* Warns the user that something is not right.
 *
 */
uploadUI.warn = function(message) {
    alert(message);
    // TODO: display error message
}

uploadUI.showProgressOverlay = function() {
    document.getElementById("grayOut").style.display = "block";
    document.getElementById("uploadProgressOverlay").style.display = "block";
}

uploadUI.setProgressBar = function(percentage) {
    percentage = percentage + "%";
    document.getElementById("progressIndicator").style.width = percentage;
    document.getElementById("progressLabel").innerHTML = percentage;
}

uploadUI.setFilename = function(name) {
    document.getElementById("filename").innerHTML = name;
    document.getElementById("filename").style.color = "black";
}

uploadUI.resetFilename = function() {
    document.getElementById("filename").innerHTML = "No file selected";
    document.getElementById("filename").style.color = "gray";
}

uploadUI.hideProgressOverlay = function() {
    document.getElementById("grayOut").style.display = "none";
    document.getElementById("uploadProgressOverlay").style.display = "none";
}

uploadUI.displayMissingFields = function(fields) {
    $('#missingFields-container').show('fast');
}

uploadUI.hideMissingFields = function() {
    $('#missingFields-container').hide('fast');
}

uploadUI.setLabelColor = function(id, color) {
   var label;
   
   if (id.substr(0,4) == 'dist') id = "dist";
   label = document.getElementById('label-' + id);
   if (label) {
       label.style.color = color;
   }
}

uploadUI.showUploadComplete = function() {
    $('#stage').load('upload_complete.html',
        function() {
            for (key in uploadManager.metadata) {
                $('#data-' + key).find('.data-value').append(uploadManager.metadata[key]);
                $('#data-' + key).css('display','block');
            }
        }
    );
}

uploadUI.log = function(message) {
    debugConsole = document.getElementById("SWFUpload_Console");
    
    if (debugConsole) {
        debugConsole.value += "UI  DEBUG: " + message + "\n";
        //debugConsole.scrollTop = console.scrollHeight;
    }
}

