/*global $, Videodisplay, Opencast, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
 @namespace the global Opencast namespace watch
 */
Opencast.Watch = (function ()
{
    var MULTIPLAYER = "Multiplayer",
        SINGLEPLAYER = "Singleplayer",
        SINGLEPLAYERWITHSLIDES = "SingleplayerWithSlides",
        AUDIOPLAYER = "Audioplayer",
        ADVANCEDPLAYER = "advancedPlayer",
        PLAYERSTYLE = "embedPlayer",
        mediaResolutionOne = "",
        mediaResolutionTwo = "",
        mediaUrlOne = "",
        mediaUrlTwo = "",
        mimetypeOne = "",
        mimetypeTwo = "",
        coverUrlOne = "",
        coverUrlTwo = "";
    var time = 0;
    var interval;
    var intervalOn = false;
    var mediaPackageIdAvailable = true;
    
    /**
     * @memberOf Opencast.Watch
     * @description Sets up the Plugins if a MediaPackage-ID is available, otherwise continues the Processing
     */
    function onPlayerReady()
    {
        var mediaPackageId = Opencast.engage.getMediaPackageId();
        var userId = Opencast.engage.getUserId();
        if (mediaPackageId === null)
        {
            mediaPackageIdAvailable = false;
        }
        var restEndpoint = Opencast.engage.getSearchServiceEpisodeIdURL() + mediaPackageId;
        restEndpoint = Opencast.engage.getVideoUrl() !== null ? "preview.xml" : restEndpoint;
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
     */
    function continueProcessing()
    {
        $('#oc-segments').html("");
        $(".segments").css("margin-top", "-3px");
        mimetypeOne = "video/x-flv";
        mimetypeTwo = "video/x-flv";
        // set the media URLs
        mediaUrlOne = Opencast.engage.getVideoUrl();
        mediaUrlTwo = Opencast.engage.getVideoUrl2();
        coverUrlOne = $('#oc-cover-presenter').html();
        coverUrlTwo = $('#oc-cover-presentation').html();
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
            //
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
        // Opencast.search.initialize();
        // Parse URL Parameters (time 't') and jump to the given Seconds
        time = Opencast.Utils.parseSeconds(Opencast.Utils.getURLParameter('t'));
        if (!intervalOn)
        {
            interval = setInterval(forwardSeconds, 250);
            intervalOn = true;
        }
    }
    
    /**
     * @memberOf Opencast.Watch
     * @description Tries to forward to given Seconds if Player ready and Second set
     */
    function forwardSeconds()
    {
        if (($('#oc_duration').text() != "NaN:NaN:NaN") && ($('#oc_duration').text() != ""))
        {
            // Videodisplay.play();
            Videodisplay.seek(parseInt(time));
            if (intervalOn)
            {
                clearInterval(interval);
                intervalOn = false;
            }
        }
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
        hoverOutSegment: hoverOutSegment,
        seekSegment: seekSegment,
        getClientShortcuts: getClientShortcuts
    };
}());
