/*global $, Videodisplay, Opencast, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
@namespace the global Opencast namespace engage
*/
Opencast.Watch = (function () {

  function onPlayerReady(playerId) {

    var mediaPackageId = Opencast.engage.getMediaPackageId();
    
    var restEndpoint = "../../search/rest/episode?id=" + mediaPackageId;

    $('#data').xslt(restEndpoint, "xsl/player-hybrid-download.xsl", function() {
      // some code to run after the mapping
      // set the title of the page
      document.title = "Opencast Matterhorn - Media Player - " + $('#oc-title').html();
      
      // set the title on the top of the player
      if($('#oc-creator').html() === ""){
        
      $('#stage-title').html("<h2>"+ $('#oc-title').html()+ "</h2>");
      }
      else{
        $('#stage-title').html("<h2>"+ $('#oc-title').html() + " by " + $('#oc-creator').html() + "</h2>");
      }
      
      // set the abstract
      $('#abstract').html($('#oc-abstract').html());

      // Get the video url
      var videoUrl = $('#oc-video-url').html();
      Opencast.Player.setMediaURL(videoUrl);

      // Set the caption
      Opencast.Player.setCaptionsURL('dfxp/matterhorn.dfxp.xml');
      
      // set embed field
      var watchUrl = window.location.href;
      var embedUrl = watchUrl.replace(/watch.html/g, "embed.html")
      var embed = $('#oc-embed').val().replace(/src_url/g, embedUrl);
      $('#oc-embed').val(embed);
      
      
      $('#info').append("<a href=" + restEndpoint + ">XML</a>&nbsp;");
      
      $('#info').append("<a href=" + watchUrl.replace(/watch.html/g, "multi.html") + ">Multi</a>");

      
    });

    
    Opencast.ariaSlider.init();
    $(document).ariaParse();
    var simpleEdit = fluid.inlineEdit("#simpleEdit", {
      selectors : {
        text: ".editableText",
        editContainer: "#editorContainer",
        edit: "#editField"
      },
      useTooltip : true,
      tooltipDelay : 500
    });

    $("#editorContainer").attr("className", "oc_editTime");
    $("#editField").attr("className", "oc_editTime");
    
    $("#btn_cc").attr('role', 'button');
    $("#btn_cc").attr('aria-pressed', 'false'); 

    $("#btn_volume").attr('role', 'button');
    $("#btn_volume").attr('aria-pressed', 'false');

    $("#btn_play_pause").attr('role', 'button');
    $("#btn_play_pause").attr('aria-pressed', 'false');

    $("#btn_skip_backward").attr('role', 'button');
    $("#btn_skip_backward").attr('aria-labelledby', 'Skip Backward');

    $("#btn_rewind").attr('role', 'button');
    $("#btn_rewind").attr('aria-labelledby', 'Rewind');

    $("#btn_fast_forward").attr('role', 'button');
    $("#btn_fast_forward").attr('aria-labelledby', 'Fast Forward');

    $("#btn_skip_forward").attr('role', 'button');
    $("#btn_skip_forward").attr('aria-labelledby', 'Skip Forward');
  }
    
    return {
      onPlayerReady : onPlayerReady
    };
}());