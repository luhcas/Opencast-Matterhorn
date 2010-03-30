/*global $, Videodisplay, Opencast, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
@namespace the global Opencast namespace engage
*/
Opencast.Watch = (function () {

  function onPlayerReady() {

    var mediaPackageId = Opencast.engage.getMediaPackageId();
    
    var restEndpoint = "xml/episode.xml";
    //var restEndpoint = "../../search/rest/episode?id=" + mediaPackageId;
   // restEndpoint = "http://video.lernfunk.de/REST/ws/episode?id="+mediaPackageId;

    $('#data').xslt(restEndpoint, "xsl/player-hybrid-download.xsl", function() {
      // some code to run after the mapping
      // set the title of the page
      document.title = "Opencast Matterhorn - Media Player - " + $('#oc-title').html();
      
      // set the title on the top of the player
      $('#oc_title').html($('#oc-title').html());
      
      // set date
      if(!($('#oc-creator').html() === "")){
        $('#oc_title_from').html(" by " + $('#oc-creator').html());
      }
      
      if($('#oc-date').html() === ""){
        $('#oc_title_from').html(" by " + $('#oc-creator').html());
      }
      else {
        $('#oc_title_from').html(" by " + $('#oc-creator').html() + " ("+$('#oc-date').html()+")");
      }
      // set the abstract
      $('#abstract').html($('#oc-abstract').html());
      
      // Get the video url
      var videoUrl = $('#oc-video-url').html();
      Opencast.Player.setMediaURL(videoUrl,"");

      // Set the caption
      Opencast.Player.setCaptionsURL('dfxp/matterhorn.dfxp.xml');
      
      // set embed field
      var watchUrl = window.location.href;
      var embedUrl = watchUrl.replace(/watch.html/g, "embed.html")
//      var embed = $('#oc-embed').val().replace(/src_url/g, embedUrl);
//      $('#oc-embed').val(embed);
      
      
      
      
   
      
      Opencast.Scrubber.init();
      
 //     $('#info').append("<a href=" + restEndpoint + ">XML</a>&nbsp;");
      
 //     $('#info').append("<a href=" + watchUrl.replace(/watch.html/g, "multi.html") + ">Multi</a>");

      
    });
  }
  
  function hoverSegment(segmentId){
    
    $("#"+segmentId).toggleClass("segment-holder");
    $("#"+segmentId).toggleClass("segment-holder-over");
    
  }
  
  function seekSegment(seconds){
	// Opencast.Player.setPlayhead(seconds);
    var eventSeek = Videodisplay.seek(seconds);
  }

  return {
      onPlayerReady : onPlayerReady,
      hoverSegment : hoverSegment,
      seekSegment : seekSegment
    };
}());