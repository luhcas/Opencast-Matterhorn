var uploadManager = uploadManager || { };

uploadManager.uploader = null;
uploadManager.selectedFile = null;
uploadManager.metadata = null;

uploadManager.init = function() {
    uploadManager.uploader = new SWFUpload(
    {
        flash_url : "swfupload/swfupload.swf",
        upload_url: "http://localhost:8080/ingest/rest/addMediaPackage",
        file_types : "*.*",
        file_types_description : "All Files",
        file_upload_limit : 0,
        file_queue_limit : 0,
        file_post_name : "mediafile",
        debug: false,

        button_placeholder_id : "uploader",
        button_width: 60,
        button_height: 22,
        button_window_mode: SWFUpload.WINDOW_MODE.TRANSPARENT,
        button_cursor: SWFUpload.CURSOR.HAND,

        file_queued_handler : uploadEvents.fileSelected,
        upload_start_handler : uploadEvents.uploadStarted,
        upload_progress_handler : uploadEvents.uploadProgress,
        upload_complete_handler : uploadEvents.uploadComplete,
        upload_error_handler : uploadEvents.uploadError
    } );
   selectedFile = null;
}

uploadManager.startUpload = function() {
    uploadManager.collectMetadata();
    // give metadata to uploader
    var list = "";
    var url = "../rest/addMediaPackage";

    for (key in uploadManager.metadata) {
        list += "[" + key + "]:" + uploadManager.metadata[key] + "\n";
        uploadManager.uploader.addFileParam(uploadManager.selectedFile.id, key, uploadManager.metadata[key]);
    }
    //alert(url);
    uploadManager.uploader.setUploadURL(url);
    uploadManager.uploader.startUpload(uploadManager.selectedFile.id);
}

uploadManager.cancelUpload = function() {
    // tell swfUpload to stop upload
    uploadEvents.uploadError(uploadManager.selectedFile, 000, "Upload aborted by user.");
}

uploadManager.collectMetadata = function() {
    uploadManager.metadata = { title       : UI.$("title").value,
                               presenter   : UI.$("contributor").value,
                               description : UI.$("description").value,
                               language    : UI.$("language").value
                             };
                             /*
                               distSakai   : UI.$("distSakai").checked,
                               distYouTube : UI.$("distYouTube").checked,
                               distitunes  : UI.$("distITunes").checked
                             };*/
}

/* Checks if the data in the form is ready for upload
 *
 */
uploadManager.checkUpload = function() {
    // check if title is provided
    if (UI.$("title").value == "") {
        uploadEvents.titleMissing();
        return false;
    }

    // check if media file is selected
    else if (uploadManager.selectedFile == null) {
        uploadEvents.fileMissing();
        return false;
    }

    // everything is fine
    return true;
}

/* Changes UI elements but it's related to data, so it goes here
 * resets internal data for the uploader
 */
uploadManager.resetUploader = function() {
    UI.$("title").value = "";
    UI.$("presenter").value = "";
    UI.$("description").value = "";
    UI.$("language").value = "";
    UI.$("distSakai").checked = false;
    UI.$("distYouTube").checked = false;
    UI.$("distITunes").checked = false;
    uploadUI.resetFilename();
    uploadManager.selectedFile = null;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~ upload mockup ~~~~~~~~~~~~~~~~~~~~~~~~~~~

// start uploadMockup
uploadManager.startUploadMockup = function() {
    uploadUI.currentProgress = 0;
    uploadEvents.uploadStarted(uploadManager.selectedFile);
    uploadManager.uploadMockup = window.setInterval("uploadManager.fireProgressEvent();", 300);
}

// fire uploadProgress event for uploadMockup
uploadManager.fireProgressEvent = function() {
    uploadUI.currentProgress += 5;
    if (uploadUI.currentProgress > 100) {
        window.clearInterval(uploadManager.uploadMockup);
        uploadManager.currentProgress = 0;
        uploadEvents.uploadComplete(null);
    } else {
        uploadEvents.uploadProgress(uploadManager.selectedFile, uploadUI.currentProgress, 100);
    }
}
