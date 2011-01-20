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

/*    NAMSPACES    */
var ocSeries        = ocSeries || {};
ocSeries.components = {};
ocSeries.additionalComponents = {};

/*    PAGE CONFIGURATION    */
var SERIES_SERVICE_URL = "/series";
var SERIES_LIST_URL = "/admin/series_list.html";
var CREATE_MODE = 1;
var EDIT_MODE   = 2;

ocSeries.mode = CREATE_MODE;

/*    UI FUNCTIONS    */
ocSeries.init = function(){
  //Load i18n strings and replace default english
  ocSeries.Internationalize();
  
  //Add folding action for hidden sections.
  $('.oc-ui-collapsible-widget .ui-widget-header').click(
    function() {
      $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-e');
      $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-s');
      $(this).next().toggle();
      return false;
    });
    
  $('#additionalContentTabs').tabs();
  
  ocSeries.RegisterComponents();
  ocSeries.FormManager = new ocAdmin.Manager('series', '', ocSeries.components, ocSeries.additionalComponents);
  $('#submitButton').click(ocSeries.SubmitForm);
  $('#cancelButton').click(function() {
    document.location = SERIES_LIST_URL;
  });
  
  if(ocUtils.getURLParam('edit') === 'true'){
    ocSeries.mode = EDIT_MODE;
    $('#submitButton').val('Update Series');
    $('#i18n_page_title').text(i18n.page.title.edit);
    $('#i18n_window_title').text(i18n.page.title.edit);
    var seriesId = ocUtils.getURLParam('seriesId');
    if(seriesId !== '') {
      $('#seriesId').val(seriesId);
      $.getJSON(SERIES_SERVICE_URL + "/" + seriesId + ".json", ocSeries.loadSeries);
    }
  }
}

ocSeries.Internationalize = function(){
  //Do internationalization of text
  jQuery.i18n.properties({
    name:'series',
    path:'i18n/'
  });
  ocUtils.internationalize(i18n, 'i18n');
  //Handle special cases like the window title.
  $('#i18n_page_title').text(i18n.page.title.add);
}

ocSeries.loadSeries = function(data) {
  $("#id").val(data.series.id);
  ocSeries.components['description'].setValue(data.series.description);
  for(m in data.series.additionalMetadata.metadata){
    var metadata = data.series.additionalMetadata.metadata[m];
    if(ocSeries.additionalComponents[metadata.key]){
      ocSeries.additionalComponents[metadata.key].setValue(metadata.value);
    }
  }
}

ocSeries.RegisterComponents = function(){
  //Core Metadata
  ocSeries.additionalComponents.title = new ocAdmin.Component(
    ['title'],
    {label:'seriesLabel',required:true}
  );
  
  ocSeries.additionalComponents.contributor = new ocAdmin.Component(
    ['contributor'],
    {label:'contributorLabel'}
  );
  
  ocSeries.additionalComponents.creator = new ocAdmin.Component(
    ['creator'],
    {label: 'creatorLabel'}
  );
  
  //Additional Metadata
  ocSeries.additionalComponents.subject = new ocAdmin.Component(
    ['subject'],
    {label: 'subjectLabel'}
  )
  
  ocSeries.additionalComponents.language = new ocAdmin.Component(
    ['language'],
    {label: 'languageLabel'}
  )
  
  ocSeries.additionalComponents.license = new ocAdmin.Component(
    ['license'],
    {label: 'licenseLabel'}
  )
  
  ocSeries.components.description = new ocAdmin.Component(
    ['description'],
    {label: 'descriptionLabel'}
  )
  
  /*
  //Extended Metadata
  ocAdmin.additionalComponents.type
  //ocAdmin.additionalComponents.subtype
  ocAdmin.additionalComponents.publisher
  ocAdmin.additionalComponents.audience
  //ocAdmin.additionalComponents.duration
  //ocAdmin.additionalComponents.startdate
  //ocAdmin.additionalComponents.enddate
  ocAdmin.additionalComponents.spatial
  ocAdmin.additionalComponents.temporal
  ocAdmin.additionalComponents.rights
  */
}

ocSeries.SubmitForm = function(){
  var seriesXml = ocSeries.FormManager.serialize();
  if(seriesXml){
    if(ocSeries.mode === CREATE_MODE) {
    $.ajax({
      type: 'PUT',
      url: SERIES_SERVICE_URL + '/',
      data: { series: seriesXml },
      complete: ocSeries.SeriesSubmitComplete
    });
    } else {
    $.ajax({
      type: 'POST',
      url: SERIES_SERVICE_URL + '/' + $('#id').val(),
      data: { series: seriesXml },
      complete: ocSeries.SeriesSubmitComplete
    });
    }
  }
}

ocSeries.SeriesSubmitComplete = function(xhr, status){
  if(status == "success"){
    document.location = SERIES_LIST_URL;
  }
  /*for(var k in ocSeries.components){
    if(i18n[k]){
      $("#data-" + k).show();
      $("#data-" + k + " > .data-label").text(i18n[k].label + ":");
      $("#data-" + k + " > .data-value").text(ocSeries.components[k].asString());
    }
  }
  $("#schedulerLink").attr('href',$("#schedulerLink").attr('href') + '?seriesId=' + ocSeries.components.seriesId.getValue());
  $("#submissionSuccess").siblings().hide();
  $("#submissionSuccess").show();*/
}