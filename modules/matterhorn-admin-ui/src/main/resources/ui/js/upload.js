/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
var Upload = Upload || {};

Upload.metadata = {};
Upload.retryId = "";

/** Init the upload page. Init events.
 *
 */
Upload.init = function() {

  // are we in debug mode?
  if (ocUtils.getURLParam("debug")) {
    $('#console').show();
  }

  // Event: Retry: use already uploaded file clicked
  $('#use-file-previous').click(function() {
    if ($(this).val()) {
      $('#regular-file-chooser').fadeOut('fast');
      $('#regular-file-chooser-flavor').fadeOut('fast');
      $('#track').val($('#previous-file-link').attr('href'));
      Upload.checkRequiredFields(false);
    }
  });

  // Event: Retry: use replacement file clicked
  $('#use-file-replace').click(function() {
    if ($(this).val()) {
      $('#regular-file-chooser').fadeIn('fast');
      $('#regular-file-chooser-flavor').fadeIn('fast');
      $('#track').val($('#filechooser-ajax'));
    }
  });

  // Event: File location selector clicked
  $(".file-location").change(function() {
    var location = $(this).val();
    $('#filechooser-ajax').attr('src', '../ingest/rest/filechooser-' + location + '.html');
  });

  // Event: Add form filed button clicked
  $('.addFormFieldBtn').click(function() {
    var toClone = $(".creatorRow").children('.formField');
    var clone = $(toClone).clone(true);
    $(clone).removeAttr('id').val('');
    $(this).parent().children('.additionalFieldsContainer').append(clone);   // .insertBefore() yields exception
    var remove = document.createElement("span");
    $(remove).addClass("deleteIcon");
    $(remove).bind('click',function() {
      $(this).prev().remove();
      $(this).remove();
    });
    $(this).parent().children('.additionalFieldsContainer').append(remove);
  });

  // Event: Submit button, submit form if no missing inputs
  $('#BtnSubmit').click( function() {
    if (Upload.checkRequiredFields(true))  {
      ocUtils.log("Collecting metadata");
      $('.formField').each( function() {
        if (($(this).attr('id') != 'flavor') && ($(this).attr('id') != 'distribution')) {
          //log("adding metadata " + $(this).attr('id') + ' ' + $(this).val());
          if ($(this).hasClass('multiValueField')) {
            if (Upload.metadata[$(this).attr('name')] == undefined) {
              Upload.metadata[$(this).attr('name')] = new Array($(this).val());
            } else {
              Upload.metadata[$(this).attr('name')].push($(this).val());
            }
          } else {
            if($(this).attr('id') === 'isPartOf'){
              if($('#series').val() !== '' && $('#isPartOf').val() === ''){ //have text and no id
                Upload.createSeriesFromSearchText();
              }
            }
            Upload.metadata[$(this).attr('name')] = $(this).val();
          }
        }
      });
      ocIngest.metadata = Upload.metadata;
      Upload.showProgressStage();
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
    //alert("Displaying help for: " + help);
    Upload.showHelpBox(help,$(this).offset().top,$(this).offset().left);
    return false;
  });

  // Event: clicked somewhere
  $('body').click( function() {
    $('#helpBox').fadeOut('fast');
  });
  
  $('#series').autocomplete({
    source: '/series/rest/search',
    select: function(event, ui){
      $('#isPartOf').val(ui.item.id);
    },
    search: function(){
      $('#isPartOf').val('');
    }
  });

  ocWorkflow.init($('#workflow-selector'), $('#workflow-config-container'));

  // test if we upload a new recording or want to retry a workflow
  Upload.retryId = ocUtils.getURLParam("retry");
  if (Upload.retryId != '') {
    $('#i18n_page_title').text("Edit Recording Before Continuing");
    $('#BtnSubmit').text("Continue Processing");
    $('#i18n_submit_instr').css('display','none');
    Upload.initRetry(Upload.retryId);
  } else {                                             // FIXME well this has to be cleaned up, agile...
    Upload.retryId = ocUtils.getURLParam("edit");
    if (Upload.retryId != '') {
      $('#i18n_page_title').text("Edit Recording Before Continuing");
      $('#BtnSubmit').text("Continue Processing");
      $('#i18n_submit_instr').css('display','none');
      Upload.initRetry(Upload.retryId);
    }
  }
}

Upload.initRetry = function(wfId) {
  // display current file element / hide file chooser
  $('#retry-file').css('display', 'block');
  $('#regular-file-selection').css('display', 'none');
  $('#regular-file-chooser').css('display', 'none');
  $('#regular-file-chooser-flavor').css('display', 'none');
  $('#track').val('reingest');
  // get failed Workflow
  $.ajax({
    method: 'GET',
    url: '../workflow/rest/instance/'+ wfId +'.xml',
    dataType: 'xml',
    success: function(data) {
      ocIngest.previousMediaPackage = data;
      var catalogUrl = $(data.documentElement).find("mediapackage > metadata > catalog[type='dublincore/episode'] > url").text();
      Upload.loadDublinCore(catalogUrl);
      // previous file
      var files = new Array();
      $(data.documentElement).find("mediapackage > media > track").each(function(index, elm) {
        var type = $(elm).attr('type');
        if (type.split(/\//)[1] == 'source') {
          var filename = $(elm).find('url').text();
          var fileItem = {url: filename, flavor: type};
          ocIngest.previousFiles.push(fileItem);
          //$('#previous-file-url').val(filename);
          filename = filename.split(/\//);
          filename = filename[filename.length-1];
          files.push(filename);
        }
      });
      $('#previous-file-list').text(files.join(', '));
      // previous workflow definition
      var defId = $(data.documentElement).find('template').text();
      $('#workflow-selector').val(defId);
      ocWorkflow.definitionSelected(defId, $('#workflow-config-container'), function() {
        $(data.documentElement).find("> configurations > configuration").each(function(index, elm) {
          var fieldname = '#' + $(elm).attr('key').replace(/\./g, '\\\\.');
          var field = $(fieldname);
          if (field) {
            if ($(field).is('input[type=checkbox]')) {
              $(field).attr('checked',$(elm).text());
            } else {
              $(field).val($(elm).text());
            }
          }
        });
      });
    }
  });
}

Upload.loadDublinCore = function(url) {
  $.ajax({
    method: 'GET',
    url: url,
    dataType: 'xml',
    success: function(data) {
      var url = $(data.documentElement).children().each(function(index, elm) {
        var tagName = elm.tagName.split(/:/)[1];
        if ($('#'+tagName).val() != '') {   // multi value? --> clone the field
          var toClone = $('#'+tagName);
          var clone = $(toClone).clone(true);
          $(clone).removeAttr('id').val('');
          $(toClone).parent().children('.additionalFieldsContainer').append(clone);   // .insertBefore() yields exception
          var remove = document.createElement("span");
          $(remove).addClass("deleteIcon");
          $(remove).bind('click',function() {
            $(this).prev().remove();
            $(this).remove();
          });
          $(toClone).parent().children('.additionalFieldsContainer').append(remove);
          $(clone).val($(elm).text());
        } else {
          $('#'+tagName).val($(elm).text());
        }
      });
    }
  });
}

/** collect data from workflow configuration panel
 *
 */
Upload.collectWorkflowConfig = function() {
  var out = {};
  $('.configField').each(function() {
    out[$(this).attr('name')] = $(this).val();
  });
  return out;
}

/** check if data for required fields is missing
 *
 */
Upload.checkRequiredFields = function(submit) {
  var missing = false;
  var wrongtype = false;
  $('.requiredField:visible, .requiredField[type|=hidden]').each( function() {
    if (!$(this).val()) {
      $('#notification-' + $(this).attr('id')).show();
      if ((submit) || ($('#container-missingFields').is(':visible'))) {
        $(this).prev('.fl-label').css('color','red');
      }
      if ((submit) && $('#track').val() == '') {
        $('#i18n_upload_file').css('color','red');
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

Upload.showHelpBox = function(help,top,left) {
  $('#helpTitle').text(helpTexts[help][0]);
  $('#helpText').text(helpTexts[help][1]);
  $('#helpBox').css({
    top:top,
    left:left
  }).fadeIn('fast');
}

/** gray-out whole page and display progress popup
 *
 */
Upload.showProgressStage = function() {
  //$('#gray-out').fadeIn('fast');
  $('#gray-out').css('display','block');
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
        if (Upload.metadata[key] instanceof Array) {
          $('#field-'+key).children('.fieldValue').text(Upload.metadata[key].join(', '));
        } else {
          $('#field-'+key).children('.fieldValue').text(Upload.metadata[key]);
        }
      }
      if (Upload.retryId != "") {
        $('#heading-metadata').text('Your recording with the following information has been resubmitted');
      }
    }
    $('#field-filename').children('.fieldValue').text(UploadListener.shortFilename);
    
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
    $('#field-filename').children('.fieldValue').text(UploadListener.shortFilename);
    
  });
}

Upload.createSeriesFromSearchText = function(){
  var seriesXml = '<series><metadataList><metadata><key>title</key><value>' + $('#series').val() + '</value></metadata></metadataList></series>';
  var creationSucceeded = false;
  $.ajax({
    async: false,
    type: 'PUT',
    url: '/series/rest/series',
    data: { series: seriesXml },
    success: function(data){
      if(data.success){
        creationSucceeded = true;
        $('#isPartOf').val(data.id);
      }
    }
  });
  return creationSucceeded;
}
