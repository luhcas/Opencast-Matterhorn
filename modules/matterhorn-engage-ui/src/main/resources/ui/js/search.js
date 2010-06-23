/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace search
 */
Opencast.search = ( function() {

  /**
   * @memberOf Opencast.search
   * @description Does the search
   */
  function showResult(value){
    
    var mediaPackageId = Opencast.engage.getMediaPackageId();
    $.ajax(
        {
          type: 'GET',
          contentType: 'text/xml',
          url:"../../search/rest/episode",
          data: "id=" + mediaPackageId+"&q="+escape(value),
          dataType: 'xml',

          success: function(xml) 
          {
          var parsedTitle = $(xml).find('title').text();
          var title = "Results for &quot;" + unescape(value) +"&quot;";
            $('#oc_slidetext-left').html(title +"<br/>" + "Title: " + parsedTitle);
          },
          error: function(a, b, c) 
          {
            // Some error while trying to get the views
          }
        }); 
  }

  /**
   * @memberOf Opencast.search
   * @description Initializes the search view
   */
  function initialize() {
    // initialize
  }

  return {
    showResult : showResult,
    initialize : initialize
  };
}());