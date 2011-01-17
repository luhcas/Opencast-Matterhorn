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
        PLAYERSTYLE = "advancedPlayer",
        mediaUrlOne = "",
        mediaUrlTwo = "",
        mimetypeOne = "",
        mimetypeTwo = "",
        mediaResolutionOne = "",
        mediaResolutionTwo = "",
        coverUrlOne = "",
        coverUrlTwo = "",
        slideLength = 0;
    var time = 0;
    var interval;
    var intervalOn = false;
            
    /**
     * @memberOf Opencast.Watch
     * @description Sets up the html page after the player has been initialized.
     *              The XSL files are loaded.
     */
    function onPlayerReady()
    {
        var mediaPackageId = Opencast.engage.getMediaPackageId();
        var userId = Opencast.engage.getUserId();
        var restEndpoint = Opencast.engage.getSearchServiceEpisodeIdURL() + mediaPackageId;
        Opencast.Player.setSessionId(Opencast.engage.getCookie("JSESSIONID"));
        Opencast.Player.setUserId(userId);
        
        // Set MediaPackage ID's in the Plugins
        Opencast.Player.setMediaPackageId(mediaPackageId);
        Opencast.Annotation_Chapter.setMediaPackageId(mediaPackageId);
        Opencast.Analytics.setMediaPackageId(mediaPackageId);
        Opencast.Series.setMediaPackageId(mediaPackageId);
        Opencast.Description.setMediaPackageId(mediaPackageId);
        Opencast.segments_ui.setMediaPackageId(mediaPackageId);
        Opencast.segments.setMediaPackageId(mediaPackageId);
        Opencast.segments_text.setMediaPackageId(mediaPackageId);
        Opencast.search.setMediaPackageId(mediaPackageId);
        
        // Initialize Segments UI
        Opencast.segments_ui.initialize();
    }

    /**
     * Continues initialization
     */
    function continueProcessing()
    {
        // set the title of the page
        document.title = $('#oc-title').html() + " | Opencast Matterhorn - Media Player";
        var dcExtent = parseInt($('#dc-extent').html());
        Opencast.Analytics.setDuration(parseInt(parseInt(dcExtent) / 1000));
        Opencast.Annotation_Chapter.setDuration(parseInt(parseInt(dcExtent) / 1000));
        Opencast.Annotation_Chapter.initialize();
        $('#oc_body').bind('resize', function ()
        {
            Opencast.AnalyticsPlugin.resizePlugin();
        });
        $('#oc_segment-table').html($('#oc-segments').html());
        $('#oc-segments').html("");
        // set the media URLs
        mediaUrlOne = $('#oc-video-presenter-delivery-x-flv-rtmp').html();
        mediaUrlTwo = $('#oc-video-presentation-delivery-x-flv-rtmp').html();
        mediaResolutionOne = $('#oc-resolution-presenter-delivery-x-flv-rtmp').html();
        mediaResolutionTwo = $('#oc-resolution-presentation-delivery-x-flv-rtmp').html();
        // set default mimetypes
        mimetypeOne = "video/x-flv";
        mimetypeTwo = "video/x-flv";
        // mimetypeOne = "audio/x-flv";
        // mimetypeTwo = "audio/x-flv";
        coverUrlOne = $('#oc-cover-presenter').html();
        coverUrlTwo = $('#oc-cover-presentation').html();
        if (coverUrlOne === null)
        {
            coverUrlOne = coverUrlTwo;
            coverUrlTwo = '';
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
        coverUrlOne = coverUrlOne === null ? '' : coverUrlOne;
        coverUrlTwo = coverUrlTwo === null ? '' : coverUrlTwo;
        mimetypeOne = mimetypeOne === null ? '' : mimetypeOne;
        mimetypeTwo = mimetypeTwo === null ? '' : mimetypeTwo;
        mediaResolutionOne = mediaResolutionOne === null ? '' : mediaResolutionOne;
        mediaResolutionTwo = mediaResolutionTwo === null ? '' : mediaResolutionTwo;
        // init the segements
        Opencast.segments.initialize();
        // init the segements_text
        Opencast.segments_text.initialize();
        slideLength = Opencast.segments.getSlideLength();
        
        Opencast.Player.setMediaURL(coverUrlOne, coverUrlTwo, mediaUrlOne, mediaUrlTwo, mimetypeOne, mimetypeTwo, PLAYERSTYLE, slideLength);
        
        if (mediaUrlOne !== '' && mediaUrlTwo !== '')
        {
            Opencast.Player.setVideoSizeList(MULTIPLAYER);
            Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
        }
        else if (mediaUrlOne !== '' && mediaUrlTwo === '')
        {
            var pos = mimetypeOne.lastIndexOf("/");
            var fileType = mimetypeOne.substring(0, pos);
            //
            if (fileType === 'audio')
            {
                Opencast.Player.setVideoSizeList(AUDIOPLAYER);
            }
            else
            {
                Opencast.Player.setVideoSizeList(SINGLEPLAYER);
                Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
            }
        }
        // Set the caption
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
        // init the search
        Opencast.search.initialize();
        Opencast.Bookmarks.initialize();
        getClientShortcuts();
        // init
        Opencast.Initialize.init();
        // **************************************
        // Segments Text View
        $('.segments-time').each(function ()
        {
            var seconds = $(this).html();
            $(this).html(Opencast.engage.formatSeconds(seconds));
        });
        // set the controls visible
        $('#oc_video-player-controls').show();
        
        // Parse URL Parameters
        time = Opencast.Utils.parseSeconds(Opencast.Utils.getURLParameter('t'));
        if(!intervalOn)
        {
            interval = setInterval(forwardSeconds, 250);
            intervalOn = true;
        }
    }
    
    /**
     * Tries to forward to given Seconds
     */
    function forwardSeconds()
    {
        if(($('#oc_duration').text() != "NaN:NaN:NaN") && ($('#oc_duration').text() != ""))
        {
            // Videodisplay.play();
            Videodisplay.seek(parseInt(time));
            if(intervalOn)
            {
                clearInterval(interval);
                intervalOn = false;
            }
        }
    }
    
    /**
     * @memberOf Opencast.Watch
     * @description Seeks the video to the passed position. Is called when the
     *              user clicks on a segment
     * @param int
     *          seconds the position in the video
     */
    function seekSegment(seconds)
    {
        // Opencast.Player.setPlayhead(seconds);
        var eventSeek = Videodisplay.seek(seconds);
    }
    
    /**
     * @memberOf Opencast.Watch
     * @description Gets the OS-specific shortcuts of the client
     */
    function getClientShortcuts()
    {
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + I = Toggle the keyboard shortcuts information between show or hide.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + P = Toggle the video between pause or play.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + S = Stop the video.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + M = Toggle between mute or unmute the video.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + U = Volume up</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + D = Volume down</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt 0 - 9 = Seek the time slider</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + C = Toggle between captions on or off.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + F = Forward the video.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + R = Rewind the video.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + T = the current time for the screen reader</span><br/>");
        $('#oc_client_shortcuts').append('<a href="javascript: " id="oc_btn-leave_shortcut" onclick="$(\'#oc_shortcuts\').dialog(\'close\');" class="handcursor ui-helper-hidden-accessible" title="Leave shortcut dialog" role="button">Leave embed dialog</a>');
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
        onPlayerReady: onPlayerReady,
        seekSegment: seekSegment,
        continueProcessing: continueProcessing,
        getClientShortcuts: getClientShortcuts
    };
}());
