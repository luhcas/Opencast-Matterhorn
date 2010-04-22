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
    
        var restEndpoint = "../../search/rest/episode?id=" + mediaPackageId;

        //var restEndpoint = "xml/episode.xml";
        
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
      
            // set the media URLs
            var mediaUrlOne = $('#oc-video-url').html();
            var mediaUrlTwo = $('#oc-video-url').html();
            
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
            
            // init
            Opencast.Initialize.init();
                       
            $('#scrubber').bind('keydown', 'left', function(evt) 
            {
                var newPosition = Math.round((($("#draggable").position().left - 20 ) / $("#scubber-channel").width()) * Opencast.Player.getDuration());
                Videodisplay.seek(newPosition);
            });
            
            $('#scrubber').bind('keydown', 'right', function(evt)
            {
                var newPosition = Math.round((($("#draggable").position().left + 20 ) / $("#scubber-channel").width()) * Opencast.Player.getDuration());
                Videodisplay.seek(newPosition);            
                
            });
            
         // set description html text
            $('#oc_description-sections').html('description');
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