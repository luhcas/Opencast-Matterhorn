var uploadManager = uploadManager || { };

uploadManager.uploader = null;
uploadManager.selectedFile = null;
uploadManager.metadata = {};
uploadManager.missingFields = new Array();

uploadManager.init = function() {
    uploadManager.uploader = new SWFUpload(
    {
        flash_url : "swfupload/swfupload.swf",
        upload_url: "../rest/addMediaPackage",
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
    uploadManager.metadata = { title       : document.getElementById("title").value,
                               presenter   : document.getElementById("contributor").value,
                               description : document.getElementById("description").value,
                               language    : document.getElementById("language").value,
                               series      : document.getElementById('series').value,
                               department  : document.getElementById('department').value,
                               subject     : document.getElementById('subject').value
                             };
                             /*
                               distSakai   : document.getElementById("distSakai").checked,
                               distYouTube : document.getElementById("distYouTube").checked,
                               distitunes  : document.getElementById("distITunes").checked
                             };*/
}

/* Checks if the data in the form is ready for upload
 *
 */
uploadManager.checkUpload = function(highlight) {
    uploadManager.missingFields = new Array();
    var label;

    // check if title is provided
    if (document.getElementById("title").value == "") {
        uploadManager.missingFields.push('title');
    }

    // check if media file is selected
    if (uploadManager.selectedFile == null) {
        uploadManager.missingFields.push('file');
    }

    // check if distribution channel is selected
    if (! (document.getElementById('distITunesU').checked ||
           document.getElementById('distMHMM').checked ||
           document.getElementById('distYouTube').checked ||
           document.getElementById('distSakai').checked)
       ) {
        uploadManager.missingFields.push('dist');
       }

    $('#missingFields-container li').css('display', 'none');
    for (i=0; i < uploadManager.missingFields.length; i++) {
        $('#notification-' + uploadManager.missingFields[i]).css('display', 'block');
    }

    if (highlight) {
        for (i=0; i < uploadManager.missingFields.length; i++) {
            uploadUI.setLabelColor(uploadManager.missingFields[i], 'red');
        }
    }

    // is everything fine ?
    if (uploadManager.missingFields.length == 0) {
        uploadUI.hideMissingFields();
        return true;
    } else {
        return false;
    }
}



/* Changes UI elements but it's related to data, so it goes here
 * resets internal data for the uploader
 */
uploadManager.resetUploader = function() {
    document.getElementById("title").value = "";
    document.getElementById("contributor").value = "";
    document.getElementById("description").value = "";
    document.getElementById("language").value = "";
    document.getElementById("distSakai").checked = false;
    document.getElementById("distYouTube").checked = false;
    document.getElementById("distITunes").checked = false;
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
