/**
 * @fileOverview Collection of function implementing the behavior of the UI
 * @name Upload UI
 */

var UI = UI || { };

UI.load = function(uiname, elementname) {
    $("#"+elementname).load(uiname+".html");
}

UI.$ = function(element) {
    return document.getElementById(element);
}

/**
 @namespace Holds all functions that implement the UIs behavior
*/
var uploadUI = uploadUI || { };

/** Warn the user that something is not right
 *  @param {string} message message to be displayed */
uploadUI.warn = function(message) {
    alert(message);
    // TODO: display error message
}

/** Grays out the whole UI and displays the upload progress overlay */
uploadUI.showProgressOverlay = function() {
    document.getElementById("grayOut").style.display = "block";
    document.getElementById("uploadProgressOverlay").style.display = "block";
}

/** Updates the progress bars indicator and label
 *  @param {integer} percentage percentage to set the progress bar to */
uploadUI.setProgressBar = function(percentage) {
    percentage = percentage.toFixed(0) + "%";
    document.getElementById("progressIndicator").style.width = percentage;
    document.getElementById("progressLabel").innerHTML = percentage;
}

/** Displays the filename in the upload form
 *  @param {string} filename filename to be displayed in the upload form */
uploadUI.setFilename = function(name) {
    document.getElementById("filename").innerHTML = name;
    document.getElementById("filename").style.color = "black";
}

/** Resets the filename display in the upload form */
uploadUI.resetFilename = function() {
    document.getElementById("filename").innerHTML = "No file selected";
    document.getElementById("filename").style.color = "gray";
}

/** Hides the upload progress overlay */
uploadUI.hideProgressOverlay = function() {
    document.getElementById("grayOut").style.display = "none";
    document.getElementById("uploadProgressOverlay").style.display = "none";
}

/** Shows the missing fileds notification */
uploadUI.displayMissingFields = function() {
    $('#missingFields-container').show('fast');
}

/** Hides the missing fields notification */
uploadUI.hideMissingFields = function() {
    $('#missingFields-container').hide('fast');
}

/** Sets the color attribute of an element in the page (necessary so that the label for the distXXX fields is also set)
 *  @param {string} id id of the element that should be changed
 *  @param {string} color new value for the elements css attribute */
uploadUI.setLabelColor = function(id, color) {
   var label;
   
   if (id.substr(0,4) == 'dist') id = "dist";
   label = document.getElementById('label-' + id);
   if (label) {
       label.style.color = color;
   }
}

/** Loads and displays the upload complete screen */
uploadUI.showUploadComplete = function() {
    $('#stage').load('upload_complete.html',
        function() {
            for (key in uploadManager.metadata) {
                if (uploadManager.metadata[key] != '') {
                    $('#data-' + key).find('.data-value').append(uploadManager.metadata[key]);
                    $('#data-' + key).css('display','block');
                }
            }
        }
    );
}

/** prints the log message in the swfUpload debug console if it is activeated
 *  @param {string} message message that should be printed in log */
uploadUI.log = function(message) {
    debugConsole = document.getElementById("SWFUpload_Console");
    
    if (debugConsole) {
        debugConsole.value += "UI  DEBUG: " + message + "\n";
        //debugConsole.scrollTop = console.scrollHeight;
    }
}

