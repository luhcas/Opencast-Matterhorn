/*global $, Videodisplay, window, Opencast, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
@namespace the global Opencast namespace engage
*/
Opencast.Watch = (function () 
{

    function onPlayerReady() 
    {
        var MULTIPLAYER			   = "Multiplayer",
            SINGLEPLAYER		   = "Singleplayer",
            SINGLEPLAYERWITHSLIDES = "SingleplayerWithSlides",
            AUDIOPLAYER			   = "Audioplayer",
            ADVANCEDPLAYER         = "advancedPlayer",
            EMBEDPLAYER            = "embedPlayer";

        var mediaPackageId = Opencast.engage.getMediaPackageId();
    
        var restEndpoint = "../../search/rest/episode?id=" + mediaPackageId;

        $('#data').xslt(restEndpoint, "xsl/embed-hybrid-player.xsl", function () 
        {
            // some code to run after the mapping
            // set the title of the page
            document.title = "Opencast Matterhorn - Media Player - " + $('#oc-title').html();
      
            // set the title on the top of the player
            $('#oc_title').html($('#oc-title').html());
      
            // set date
            if (!($('#oc-creator').html() === ""))
            {
                $('#oc_title_from').html(" by " + $('#oc-creator').html());
            }
      
            if ($('#oc-date').html() === "")
            {
                $('#oc_title_from').html(" by " + $('#oc-creator').html());
            }
            else 
            {
                $('#oc_title_from').html(" by " + $('#oc-creator').html() + " (" + $('#oc-date').html() + ")");
            }
            // set the abstract
            $('#abstract').html($('#oc-abstract').html());
      
            // TODO MERGE
            var mediaUrlOne = $('#oc-video-url').html();
            var mediaUrlTwo = $('#oc-video-url').html();
            
            // Get the video url
            //var mediaUrlOne = 'rtmp://freecom.serv.uni-osnabrueck.de/oflaDemo/algorithmen09_2009_10_27_14_9__131_173_10_32.flv';
            //var mediaUrlOne = 'http://mediapm.edgesuite.net/osmf/content/test/train_1500.mp3';
            //var mediaUrlOne = 'engage-hybrid-player/mp3/need.mp3';
            // var mediaUrlTwo = 'rtmp://freecom.serv.uni-osnabrueck.de/oflaDemo/algorithmen09_2009_10_27_14_9__131_173_10_32.flv';
            //var mediaUrlTwo = '';

            Opencast.Player.setMediaURL(mediaUrlOne, mediaUrlTwo);
            //
            if (mediaUrlOne !== '' && mediaUrlTwo !== '')
            {
                Opencast.Player.setVideoSizeList(SINGLEPLAYERWITHSLIDES);
                Opencast.Player.videoSizeControlMultiOnlyLeftDisplay();
            }
            else if (mediaUrlOne !== '' && mediaUrlTwo === '')
            {
                var pos = mediaUrlOne.lastIndexOf(".");
                var fileType = mediaUrlOne.substring(pos + 1);
                //
                if (fileType === 'mp3')
                {
                    Opencast.Player.setVideoSizeList(AUDIOPLAYER);
                }
                else
                {
                    Opencast.Player.setVideoSizeList(SINGLEPLAYER);
                }
            }
      
            // Set the caption
            Opencast.Player.setCaptionsURL('engage-hybrid-player/dfxp/matterhorn.dfxp.xml');
      
            // set embed field
            var watchUrl = window.location.href;
            var embedUrl = watchUrl.replace(/watch.html/g, "embed.html");
            //var embed = $('#oc-embed').val().replace(/src_url/g, embedUrl);
            //$('#oc-embed').val(embed);
      
            // init the volume scrubber
            Opencast.Scrubber.init();
      
            //$('#info').append("<a href=" + restEndpoint + ">XML</a>&nbsp;");
      
            //$('#info').append("<a href=" + watchUrl.replace(/watch.html/g, "multi.html") + ">Multi</a>");

      
        });
    }
  
    function hoverSegment(segmentId)
    {
    
        $("#" + segmentId).toggleClass("segment-holder");
        $("#" + segmentId).toggleClass("segment-holder-over");
    
    }
  
    function seekSegment(seconds)
    {
        // Opencast.Player.setPlayhead(seconds);
        var eventSeek = Videodisplay.seek(seconds);
    }
    return {
        onPlayerReady : onPlayerReady,
        hoverSegment : hoverSegment,
        seekSegment : seekSegment
    };
}());