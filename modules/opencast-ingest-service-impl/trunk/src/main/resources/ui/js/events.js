var uploadEvents = uploadEvents || { };

// ~~~~~~~~~~~~~~~~~~~~~~~ ui elements ~~~~~~~~~~~~~~~~~~~~~~~

/* fired when the value of a form field is changed
 *
 */
uploadEvents.fieldChanged = function(field, value) {
   uploadUI.log("EVENT: " + "Value of filed " + field.id + " changed to: " + value);
}

uploadEvents.btnSubmit = function() {
    uploadUI.log("EVENT: " + "Submit button clicked");
    if (uploadManager.checkUpload()) {
        uploadManager.startUpload();
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
    UI.load("version3", "stage");
}


// ~~~~~~~~~~~~~~~~~~~~~~~ misc events ~~~~~~~~~~~~~~~~~~~~~~~

uploadEvents.titleMissing = function() {
    uploadUI.warn("Please enter a title for the recording.");
}

uploadEvents.fileMissing = function() {
    uploadUI.warn("Please select a media file to upload.");
}


// ~~~~~~~~~~~~~~~~~~~~~~~ swfUpload events ~~~~~~~~~~~~~~~~~~~~~~~

uploadEvents.fileSelected = function(file) {
    uploadManager.selectedFile = file;
    uploadUI.setFilename(file.name);
}

uploadEvents.uploadStarted = function(file) {
    uploadUI.showProgressOverlay();
}

uploadEvents.uploadProgress = function(file, completed, total) {
    uploadUI.setProgressBar(completed / total * 100);
}

uploadEvents.uploadComplete = function(file) {
    uploadManager.uploader.destroy();
    UI.load("upload_complete", "stage");
    uploadUI.hideProgressOverlay();
}

uploadEvents.uploadError = function(file, code, message) {
    uploadUI.hideProgressOverlay();
    //uploadUI.warn(code + " " + message);
}
