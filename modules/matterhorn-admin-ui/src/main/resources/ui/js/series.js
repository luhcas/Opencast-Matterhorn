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
var Series        = Series || {};
Series.components = {};
var UI            = UI || {};


/*    PAGE CONFIGURATION    */
var SERIES_SERVICE_URL = "/series/rest";

/*    UI FUNCTIONS    */
UI.Init = function(){
  //Load i18n strings and replace default english
  UI.Internationalize();
  
  //Add folding action for hidden sections.
  $('.folder-head').click(
    function() {
      $(this).children('.fl-icon').toggleClass('icon-arrow-right');
      $(this).children('.fl-icon').toggleClass('icon-arrow-down');
      $(this).next().toggle('fast');
      return false;
  });
  
  UI.RegisterComponents();
  Series.FormManager = new AdminForm.Manager('Series', '', Series.components);
  $('#submitButton').click(UI.SubmitForm);
  $('#cancelButton').click(function() {
    document.location = 'recordings.html';
  });
  $.get(SERIES_SERVICE_URL + '/new/id', function(data){ $('#seriesId').val(data.id); });
}

UI.Internationalize = function(){
  //Do internationalization of text
  jQuery.i18n.properties({
    name:'series',
    path:'i18n/'
  });
  AdminUI.internationalize(i18n, 'i18n');
  
  //Handle special cases like the window title.
}

UI.SelectMetaTab = function(elm){
  $(elm).siblings().removeClass('selected');
  $(elm).addClass('selected');
  if(elm.id == "meta-common-tab"){
    $("#common_descriptors").show();
    $("#additional_metadata").hide();
  }else{
    $("#common_descriptors").hide();
    $("#additional_metadata").show();
  }
}

UI.RegisterComponents = function(){
  Series.components.seriesId = new AdminForm.Component(
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
  Series.components.title = new AdminForm.Component(
    ['title'],
    {label:'label-series',required:true}
  );
  
  Series.components.contributor = new AdminForm.Component(
    ['contributor'],
    {label:'label-contributor'}
  );
  
  Series.components.creator = new AdminForm.Component(
    ['creator'],
    {label: 'label-creator'}
  );
  
  //Additional Metadata
  Series.components.subject = new AdminForm.Component(
    ['subject'],
    {label: 'label-subject'}
  )
  
  Series.components.language = new AdminForm.Component(
    ['language'],
    {label: 'label-language'}
  )
  
  Series.components.license = new AdminForm.Component(
    ['license'],
    {label: 'label-license'}
  )
  
  Series.components.description = new AdminForm.Component(
    ['description'],
    {label: 'label-description'}
  )
  
  /*
  //Extended Metadata
  AdminForm.components.type
  //AdminForm.components.subtype
  AdminForm.components.publisher
  AdminForm.components.audience
  //AdminForm.components.duration
  //AdminForm.components.startdate
  //AdminForm.components.enddate
  AdminForm.components.spatial
  AdminForm.components.temporal
  AdminForm.components.rights
  */
}

UI.SubmitForm = function(){
  var seriesXml = Series.FormManager.serialize();
  if(seriesXml){
    $.ajax({
      type: "PUT",
      url: SERIES_SERVICE_URL + '/series',
      data: {
        series: seriesXml
      },
      success: UI.SeriesSubmitComplete
    });
  }
}

UI.SeriesSubmitComplete = function(){
  for(var k in Series.components){
    if(i18n[k]){
      $("#data-" + k).show();
      $("#data-" + k + " > .data-label").text(i18n[k].label + ":");
      $("#data-" + k + " > .data-value").text(Series.components[k].asString());
    }
  }
  $("#schedulerLink").attr('href',$("#schedulerLink").attr('href') + '?seriesId=' + Series.components.seriesId.getValue());
  $("#submission_success").siblings().hide();
  $("#submission_success").show();
}