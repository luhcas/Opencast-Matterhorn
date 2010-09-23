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

/*    PAGE CONFIGURATION    */
var SERIES_SERVICE_URL = "/series/rest";

/*    UI FUNCTIONS    */
ocSeries.Init = function(){
  //Load i18n strings and replace default english
  ocSeries.Internationalize();
  
  //Add folding action for hidden sections.
  $('.folder-head').click(
    function() {
      $(this).children('.fl-icon').toggleClass('icon-arrow-right');
      $(this).children('.fl-icon').toggleClass('icon-arrow-down');
      $(this).next().toggle('fast');
      return false;
  });
  
  ocSeries.RegisterComponents();
  ocSeries.FormManager = new ocAdmin.Manager('Series', '', ocSeries.components);
  $('#submitButton').click(ocSeries.SubmitForm);
  $('#cancelButton').click(function() {
    document.location = 'recordings.html';
  });
  $.get(SERIES_SERVICE_URL + '/new/id', function(data){ $('#seriesId').val(data.id); });
}

ocSeries.Internationalize = function(){
  //Do internationalization of text
  jQuery.i18n.properties({
    name:'series',
    path:'i18n/'
  });
  ocAdmin.internationalize(i18n, 'i18n');
  
  //Handle special cases like the window title.
}

ocSeries.SelectMetaTab = function(elm){
  $(elm).siblings().removeClass('selected');
  $(elm).addClass('selected');
  if(elm.id == "metaCommonTab"){
    $("#commonDescriptors").show();
    $("#additionalMetadata").hide();
  }else{
    $("#commonDescriptors").hide();
    $("#additionalMetadata").show();
  }
}

ocSeries.RegisterComponents = function(){
  ocSeries.components.seriesId = new ocAdmin.Component(
    ['seriesId'],
    { required: true, nodeKey: 'seriesId' },
    { toNode: function(parent) {
        for(var el in this.fields){
          var container = parent.ownerDocument.createElement(this.nodeKey);
          container.appendChild(parent.ownerDocument.createTextNode(this.getValue()));
        }
        if(parent && parent.nodeType){
          parent.ownerDocument.documentElement.appendChild(container);
        }
        return container;
      }
    }
  );
  //Core Metadata
  ocSeries.components.title = new ocAdmin.Component(
    ['title'],
    {label:'seriesLabel',required:true}
  );
  
  ocSeries.components.contributor = new ocAdmin.Component(
    ['contributor'],
    {label:'contributorLabel'}
  );
  
  ocSeries.components.creator = new ocAdmin.Component(
    ['creator'],
    {label: 'creatorLabel'}
  );
  
  //Additional Metadata
  ocSeries.components.subject = new ocAdmin.Component(
    ['subject'],
    {label: 'subjectLabel'}
  )
  
  ocSeries.components.language = new ocAdmin.Component(
    ['language'],
    {label: 'languageLabel'}
  )
  
  ocSeries.components.license = new ocAdmin.Component(
    ['license'],
    {label: 'licenseLabel'}
  )
  
  ocSeries.components.description = new ocAdmin.Component(
    ['description'],
    {label: 'descriptionLabel'}
  )
  
  /*
  //Extended Metadata
  ocAdmin.components.type
  //ocAdmin.components.subtype
  ocAdmin.components.publisher
  ocAdmin.components.audience
  //ocAdmin.components.duration
  //ocAdmin.components.startdate
  //ocAdmin.components.enddate
  ocAdmin.components.spatial
  ocAdmin.components.temporal
  ocAdmin.components.rights
  */
}

ocSeries.SubmitForm = function(){
  var seriesXml = ocSeries.FormManager.serialize();
  if(seriesXml){
    $.ajax({
      type: "PUT",
      url: SERIES_SERVICE_URL + '/series',
      data: {
        series: seriesXml
      },
      success: ocSeries.SeriesSubmitComplete
    });
  }
}

ocSeries.SeriesSubmitComplete = function(){
  for(var k in ocSeries.components){
    if(i18n[k]){
      $("#data-" + k).show();
      $("#data-" + k + " > .data-label").text(i18n[k].label + ":");
      $("#data-" + k + " > .data-value").text(ocSeries.components[k].asString());
    }
  }
  $("#schedulerLink").attr('href',$("#schedulerLink").attr('href') + '?seriesId=' + ocSeries.components.seriesId.getValue());
  $("#submissionSuccess").siblings().hide();
  $("#submissionSuccess").show();
}