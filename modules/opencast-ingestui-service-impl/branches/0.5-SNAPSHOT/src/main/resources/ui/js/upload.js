selectedFile = null;
metaData = null;
mockup = null;
bar = 0;

function initUploader() {

    uploader = new SWFUpload(
            {
                flash_url : "swfupload/swfupload.swf",
                upload_url: "upload.php",
                file_types : "*.*",
                file_types_description : "All Files",
                file_upload_limit : 0,
		file_queue_limit : 0,
                debug: false,

                button_placeholder_id : "uploader",
		button_width: 60,
		button_height: 22,
		button_window_mode: SWFUpload.WINDOW_MODE.TRANSPARENT,
		button_cursor: SWFUpload.CURSOR.HAND,

                file_dialog_complete_handler : fileSelected,
                upload_progress_handler : uploadProgress,
                upload_complete_handler : uploadComplete,
                upload_error_handler : uploadError
            } );
   selectedFile = null;
}

function fileSelected(nSelected, nQueued, totalQueued ) {
    var stats = uploader.getStats();
    selectedFile = uploader.getFile(stats.files_queued - 1);
    document.getElementById("filename").value = selectedFile.name;
    fileSelected = true;
}

function startUpload() {
    if (checkUpload()) {
        // display progress overlay
        document.getElementById("grayOut").style.display = "block";
        var progressPopup = document.getElementById("uploadProgressOverlay");
        progressPopup.style.left = (window.innerWidth / 2) - 214;
        progressPopup.style.top = (window.innerHeight / 2) - 50;
        progressPopup.style.display = "inline-block";

        // start upload
        //uploader.startUpload(selectedFile.id);
        uploadMockup();
    }
}

function uploadProgress(file, completed, total) {
    var percentage = (completed / total) * 100;
    document.getElementById("progressLabel").innerHTML = percentage + "%";
}

function uploadError(id, code, message) {
    uploadComplete(); 
}

function uploadComplete(file) {
    uploadMetadata();
}

function uploadMockup() {
    mockup = window.setInterval("increaseBar();", 500);
}

function increaseBar() {
    if (bar == 100) {
	window.clearInterval(mockup);
	bar = 0;
        uploadMetadata();
    } else {
	bar += 5;
	document.getElementById("progressLabel").innerHTML = bar + "%";
	document.getElementById("progressIndicator").style.width = bar + "%";
    }
}

function uploadMetadata() {
    document.getElementById("progressLabel").innerHTML = "Uploading meta data";
    metaData = { submitter   : document.getElementById("submitter").value,
                 title       : document.getElementById("title").value,
                 presenter   : document.getElementById("presenter").value,
                 description : document.getElementById("description").value,
                 language    : document.getElementById("language").value,
                 distSakai   : document.getElementById("distSakai").checked,
                 distYouTube : document.getElementById("distYouTube").checked,
                 distitunes  : document.getElementById("distITunes").checked
            };
     //$.post("/ingestui/metadata/" + document.getElementById("file").value, $.toJSON(metaData), doneUploadMetadata, "text");
     //$.post("upload.php", metaData, metadataUploaded, "text");
     metadataUploaded();
}

function metadataUploaded(data, textStatus) {
    resetProgressPopup();
    loadUi("upload_complete", "stage");
}

function cancelUpload() {
    uploader.stopUpload();
    resetProgressPopup();
}

function resetProgressPopup() {
    document.getElementById("grayOut").style.display = "none";
    document.getElementById("uploadProgressOverlay").style.display = "none";
}

function checkUpload() {
    if (document.getElementById("title").value == "") {
        alert("Please enter a title for the recording.");
        return false;
    } else if (selectedFile == null) {
        alert("Please choose a file to upload.");
        return false;
    }
    return true;
}

function clearForm() {
    document.getElementById("title").value = "";
    document.getElementById("presenter").value = "";
    document.getElementById("description").value = "";
    document.getElementById("language").value = "";
    document.getElementById("distSakai").checked = false;
    document.getElementById("distYouTube").checked = false;
    document.getElementById("distITunes").checked = false;
    document.getElementById("filename").value = "";
    selectedFile = null;
}