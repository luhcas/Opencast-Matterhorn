/**
 * @fileOverview Event hub for the Ingest UI
 * @name Ingest UI
 */

/**
 @namespace Holds all event handlers for the Ingest UI
*/
var uploadEvents = uploadEvents || { };

// ~~~~~~~~~~~~~~~~~~~~~~~ ui elements ~~~~~~~~~~~~~~~~~~~~~~~

/** fired when the value of a form field is changed */
uploadEvents.fieldChanged = function(field) {
   uploadUI.log("EVENT: " + "Value of filed " + field.id + " changed to: " + field.value);
   uploadUI.setLabelColor(field.id, 'black');
   uploadManager.checkUpload( ($('#missingFields-container').css('display') == 'block') );
   uploadManager.dataChanged = true;
}

/** fired when submit button is clicked */
uploadEvents.btnSubmit = function() {
    uploadUI.log("EVENT: " + "Submit button clicked");
    if (uploadManager.checkUpload(true)) {
        uploadManager.startUpload();
    } else {
        uploadUI.displayMissingFields();
    }
}

/** fired when clear button is clicked */
uploadEvents.btnClear = function() {
    uploadUI.log("EVENT: " + "Clear button clicked");
    uploadManager.resetUploader();
}

/** fired if cancel button in upload progress overlay is clicked */
uploadEvents.btnCancelUpload = function() {
    uploadUI.log("EVENT: " + "Submit button clicked");
    if (confirm(uploadManager.selectedFile.name + ' is still uploading.\nAre you sure you want to stop the upload?')) {
      uploadManager.cancelUpload();
    }
}

/*
uploadEvents.btnAnotherUpload = function() {
    uploadUI.log("EVENT: " + "'Upload another file' button clicked");
    $('#stage').load('upload.html', {}, function() {$(document).ready();});
}
*/

// ~~~~~~~~~~~~~~~~~~~~~~~ swfUpload events ~~~~~~~~~~~~~~~~~~~~~~~

/** fired when user has selected a file in the file dialog */
uploadEvents.fileSelected = function(file) {
    if (file.size > 2147483648) {
      alert('This system supports the uploading of files less then 2GB. The file you have selected is bigger then 2GB and cannot be uploaded.');
    } else {
      uploadManager.selectedFile = file;
      uploadUI.setFilename(file.name);
      var field = { id : 'file' };
      uploadEvents.fieldChanged(field);
    }
}

/** fired when swfUpload has started the upload */
uploadEvents.uploadStarted = function(file) {
    uploadUI.showProgressOverlay();
    uploadManager.uploading = true;
}

/** fired periodically when upload is in progress */
uploadEvents.uploadProgress = function(file, completed, total) {
    uploadUI.setProgressBar(completed / total * 100);
}

/** fired when file was successfully uploaded */
uploadEvents.uploadComplete = function(file) {
    uploadManager.uploader.destroy();
    uploadUI.hideProgressOverlay();
    uploadUI.showUploadComplete();
    uploadManager.uploading = false;
}

/** fired when an error occured during upload */
uploadEvents.uploadError = function(file, code, message) {
    uploadUI.hideProgressOverlay();
    uploadManager.uploading = false;
    alert('Error during upload: ' + code + "\n" + message);
}
