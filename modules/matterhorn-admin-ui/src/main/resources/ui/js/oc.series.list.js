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
ocSeriesList = {} || ocSeriesList;
ocSeriesList.views = {} || ocSeriesList.views;
ocSeriesList.views.seriesView = {} || ocSeriesList.seriesView;

ocSeriesList.init = function(){
  //var result = TrimPath.processDOMTemplate("seriesTemplate", ocSeriesList.views);
  //  $('#seriesTableContainer').html(result);
  
  $('#addHeader').jqotesubtpl('templates/series_list-header.tpl', {});
  
  $.ajax({
    url: "/series/series.json?edit=true",
    type: "GET",
    success: function(data)
    {
      ocSeriesList.buildSeriesView(data);
    }
  });
  
  $("#addSeriesButton").button({
    icons:{
      primary:"ui-icon-circle-plus"
    }
  });
}

ocSeriesList.buildSeriesView = function(data) {
  ocUtils.log($.isArray(data));
  for(var i = 0; i < data.length; i++) {
    var s = ocSeriesList.views.seriesView[data[i]['http://purl.org/dc/terms/']['identifier'][0].value] = {};
    s.id = data[i]['http://purl.org/dc/terms/']['identifier'][0].value;
    for(var key in data[i]['http://purl.org/dc/terms/']) {
      if(key === 'title'){
        s.title = data[i]['http://purl.org/dc/terms/'][key][0].value
      } else if(key === 'creator') {
        s.creator = data[i]['http://purl.org/dc/terms/'][key][0].value
      } else if(key  === 'contributor') {
        s.contributor = data[i]['http://purl.org/dc/terms/'][key][0].value
      }
    }
  }
  $('#seriesTableContainer').jqotesubtpl("templates/series_list-table.tpl", ocSeriesList.views);
  $('#seriesTable').tablesorter({
    cssHeader: 'oc-ui-sortable',
    cssAsc: 'oc-ui-sortable-Ascending',
    cssDesc: 'oc-ui-sortable-Descending' ,
    headers: {
      3: {
        sorter: false
      }
    }
  });
}

ocSeriesList.deleteSeries = function(seriesId, title) {
  if(confirm('Are you sure you want to delete the series "' + title + '"?')){
    $.ajax({
      type: 'DELETE',
      url: '/series/' + seriesId,
      error: function(XHR,status,e){
        alert('Could not remove series "' + title + '"');
      },
      success: function(data) {
        location.reload();
      }
    });
  }
}