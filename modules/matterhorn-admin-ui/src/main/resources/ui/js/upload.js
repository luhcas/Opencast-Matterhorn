var Upload = Upload || {};

Upload.metadata = {};

/** Init the upload page. Init events.
 *
 */
Upload.init = function() {

  // are we in debug mode?
  if (Upload.getURLParam("debug")) {
    $('#console').show();
  }

  // Event: Submit button, submit form if no missing inputs
  $('#BtnSubmit').click( function() {
    if (Upload.checkRequiredFields(true))  {
      Upload.log("Collecting metadata");
      $('.formField').each( function() {
        if (($(this).attr('id') != 'flavor') && ($(this).attr('id') != 'distribution')) {
          //log("adding metadata " + $(this).attr('id') + ' ' + $(this).val());
          Upload.metadata[$(this).attr('id')] = $(this).val();
        }
      });
      UploadListener.uploadStarted();
      ocIngest.metadata = Upload.metadata;
      ocIngest.createMediaPackage();
    } else {
      $('#container-missingFields').show('fast');
    }
  });

  // Event: collapsable title clicked, de-/collapse collapsables
  $('.collapse-control').click(function() {
    $(this).toggleClass('collapse-control-closed');
    $(this).toggleClass('collapse-control-open');
    $(this).next('.collapsable').toggle('fast');
  });

  // Event: form field changed, check if data for required fields is missing when a input value changes
  $('.requiredField').change( function() {
    Upload.checkRequiredFields();
  });

  // Event: Help icon clicked, display help
  $('.helpIcon').click( function() {
    var help = $(this).prev().attr('id');
    Upload.showHelpBox(help);
    return false;
  });

  // Event: clicked somewhere
  $('body').click( function() {
    $('#helpBox').fadeOut('fast');
  });

  // Event: workflow selected
  $('#workflow-selector').change( function() {
    Upload.workflowSelected($(this).val());
  })

  // get workflow definitions
  $.ajax({
    url: '../workflow/rest/definitions.json',
    dataType: 'json',
    success: function(data) {
      for (i in data.workflow_definitions) {
        var option = document.createElement("option");
        option.setAttribute("value", data.workflow_definitions[i].title);
        option.innerHTML = data.workflow_definitions[i].title;
        if (data.workflow_definitions[i].title == "full") {
          option.setAttribute("selected", "true");
        }
        $('#workflow-selector').append(option);
      }
      Upload.workflowSelected($('#workflow-selector').val());
    }
  });
}

/** invoked when a workflow is selected. display configuration panel for
 *  the selected workflow if defined.
 *  
 */
Upload.workflowSelected = function(workflow) {
  $('#workflow-config-container').load(
    '../workflow/rest/configurationPanel?definitionId=' + workflow,
    function() {
      $('#workflow-config-container').show('fast');
    }
    );
}

/** collect data from workflow configuration panel
 *
 */
Upload.collectWorkflowConfig = function() {
  //TODO write something
  }

/** check if data for required fields is missing
 *
 */
Upload.checkRequiredFields = function(submit) {
  var missing = false;
  var wrongtype = false;
  $('.requiredField').each( function() {
    if (!$(this).val()) {
      $('#notification-' + $(this).attr('id')).show();
      if ((submit) || ($('#container-missingFields').is(':visible'))) {
        $(this).prev('.fl-label').css('color','red');
      }
      missing = true;
    } else {
      $('#notification-' + $(this).attr('id')).hide();
      $(this).prev('.fl-label').css('color','black');
    }
  });
  // check for right file extension
  /*
  if ($('#track').val() != '') {
    var ext = $('#track').val();
    ext = ext.substr(ext.length-3).toLowerCase();
    var right = (ext == 'avi') || (ext == 'mpg') ||
    (ext == 'mp4') || (ext == 'mkv') ||
    (ext == 'flv') || (ext == 'mov') ||
    (ext == 'wmv') || (ext == 'mp3');
    if (!right) {
      $('#notification-track').show();
      wrongtype = true;
    } else {
      $('#notification-track').hide();
      wrongtype = false;
    }
  }
  if (!missing && !wrongtype) {
    $('#container-missingFields').hide('fast');
  }
  return !missing && right;*/
  if (!missing) {
    $('#container-missingFields').hide('fast');
  }
  return !missing;
}

Upload.showHelpBox = function(help) {
  $('#helpTitle').text(helpTexts[help][0]);
  $('#helpText').text(helpTexts[help][1]);
  $('#helpBox').css({
    top:$(this).offset().top,
    left:$(this).offset().left
  }).fadeIn('fast');
}

/** gray-out whole page and display progress popup
 *
 */
Upload.showProgressStage = function() {
  $('#gray-out').fadeIn('fast');
  $('#progress-stage').fadeIn('normal');
}

/** Restore view
 *
 */
Upload.hideProgressStage = function() {
  $('#gray-out').fadeOut('fast');
  $('#progress-stage').fadeOut('normal');
}

/** Set the progress view to a certain state
 *
 */
Upload.setProgress = function(width, text, total, transfered) {
  $('#progressbar-indicator').css('width',width);
  $('#progressbar-label').text(text);
  $('#label-filesize').text(total);
  $('#label-bytestrasfered').text(transfered);
}

/** Load success screen into stage
 *
 */
Upload.showSuccessScreen = function() {
  $('#stage').load('complete.html', function() {
    for (key in Upload.metadata) {
      if (Upload.metadata[key] != "") {
        $('#field-'+key).css('display','block');
        $('#field-'+key).children('.fieldValue').text(Upload.metadata[key]);
      }
    }
  });
}

/** Load failed screen into stage and display error message
 *
 */
Upload.showFailedScreen = function(message) {
  Upload.hideProgressStage();
  $('#stage').load('error.html', function() {
    if (message) {
      $('#error-message').text(message).show();
    }
  });
}

/** print line to log console
 *
 */
Upload.log = function(message) {
  var console = document.getElementById("console");
  console.innerHTML += message + "\n";
  console.scrollTop = console.scrollHeight;
}

/** get parameter from URL
 *
 */
Upload.getURLParam = function(name) {
  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
  var regexS = "[\\?&]"+name+"=([^&#]*)";
  var regex = new RegExp( regexS );
  var results = regex.exec( window.location.href );
  if( results == null )
    return "";
  else
    return results[1];
}