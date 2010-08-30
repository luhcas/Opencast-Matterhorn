/*global $, Videodisplay, Opencast, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
@namespace the global Opencast namespace engage
*/
Opencast.engage = (function () {

  var loadProgressPercent = -1;

  /**
   * @memberOf Opencast.engage
   * @description Gets player type ("watch" or "embed")
   * @return the player type
   */
  function getPlayerType() {
    var pathname = window.location.pathname;
    return pathname;
  }

  /**
   * @memberOf Opencast.engage
   * @description Gets the url to the search service;
   * @return the search service endpoint url
   */
  function getSearchServiceEpisodeIdURL() {
    var restEndpoint = "../../search/rest/episode.xml?id="; // Production 
    //var restEndpoint = "xml/episode.xml?id="; // Activate for testing purposes
    //var restEndpoint = "episode-segments.xml?id="; // Activate for testing purposes
    return restEndpoint;
  }

  /**
   * @memberOf Opencast.engage
   * @description Gets the current load progress
   * @return The current load progress
   */
  function getLoadProgress() {
    if(loadProgressPercent === -1)
      return -1;
    else {
      var duration = Opencast.Player.getDuration();
      return duration * loadProgressPercent/100;
    }
  }

  /**
   * @memberOf Opencast.engage
   * @description Sets the current load progress
   * @param The current load progress
   */
  function setLoadProgressPercent(value) {
    if(0 <= value && value <= 100){
      loadProgressPercent = value;
    }
  }

  function getCookie(name) {
    var start = document.cookie.indexOf( name + "=" );
    var len = start + name.length + 1;
    if ( ( !start ) && ( name != document.cookie.substring( 0, name.length ) ) ) {
      return null;
    }
    if ( start == -1 ) return null;
    
    var end = document.cookie.indexOf( ';', len );
    if ( end == -1 ) 
      end = document.cookie.length;
    return unescape( document.cookie.substring( len, end ) );
  }

  function formatSeconds(seconds) {
    var result = "";

    if(parseInt(seconds / 3600) < 10)
      result += "0";
    result += parseInt(seconds / 3600);
    result += ":";

    if((parseInt(seconds/60) - parseInt(seconds/3600) * 60) < 10)
      result += "0";
    result += parseInt(seconds/60) - parseInt(seconds/3600) * 60;
    result += ":";

    if(seconds % 60 < 10)
      result += "0";
    result += seconds % 60;

    return result;
  }

  /**
     * @memberOf Opencast.engage
     * @description Gets the current media package id
     * @return The current media package id
     */
    function getMediaPackageId() {
      var value = getGETParameter("id");
      return value;
    }

    /**
     * @memberOf Opencast.engage
     * @description Gets the play paramenter
     * @return The play parameter
     */
    function getPlay() {
      var value = getGETParameter("play");
      return value;
    }

    /**
     * @memberOf Opencast.engage
     * @description Gets the current video url
     * @return The video url
     */
    function getVideoUrl() {
      var value = getGETParameter("videoUrl");
      return value;
    }

    /**
     * @memberOf Opencast.engage
     * @description Gets the current video url 2
     * @return The video url 2
     */
    function getVideoUrl2() {
      var value = getGETParameter("videoUrl2");
      return value;
    }

    /**
     * @memberOf Opencast.engage
     * @description Gets the current cover url
     * @return The cover url
     */
    function getCoverUrl() {
      var value = getGETParameter("coverUrl");
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
      getCookie : getCookie,
      formatSeconds : formatSeconds,
      getMediaPackageId : getMediaPackageId,
      getPlay : getPlay,
      getPlayerType : getPlayerType,
      getVideoUrl : getVideoUrl,
      getVideoUrl2 : getVideoUrl2,
      getCoverUrl : getCoverUrl,
      getLoadProgress : getLoadProgress,
      setLoadProgressPercent : setLoadProgressPercent,
      getSearchServiceEpisodeIdURL :  getSearchServiceEpisodeIdURL
    };
}());
