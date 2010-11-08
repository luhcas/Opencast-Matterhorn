/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace analytics delegate. This file contains the rest endpoint and passes the data to the analytics plugin
 */
Opencast.Analytics = ( function() {

  /**
   * 
   * 
   *  variables
   */
  var
  mediaPackageId,
  duration,
  ANALYTICS              = "Analytics",
  ANALYTICSHIDE          = "Analytics off";
  /**
   * true if analytics are displayed
   */
  var analyticsDisplayed = false;

  /**
  * @memberOf Opencast.Analytics
  * @description Show Analytics
  */
  function showAnalytics()
  {
      $.ajax( {
        type : 'GET',
        contentType : 'text/xml',
        url : "../../usertracking/rest/footprint.xml",
        data : "id=" + mediaPackageId,
        dataType : 'xml',

        success : function(xml) {

        var position = 0;
        var views;
        var lastPosition = -1;
        var lastViews;
        var footprintData = new Array(duration);

        for ( var i = 0; i < footprintData.length; i++)
          footprintData[i] = 0;
        $(xml).find('footprint').each(function() {
          position = parseInt($(this).find('position').text());
          views = parseInt($(this).find('views').text());

          if (position -1 != lastPosition ) {
            for(var j = lastPosition + 1; j < position; j++) {
              footprintData[j] = lastViews;
            }
          }
          footprintData[position] = views;
          lastPosition = position;
          lastViews = views;
        })

         Opencast.AnalyticsPlugin.addAsPlugin('#analytics', footprintData);
        },
          error : function(a, b, c) {
          // Some error while trying to get the views
        }
        });

      $(".segments").css('top', '-25px');
      //$.sparkline_display_visible()
      $("#analytics").show();
  }

/**
   * @memberOf Opencast.Analytics
   * @description Hide the notes
   */
  function hideAnalytics()
  {
      $("#analytics").hide();
      $(".segments").css('top', '0px');
  }


  /**
   * @memberOf Opencast.Analytics
   * @description Toggle Analytics
   */
  function doToggleAnalytics()
  {
      if (!analyticsDisplayed)
      {
          showAnalytics();
          analyticsDisplayed = true;
          $('#oc_checkbox-statistics').attr('checked', 'checked');
      }
      else
      {
          hideAnalytics();
          analyticsDisplayed = false;
           $('#oc_checkbox-statistics').removeAttr('checked');
      }
  }

  /**
   * @memberOf Opencast.Analytics
   * @description Set the mediaPackageId
   * @param String mediaPackageId 
   */
  function setMediaPackageId(id) 
  {
      mediaPackageId = id;
  }
  
  /**
   * @memberOf Opencast.Analytics
   * @description Set the duration
   * @param int duration 
   */
  function setDuration(val) 
  {
      duration = val;
  }

  return {
    hideAnalytics : hideAnalytics,
    showAnalytics : showAnalytics,
    setDuration : setDuration,
    setMediaPackageId : setMediaPackageId,
    doToggleAnalytics : doToggleAnalytics
  };
}());
