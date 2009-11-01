var uploadEvents = uploadEvents || { };

// ~~~~~~~~~~~~~~~~~~~~~~~ ui elements ~~~~~~~~~~~~~~~~~~~~~~~

/* fired when the value of a form field is changed
 *
 */
uploadEvents.fieldChanged = function(field) {
   uploadUI.log("EVENT: " + "Value of filed " + field.id + " changed to: " + field.value);
   uploadUI.setLabelColor(field.id, 'black');
   uploadManager.checkUpload( ($('#missingFields-container').css('display') == 'block') );
}

uploadEvents.btnSubmit = function() {
    uploadUI.log("EVENT: " + "Submit button clicked");
    if (uploadManager.checkUpload(true)) {
        uploadManager.startUpload();
    } else {
        uploadUI.displayMissingFields();
    }
}

uploadEvents.btnClear = function() {
    uploadUI.log("EVENT: " + "Clear button clicked");
    uploadManager.resetUploader();
}

uploadEvents.btnCancelUpload = function() {
    uploadUI.log("EVENT: " + "Submit button clicked");
    uploadManager.cancelUpload();
}

uploadEvents.btnAnotherUpload = function() {
    uploadUI.log("EVENT: " + "'Upload another file' button clicked");
    $('#stage').load('upload.html', {}, function() {$(document).ready();});
}


// ~~~~~~~~~~~~~~~~~~~~~~~ swfUpload events ~~~~~~~~~~~~~~~~~~~~~~~

uploadEvents.fileSelected = function(file) {
    uploadManager.selectedFile = file;
    uploadUI.setFilename(file.name);
    var field = { id : 'file' };
    uploadEvents.fieldChanged(field);
}

uploadEvents.uploadStarted = function(file) {
    uploadUI.showProgressOverlay();
}

uploadEvents.uploadProgress = function(file, completed, total) {
    uploadUI.setProgressBar(completed / total * 100);
}

uploadEvents.uploadComplete = function(file) {
    uploadManager.uploader.destroy();
    uploadUI.hideProgressOverlay();
    uploadUI.showUploadComplete();
}

uploadEvents.uploadError = function(file, code, message) {
    uploadUI.hideProgressOverlay();
    //uploadUI.warn(code + " " + message);
}
