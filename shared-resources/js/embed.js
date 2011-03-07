/*global $, Videodisplay, Opencast, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
 * @namespace the global Opencast namespace watch
 */
Opencast.Watch = (function ()
{
    var MULTIPLAYER = "Multiplayer",
        SINGLEPLAYER = "Singleplayer",
        SINGLEPLAYERWITHSLIDES = "SingleplayerWithSlides",
        AUDIOPLAYER = "Audioplayer",
        PLAYERSTYLE = "embedPlayer",
        mediaResolutionOne = "",
        mediaResolutionTwo = "",
        mediaUrlOne = "",
        mediaUrlTwo = "",
        mimetypeOne = "",
        mimetypeTwo = "",
        coverUrlOne = "",
        coverUrlTwo = "",
        slideLength = 0,
        timeoutTime = 400,
        duration = 0,
        mediaPackageIdAvailable = true;
    
    /**
     * @memberOf Opencast.Watch
     * @description Sets up the Plugins if a MediaPackage-ID is available, otherwise continues the Processing
     */
    function onPlayerReady()
    {
        var mediaPackageId = Opencast.Utils.getURLParameter('id');
        var userId = Opencast.Utils.getURLParameter('user');
        if (mediaPackageId === null)
        {
            mediaPackageIdAvailable = false;
        }
        var restEndpoint = Opencast.engage.getSearchServiceEpisodeIdURL() + mediaPackageId;
        restEndpoint = Opencast.Utils.getURLParameter('videoUrl') !== null ? "preview.xml" : restEndpoint;
        Opencast.Player.setSessionId(Opencast.engage.getCookie("JSESSIONID"));
        Opencast.Player.setUserId(userId);
        if (mediaPackageIdAvailable)
        {
            // Set MediaPackage ID's in the Plugins
            Opencast.Player.setMediaPackageId(mediaPackageId);
            Opencast.Series.setMediaPackageId(mediaPackageId);
            Opencast.Description.setMediaPackageId(mediaPackageId);
            Opencast.segments_ui.setMediaPackageId(mediaPackageId);
            Opencast.segments.setMediaPackageId(mediaPackageId);
            Opencast.segments_text.setMediaPackageId(mediaPackageId);
            // Initialize Segments UI
            Opencast.segments_ui.initialize();
        }
        else
        {
            continueProcessing();
        }
    }
    
    /**
     * @memberOf Opencast.Watch
     * @description Sets up the html page after the player and the Plugins have been initialized.
     * @param error flag if error occured (=> display nothing, hide initialize); optional: set only if an error occured
     */
    function continueProcessing(error)
    {
        var err = error||false;
        if(error)
        {
            $('body').css('background-color', '#FFFFFF');
            $('body').html('<span id="initializing-matter">matter</span><span id="initializing-horn">horn</span><span id="initializing">&nbsp;The media is not available.</span>');
            $('#initializing').css('color', '#000000');
            return;
        }
        
        $('#oc-segments').html("");
        $(".segments").css("margin-top", "-3px");
        mimetypeOne = "video/x-flv";
        mimetypeTwo = "video/x-flv";
        // set the media URLs
        mediaUrlOne = Opencast.Utils.getURLParameter('videoUrl');
        mediaUrlTwo = Opencast.Utils.getURLParameter('videoUrl2');
        coverUrlOne = $('#oc-cover-presenter').html();
        coverUrlTwo = $('#oc-cover-presentation').html();
        
        // If URL Parameter display exists and is set to revert
        var display = Opencast.Utils.getURLParameter('display');
        if((display != null) && (display.toLowerCase() == 'invert'))
        {
            // Switch the displays and its covers
            var tmpMediaURLOne = mediaUrlOne;
            var tmpCoverURLOne = coverUrlOne;
            var tmpMimetypeOne = mimetypeOne;
            mediaUrlOne = mediaUrlTwo;
            coverUrlOne = coverUrlTwo;
            mimetypeOne = mimetypeTwo;
            mediaUrlTwo = tmpMediaURLOne;
            coverUrlTwo = tmpCoverURLOne;
            mimetypeTwo = tmpMimetypeOne;
        }
        
        if (coverUrlOne === null)
        {
            coverUrlOne = coverUrlTwo;
            coverUrlTwo = '';
        }
        if (mediaUrlOne === null) $('#oc-link-advanced-player').css("display", "inline");
        if (mediaUrlOne === null)
        {
            mediaUrlOne = $('#oc-video-presenter-delivery-x-flv-rtmp').html();
            mediaResolutionOne = $('#oc-resolution-presenter-delivery-x-flv-rtmp').html();
            mimetypeOne = $('#oc-mimetype-presenter-delivery-x-flv-rtmp').html();
        }
        if (mediaUrlTwo === null)
        {
            mediaUrlTwo = $('#oc-video-presentation-delivery-x-flv-rtmp').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-delivery-x-flv-rtmp').html();
            mimetypeTwo = $('#oc-mimetype-presentation-delivery-x-flv-rtmp').html();
        }
        if (mediaUrlOne === null)
        {
            mediaUrlOne = $('#oc-video-presenter-delivery-x-flv-http').html();
            mediaResolutionOne = $('#oc-resolution-presenter-delivery-x-flv-http').html();
            mimetypeOne = $('#oc-mimetype-presenter-delivery-x-flv-http').html();
        }
        if (mediaUrlOne === null)
        {
            mediaUrlOne = $('#oc-video-presenter-source-x-flv-rtmp').html();
            mediaResolutionOne = $('#oc-resolution-presenter-source-x-flv-rtmp').html();
            mimetypeOne = $('#oc-mimetype-presenter-source-x-flv-rtmp').html();
        }
        if (mediaUrlOne === null)
        {
            mediaUrlOne = $('#oc-video-presenter-source-x-flv-http').html();
            mediaResolutionOne = $('#oc-resolution-presenter-source-x-flv-http').html();
            mimetypeOne = $('#oc-mimetype-presenter-source-x-flv-http').html();
        }
        if (mediaUrlTwo === null)
        {
            mediaUrlTwo = $('#oc-video-presentation-delivery-x-flv-http').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-delivery-x-flv-http').html();
            mimetypeTwo = $('#oc-mimetype-presentation-delivery-x-flv-http').html();
        }
        if (mediaUrlTwo === null)
        {
            mediaUrlTwo = $('#oc-video-presentation-source-x-flv-rtmp').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-source-x-flv-rtmp').html();
            mimetypeTwo = $('#oc-mimetype-presentation-source-x-flv-rtmp').html();
        }
        if (mediaUrlTwo === null)
        {
            mediaUrlTwo = $('#oc-video-presentation-source-x-flv-http').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-source-x-flv-http').html();
            mimetypeTwo = $('#oc-mimetype-presentation-source-x-flv-http').html();
        }
        if (mediaUrlOne === null)
        {
            mediaUrlOne = mediaUrlTwo;
            mediaUrlTwo = null;
            mediaResolutionOne = mediaResolutionTwo;
            mediaResolutionTwo = null;
            mimetypeOne = mimetypeTwo;
            mimetypeTwo = null;
        }
        mediaUrlOne = mediaUrlOne === null ? '' : mediaUrlOne;
        mediaUrlTwo = mediaUrlTwo === null ? '' : mediaUrlTwo;
        mediaResolutionOne = mediaResolutionOne === null ? '' : mediaResolutionOne;
        mediaResolutionTwo = mediaResolutionTwo === null ? '' : mediaResolutionTwo;
        if (mediaPackageIdAvailable)
        {
            // Initialize the Segements
            Opencast.segments.initialize();
            slideLength = Opencast.segments.getSlideLength();
        }
        else
        {
            slideLength = 0;
        }
        Opencast.Player.setMediaURL(coverUrlOne, coverUrlTwo, mediaUrlOne, mediaUrlTwo, mimetypeOne, mimetypeTwo, PLAYERSTYLE, slideLength);
        if (mediaUrlOne !== '' && mediaUrlTwo !== '')
        {
            Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
            Opencast.Player.setVideoSizeList(SINGLEPLAYERWITHSLIDES);
            Opencast.Player.videoSizeControlMultiOnlyLeftDisplay();
        }
        else if (mediaUrlOne !== '' && mediaUrlTwo === '')
        {
            var pos = mediaUrlOne.lastIndexOf(".");
            var fileType = mediaUrlOne.substring(pos + 1);
            if (fileType === 'mp3')
            {
                Opencast.Player.setVideoSizeList(AUDIOPLAYER);
            }
            else
            {
                Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
                Opencast.Player.setVideoSizeList(SINGLEPLAYER);
            }
        }
        Opencast.Initialize.doResize();
        // Set the Caption
        // oc-captions using caption file generated by Opencaps
        var captionsUrl = $('#oc-captions').html();
        captionsUrl = captionsUrl === null ? '' : captionsUrl;
        Opencast.Player.setCaptionsURL(captionsUrl);
        // init the volume scrubber
        Opencast.Scrubber.init();
        // bind handler 
        $('#scrubber').bind('keydown', 'left', function (evt)
        {
            Opencast.Player.doRewind();
        });
        $('#scrubber').bind('keyup', 'left', function (evt)
        {
            Opencast.Player.stopRewind();
        });
        $('#scrubber').bind('keydown', 'right', function (evt)
        {
            Opencast.Player.doFastForward();
        });
        $('#scrubber').bind('keyup', 'right', function (evt)
        {
            Opencast.Player.stopFastForward();
        });
        getClientShortcuts();
        
        // Hide loading indicators
        $('#oc_flash-player-loading').hide();
        // Show video controls and data
        $('#data').show();
        
        // Set Duration
        var durDiv = $('#dc-extent').html();
        if((durDiv !== undefined) && (durDiv !== null) && (durDiv != ''))
        {
            duration = parseInt(parseInt(durDiv) / 1000);
            if((!isNaN(duration)) && (duration > 0))
            {
                Opencast.Player.setDuration(duration);
            }
        }
        Opencast.Player.setTotalTime(Opencast.Utils.formatSeconds(Opencast.Player.getDuration()));
        // Set seconds or autoplay
        durationSet();
    }
    
    /**
     * @memberOf Opencast.Watch
     * @description Checks and executes the URL Parameters 't' and 'play'
     *              Callback if duration time has been set
     */
    function durationSet()
    {
        var playParam = Opencast.Utils.getURLParameter('play');
        var timeParam = Opencast.Utils.getURLParameter('t');
        var durTextSet = ($('#oc_duration').text() != 'Initializing');
        
        var autoplay = (playParam !== null) && (playParam.toLowerCase() == 'true');
        var time = (timeParam === null) ? 0 : Opencast.Utils.parseSeconds(timeParam);
        time = (time < 0) ? 0 : time;
        
        var rdy = false;
        // duration set
        if(durTextSet)
        {
            // autoplay and jump to time OR autoplay and not jump to time
            if((autoplay && (time != 0 )) || (autoplay && (time == 0 )))
            {
                // attention: first call 'play', after that 'jumpToTime', otherwise nothing happens!
                if(Opencast.Player.doPlay() && jumpToTime(time))
                {
                    rdy = true;
                }
            }
            // not autoplay and jump to time
            else if(!autoplay && (time != 0 ))
            {
                if(jumpToTime(time))
                {
                    rdy = true;
                }
            }
            // not autoplay and not jump to time
            else
            {
                rdy = true;
            }
        } else
        {
            rdy = false;
        }
        
        if(!rdy)
        {
            // If duration time not set, yet: set a timeout and call again
            setTimeout(function()
            {
                Opencast.Watch.durationSet();
            }, timeoutTime);
        }
    }
    
    /**
     * @memberOf Opencast.Watch
     * @description tries to jump to a given time
     * @return true if successfully jumped, false else
     */
    function jumpToTime(time)
    {
        var success = false;
        try
        {
            Videodisplay.seek(time);
            success = true;
        } catch(err)
        {
            success = false;
        }
        return success;
    }
    
    /**
     * @memberOf Opencast.Watch
     * @description Toggles the class segment-holder-over
     * @param String segmentId the id of the segment
     */
    function hoverSegment(segmentId)
    {
        $("#" + segmentId).toggleClass("segment-holder");
        $("#" + segmentId).toggleClass("segment-holder-over");
        var index = parseInt(segmentId.substr(7)) - 1;
        var imageHeight = 120;
        //if ($.browser.msie) {
        //  imageHeight = 30;
        //}
        $("#segment-tooltip").html('<img src="' + Opencast.segments.getSegmentPreview(index) + '" height="' + imageHeight + '"/>');
        var segmentLeft = $("#" + segmentId).offset().left;
        var segmentTop = $("#" + segmentId).offset().top;
        var segmentWidth = $("#" + segmentId).width();
        var tooltipWidth = $("#segment-tooltip").width();
        var posLeft = segmentLeft + segmentWidth / 2 - tooltipWidth / 2;
        posLeft = posLeft < 0 ? 0 : posLeft;
        posLeft = posLeft > ($("#oc_seek-slider").width() - tooltipWidth - 10) ? ($("#oc_seek-slider").width() - tooltipWidth - 10) : posLeft;
        $("#segment-tooltip").css("left", posLeft + "px");
        $("#segment-tooltip").css("top", segmentTop - (imageHeight + 7) + "px");
        $("#segment-tooltip").show();
    }
    
    /**
     * @memberOf Opencast.Watch
     * @description Toggles the class segment-holder-over
     * @param String segmentId the id of the segment
     */
    function hoverOutSegment(segmentId)
    {
        $("#" + segmentId).toggleClass("segment-holder");
        $("#" + segmentId).toggleClass("segment-holder-over");
        $("#segment-tooltip").hide();
    }
    
    /**
     * @memberOf Opencast.Watch
     * @description Seeks the video to the passed position. Is called when the user clicks on a segment
     * @param int seconds the position in the video
     */
    function seekSegment(seconds)
    {
        var eventSeek = Videodisplay.seek(seconds);
    }
    
    /**
     * @memberOf Opencast.Watch
     * @description Gets the OS-specific shortcuts of the client
     */
    function getClientShortcuts()
    {
        $('#oc_client_shortcuts').append("Control + Alt + I = Toggle the keyboard shortcuts information between show or hide.<br/>");
        $('#oc_client_shortcuts').append("Control + Alt + P = Toggle the video between pause or play.<br/>");
        $('#oc_client_shortcuts').append("Control + Alt + S = Stop the video.<br/>");
        $('#oc_client_shortcuts').append("Control + Alt + M = Toggle between mute or unmute the video.<br/>");
        $('#oc_client_shortcuts').append("Control + Alt + U = Volume up<br/>");
        $('#oc_client_shortcuts').append("Control + Alt + D = Volume down<br/>");
        $('#oc_client_shortcuts').append("Control + Alt 0 - 9 = Seek the time slider<br/>");
        $('#oc_client_shortcuts').append("Control + Alt + C = Toggle between captions on or off.<br/>");
        $('#oc_client_shortcuts').append("Control + Alt + F = Forward the video.<br/>");
        $('#oc_client_shortcuts').append("Control + Alt + R = Rewind the video.<br/>");
        $('#oc_client_shortcuts').append("Control + Alt + T = the current time for the screen reader<br/>");
        switch ($.client.os)
        {
        case "Windows":
            $('#oc_client_shortcuts').append("Windows Control + = to zoom in the player<br/>");
            $('#oc_client_shortcuts').append("Windows Control - = to minimize in the player<br/>");
            break;
        case "Mac":
            $('#oc_client_shortcuts').append("cmd + = to zoom in the player<br/>");
            $('#oc_client_shortcuts').append("cmd - = to minimize the player<br/>");
            break;
        case "Linux":
            break;
        }
    }
    
    return {
        continueProcessing: continueProcessing,
        onPlayerReady: onPlayerReady,
        hoverSegment: hoverSegment,
        durationSet: durationSet,
        hoverOutSegment: hoverOutSegment,
        seekSegment: seekSegment,
        getClientShortcuts: getClientShortcuts
    };
}());