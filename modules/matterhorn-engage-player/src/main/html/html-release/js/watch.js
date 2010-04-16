/*global $, Videodisplay, Opencast, fluid*/
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
    
        var restEndpoint = "xml/episode.xml";
        //var restEndpoint = "../../search/rest/episode?id=" + mediaPackageId;
        // restEndpoint = "http://video.lernfunk.de/REST/ws/episode?id="+mediaPackageId;

        $('#data').xslt(restEndpoint, "xsl/player-hybrid-download.xsl", function () 
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
            //var mediaUrlOne = $('#oc-video-url').html();
            //var mediaUrlTwo = $('#oc-video-url').html();
            
            // Get the video url
            //var mediaUrlOne = 'rtmp://cp67126.edgefcs.net/ondemand/mediapm/strobe/content/test/SpaceAloneHD_sounas_640_500_short';
            //var mediaUrlOne = 'http://mediapm.edgesuite.net/osmf/content/test/train_1500.mp3';
            var mediaUrlOne = 'http://mediapm.edgesuite.net/strobe/content/test/elephants_dream_768x428_24_short.flv';
            var mediaUrlTwo = 'rtmp://cp67126.edgefcs.net/ondemand/mediapm/strobe/content/test/SpaceAloneHD_sounas_640_500_short';
            //var mediaUrlTwo = 'http://mediapm.edgesuite.net/strobe/content/test/elephants_dream_768x428_24_short.flv';

            //var mediaUrlTwo = '';
            
           
            

            Opencast.Player.setMediaURL(mediaUrlOne, mediaUrlTwo);

            //
            if (mediaUrlOne !== '' && mediaUrlTwo !== '')
            {
                Opencast.Player.setVideoSizeList(MULTIPLAYER);
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