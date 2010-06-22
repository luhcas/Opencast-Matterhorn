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
    var title = "Results for &quot;" + unescape(value) +"&quot;";
    $('#oc_slidetext-left').html(title);
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