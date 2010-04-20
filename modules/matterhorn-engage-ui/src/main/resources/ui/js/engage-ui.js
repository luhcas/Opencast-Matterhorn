/*global $, Videodisplay, Opencast, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
@namespace the global Opencast namespace engage
*/
Opencast.engage = (function () {

    /**
     * @memberOf Opencast.engage
     * @description Gets the current media package id
     */
    function getMediaPackageId() {
      var value = getGETParameter("id");
      return value;
    }
    
    /**
     * @memberOf Opencast.engage
     * @description Gets the current video url
     */
    function getVideoUrl() {
      var value = getGETParameter("videoUrl");
      return value;
    }
    
    /**
     * @memberOf Opencast.engage
     * @description Get the value of the GET parameter with the passed "name"
     * @param string name
     * @return The value of the GET parameter
     */
    function getGETParameter(name) {
      name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
      var regexS = "[\\?&]" + name + "=([^&#]*)";
      var regex = new RegExp(regexS);
      var results = regex.exec(window.location.href);
      if (results == null)
        return null;
      else
        return results[1];
    }
    
    return {
      getMediaPackageId : getMediaPackageId,
      getVideoUrl : getVideoUrl
    };
}());