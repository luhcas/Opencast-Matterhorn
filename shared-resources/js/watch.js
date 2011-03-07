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
        duration = 0;
        
    /**
     * @memberOf Opencast.Watch
     * @description Parses a query string
     */
    function parseQueryString(qs){
        var urlParams = {};
        var currentUrl = window.location.href;
        var email_message = "mailto:?subject=I recommend you to take a look at this Opencast video&body=Please have a look at: "
        document.getElementById("oc_btn-email").setAttribute("href", email_message + currentUrl);
        var e, d = function(s){
            return decodeURIComponent(s.replace(/\+/g, " "));
        }, q = window.location.search.substring(1), r = /([^&=]+)=?([^&]*)/g;
        while (e = r.exec(q))
        {
            urlParams[d(e[1])] = d(e[2]);
        }
        return urlParams;
    }
    
    /**
     * @memberOf Opencast.Watch
     * @description Sets up the Plugins
     */
    function onPlayerReady()
    {
        // Hide Screen Settings until clicked 'play'
        $("#oc_btn-dropdown").css("display", 'none');
        $("#oc_player_video-dropdown").css("display", 'none');
        
        var mediaPackageId = Opencast.Utils.getURLParameter('id');
        var userId = Opencast.Utils.getURLParameter('user');
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
     * @memberOf Opencast.Watch
     * @description Sets up the html page after the player and the Plugins have been initialized.
     * @param error flag if error occured (=> display nothing, hide initialize); optional: set only if an error occured
     */
    function continueProcessing(error)
    {
        var err = error||false;
        if(error)
        {
            $('#oc_Videodisplay').hide();
            $('#initializing').html('The media is not available.');
            $('#oc_flash-player-loading').css('width', '60%');
            $('#loading-init').hide();
            return;
        }
        
        // set the title of the page
        document.title = $('#oc-title').html() + " | Opencast Matterhorn - Media Player";
        var dcExtent = parseInt($('#dc-extent').html());
        Opencast.Analytics.setDuration(parseInt(parseInt(dcExtent) / 1000));
        Opencast.Analytics.initialize();
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
        // Segments Text View
        $('.segments-time').each(function ()
        {
            var seconds = $(this).html();
            $(this).html(Opencast.Utils.formatSeconds(seconds));
        });
        // Set the Controls visible
        $('#oc_video-player-controls').show();
        
        // Hide loading indicators
        $('#oc_flash-player-loading').hide();
        
        if(parseQueryString(window.location.search.substring(1)).embed) {
            $('#oc_title-bar').hide();
            $('#oc_btn-embed').hide();
            $('#oc_slidetext').addClass('scroll');
        }
        // Show video controls and data
        $('#oc_player_video-dropdown').show();
        $('#data').show();
        $('#oc_player-head-right').show();
        $('#oc_ui_tabs').show();
        
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
        $('#oc_client_shortcuts').append('<a href="javascript: " id="oc_btn-leave_shortcut" onclick="$(\'#oc_shortcut-button\').trigger(\'click\');" class="handcursor ui-helper-hidden-accessible" title="Leave shortcut dialog" role="button">Leave embed dialog</a>');
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
        durationSet: durationSet,
        getClientShortcuts: getClientShortcuts
    };
}());