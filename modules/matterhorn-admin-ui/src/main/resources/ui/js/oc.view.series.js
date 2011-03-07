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
var ocViewSeries = (function(){
  var SERIES_URL = '/series';
  var self = this;
  
  this.init = function() {
    var id = ocUtils.getURLParam('seriesId');

    $('.oc-ui-collapsible-widget .ui-widget-header').click( function() {
      $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-e');
      $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-s');
      $(this).next().toggle();
      return false;
    });
    $('#id').text(id);
    self.loadSeries(id);
  };
    
  this.loadSeries = function(seriesId) {
    var i, mdList, metadata, series;
    if(seriesId !== '') {
      $.get(SERIES_URL + '/' + seriesId + '.json', function(data) {
        series = data.series;
        if(series.description !== undefined) {
          $('#description').text(series.description);
        }
        if(series.additionalMetadata !== undefined) {
          if(!$.isArray(series.additionalMetadata.metadata)){
            mdList = [series.additionalMetadata.metadata];
          } else {
            mdList = series.additionalMetadata.metadata;
          }
          for(i in mdList) {
            metadata = mdList[i];
            $('#' + metadata.key).text(metadata.value);
          }
        }
      });
    }
  };
  
  return this;
  
}());