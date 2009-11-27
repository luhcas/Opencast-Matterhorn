/**
 * @fileOverview Collection of functions to deal with swfUpload
 * @name Upload Manager
 */

/**
 @namespace Holds all functions to deal with swfUpload
*/
var uploadManager = uploadManager || { };

/** Instace of swfUpload */
uploadManager.uploader = null;

/** File currently selected for upload */
uploadManager.selectedFile = null;

/** List of metadata that is uploaded along with the file */
uploadManager.metadata = {};

/** List of fields in to forms mandatory fields that the user still must fill */
uploadManager.missingFields = new Array();

/** Instanciate swfUpload and init this namespace */
uploadManager.init = function() {
    uploadManager.uploader = new SWFUpload(
    {
        flash_url : "swfupload/swfupload.swf",
        upload_url: "../../rest/addMediaPackage",
        preserve_relative_urls : true,
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

/** Colloects the metadata from the form and starts the file upload */
uploadManager.startUpload = function() {
    uploadManager.collectMetadata();
    /* var list = "";    
    for (key in uploadManager.metadata) {
        list += "[" + key + "]:" + uploadManager.metadata[key] + "\n";
        uploadManager.uploader.addFileParam(uploadManager.selectedFile.id, key, uploadManager.metadata[key]);
    }
    alert("Metadata:\n" + list);*/
    uploadManager.uploader.startUpload(uploadManager.selectedFile.id);
}

/** Cancels the current upload and generates an upload error event */
uploadManager.cancelUpload = function() {
    uploadManager.uploader.cancelUpload(uploadManager.selectedFile, false);
    uploadEvents.uploadError(uploadManager.selectedFile, 000, "Upload aborted by user.");
}

/** Collect metadata from the forms
 *  TODO use form.serialize() the serialize the forms */
uploadManager.collectMetadata = function() {
    uploadManager.metadata = { title       : document.getElementById("title").value,
                               presenter   : document.getElementById("contributor").value,
                               description : document.getElementById("description").value,
                               language    : document.getElementById("language").value,
                               series      : document.getElementById('series').value,
                               department  : document.getElementById('department').value,
                               content     : document.getElementById('mediacontent').value,
                               subject     : document.getElementById('subject').value
                             };
                             /*
                               distSakai   : document.getElementById("distSakai").checked,
                               distYouTube : document.getElementById("distYouTube").checked,
                               distitunes  : document.getElementById("distITunes").checked
                             };*/
}

/** Checks if the data in the form is ready for upload */
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
    if (! (/*document.getElementById('distITunesU').checked ||*/
           document.getElementById('distMHMM').checked ||
           /*document.getElementById('distYouTube').checked ||*/
           document.getElementById('distSakai').checked)
       ) {
        uploadManager.missingFields.push('dist');
       }

    // check if media file content was selected
    if (document.getElementById('mediacontent').value == 'none') {
        uploadManager.missingFields.push('mediacontent');
    }

    // display missing field notification
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

/** Resets all forms, deselects selected file */
uploadManager.resetUploader = function() {
    document.basicData.reset.click();           // somehow document.basicData.reset() 'is not a function', twilight zone...
    document.additionalData.reset();
    document.distributionData.reset();
    uploadUI.resetFilename();
    uploadManager.selectedFile = null;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~ upload mockup ~~~~~~~~~~~~~~~~~~~~~~~~~~~

/** Starts a simulated upload */
uploadManager.startUploadMockup = function() {
    uploadUI.currentProgress = 0;
    uploadEvents.uploadStarted(uploadManager.selectedFile);
    uploadManager.uploadMockup = window.setInterval("uploadManager.fireProgressEvent();", 300);
}

/** fires the uploadProgress event to simulate upload progress */
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
