/*global $, Player, Videodisplay, Scrubber*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
    @namespace the global Opencast namespace
*/
var Opencast = Opencast || {};

/**
    @namespace Opencast namespace Player
*/
Opencast.Player = (function () {

    /**
     * 
     * 
        global
     */
    var PLAYING           = "playing",
    PAUSING               = "pausing",
    PLAY                  = "Play: Control + Alt + P",
    PAUSE                 = "Pause: Control + Alt + P",
    CCON                  = "Closed Caption On: Control + Alt + C",
    CCOFF                 = "Closed Caption Off: Control + Alt + C",
    UNMUTE                = "Unmute: Control + Alt + M",
    MUTE                  = "Mute: Control + Alt + M",
    SLIDERVOLUME          = "slider_volume_Thumb",
    SLIDES                = "Slides",
    SLIDESHIDE            = "Hide Slides",
    NOTES                  = "Notes",
    NOTESHIDE             = "Hide Notes",
    SEARCH                = "Search",
    SEARCHHIDE            = "Hide Search",
    SHORTCUTS             = "Shortcuts",
    SHORTCUTSHIDE         = "Hide Shortcuts",
    EMBED                 = "Embed",
    EMBEDHIDE             = "Hide Embed",
    currentPlayPauseState = PAUSING,
    showSections          = true,
    mouseOverBool         = false,
    captionsBool          = false,
    dragging              = false,
    duration              = 0;
   
    /**
        @memberOf Opencast.Player
        @description Get the current play pause state.
     */
    function getCurrentPlayPauseState()
    {
        return currentPlayPauseState;
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the current play pause state.
        @param String state
     */
    function setCurrentPlayPauseState(state)
    {
        currentPlayPauseState = state;
    }

    /**
        @memberOf Opencast.Player
        @description Get the showSections.
     */
    function getShowSections()
    {
        return showSections;
    }

    /**
         @memberOf Opencast.Player
         @description Set the showSections.
         @param Boolean bool
     */
    function setShowSections(bool)
    {
        showSections = bool;
    } 
    
    /**
        @memberOf Opencast.Player
        @description Get the mouseOverBool.
     */
    function getMouseOverBool()
    {
        return mouseOverBool;
    }

    /**
        @memberOf Opencast.Player
        @description Set the mouseOverBool.
        @param Booelan bool
     */
    function setMouseOverBool(bool)
    {
        mouseOverBool = bool;
    }

    /**
        @memberOf Opencast.Player
        @description Get the captionsBool.
     */
    function getCaptionsBool()
    {
        return captionsBool;
    }

    /**
        @memberOf Opencast.Player
        @description Set the captionsBool.
        @param Booelan bool
     */
    function setCaptionsBool(bool)
    {
        captionsBool = bool;
    }
    
    /**
        @memberOf Opencast.Player
        @description Get the duration.
      */
    function getDuration()
    {
        return duration;
    }
    
    /**
        @memberOf Opencast.Player
        @description Get the dragging.
        @param Booelan bool
     */
    function getDragging(bool)
    {
        return dragging;
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the dragging.
        @param Booelan bool
     */
    function setDragging(bool)
    {
        dragging = bool;
    }
    
    /**
        @memberOf Opencast.Player
        @description Mouse over effect, change the css style.
     */
    function PlayPauseMouseOver() 
    {
        if (getCurrentPlayPauseState() === PLAYING) 
        {
            $("#oc_btn-play-pause").attr("className", "oc_btn-pause-over");
            setMouseOverBool(true);
        }
        else if (getCurrentPlayPauseState() === PAUSING) 
        {
            $("#oc_btn-play-pause").attr("className", "oc_btn-play-over");
            setMouseOverBool(true);
        }
    }

    /**
        @memberOf Opencast.Player
        @description Mouse out effect, change the css style.
     */
    function PlayPauseMouseOut() 
    {
        if (getCurrentPlayPauseState() === PLAYING) 
        {
            $("#oc_btn-play-pause").attr("className", "oc_btn-pause");
            setMouseOverBool(false);
        }
        else if (getCurrentPlayPauseState() === PAUSING) 
        {
            $("#oc_btn-play-pause").attr("className", "oc_btn-play");
            setMouseOverBool(false);
        }
    }

    /**
        @memberOf Opencast.Player
        @description Show the slides
     */
    function showSlides()
    {
        $("#oc_slides-sections").attr("className", "oc_slidesDisplayBlock");
        $("#oc_btn-slides").attr({ 
            alt: SLIDESHIDE,
            title: SLIDESHIDE
        });
        $("#oc_btn-slides").attr('aria-pressed', 'true');
    }
    
    /**
        @memberOf Opencast.Player
        @description Hide the slides
     */
    function hideSlides()
    {
        $("#oc_slides-sections").attr("className", "oc_DisplayNone");
        $("#oc_btn-slides").attr({ 
            alt: SLIDES,
            title: SLIDES
        });
        $("#oc_btn-slides").attr('aria-pressed', 'false');
    }

    /**
        @memberOf Opencast.Player
        @description Show the notes
     */
    function showNotes()
    {
        $("#oc_notes-sections").attr("className", "oc_notesDisplayBlock");
        $("#oc_btn-notes").attr({ 
            alt: NOTESHIDE,
            title: NOTESHIDE
        });
        $("#oc_btn-notes").attr('aria-pressed', 'true');
    }
    
    /**
        @memberOf Opencast.Player
        @description Hide the notes
     */
    function hideNotes()
    {
        $("#oc_notes-sections").attr("className", "oc_DisplayNone");
        $("#oc_btn-notes").attr({ 
            alt: NOTES,
            title: NOTES
        });
        $("#oc_btn-notes").attr('aria-pressed', 'false');
    }

    /**
        @memberOf Opencast.Player
        @description Show the search
     */
    function showSearch()
    {
        $("#oc_search-sections").attr("className", "oc_searchDisplayBlock");
        $("#oc_btn-search").attr({ 
            alt: SEARCHHIDE,
            title: SEARCHHIDE
        });
        $("#oc_btn-search").attr('aria-pressed', 'true');
    }
    
    /**
        @memberOf Opencast.Player
        @description Hide the search
     */
    function hideSearch()
    {
        $("#oc_search-sections").attr("className", "oc_DisplayNone");
        $("#oc_btn-search").attr({ 
            alt: SEARCH,
            title: SEARCH
        });
        $("#oc_btn-search").attr('aria-pressed', 'false');
    }

    /**
        @memberOf Opencast.Player
        @description Show the shortcuts
     */
    function showShortcuts()
    {
        $("#oc_shortcuts-sections").attr("className", "oc_shortcutsDisplayBlock");
        $("#oc_btn-shortcuts").attr({ 
            alt: SHORTCUTSHIDE,
            title: SHORTCUTSHIDE
        });
        $("#oc_btn-shortcuts").attr('aria-pressed', 'true');
    }
    
    /**
        @memberOf Opencast.Player
        @description Hide the shortcuts
     */
    function hideShortcuts()
    {
        $("#oc_shortcuts-sections").attr("className", "oc_DisplayNone");
        $("#oc_btn-shortcuts").attr({ 
            alt: SHORTCUTS,
            title: SHORTCUTS
        });
        $("#oc_btn-shortcuts").attr('aria-pressed', 'false');
    }

     /**
        @memberOf Opencast.Player
        @description Show the embed
     */
    function showEmbed()
    {
        $("#oc_embed-sections").attr("className", "oc_embedDisplayBlock");
        $("#oc_btn-embed").attr({ 
            alt: EMBEDHIDE,
            title: EMBEDHIDE
        });
        $("#oc_btn-embed").attr('aria-pressed', 'true');
    }
    
    /**
       @memberOf Opencast.Player
       @description Hide the embed
     */
    function hideEmbed()
    {
        $("#oc_embed-sections").attr("className", "oc_DisplayNone");
        $("#oc_btn-embed").attr({ 
            alt: EMBED,
            title: EMBED
        });
        $("#oc_btn-embed").attr('aria-pressed', 'false');
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle the slides
     */
    function doToggleSlides()
    {
        if ($("#oc_btn-slides").attr("title") === SLIDES)
        {
            showSlides();
            hideNotes();
            hideSearch();
            hideShortcuts();
            hideEmbed();
            setShowSections(true);
        }
        else
        {
            hideSlides();
            setShowSections(false);
        }
        Opencast.Initialize.doTest();
    }

    /**
        @memberOf Opencast.Player
        @description Toggle the slides
     */
    function doToggleNotes()
    {
        if ($("#oc_btn-notes").attr("title") === NOTES)
        {
            showNotes(); 
            hideSlides();
            hideSearch();
            hideShortcuts();
            hideEmbed();
            setShowSections(true);
        }
        else
        {
            hideNotes();
            setShowSections(false);
        }
        Opencast.Initialize.doTest();
    }

    /**
        @memberOf Opencast.Player
        @description Toggle the search
     */
    function doToggleSearch()
    {
        if ($("#oc_btn-search").attr("title") === SEARCH)
        {
            showSearch();
            hideNotes(); 
            hideSlides();
            hideShortcuts();
            hideEmbed();
            setShowSections(true);
        }
        else
        {
            hideSearch();
            setShowSections(false);
        }
        Opencast.Initialize.doTest();
    }

    /**
        @memberOf Opencast.Player
        @description Toggle the shortcuts
     */
    function doToggleShortcuts()
    {
        if ($("#oc_btn-shortcuts").attr("title") === SHORTCUTS)
        {
            showShortcuts();
            hideNotes(); 
            hideSlides();
            hideSearch();
            hideEmbed();
            setShowSections(true);
        }
        else
        {
            hideShortcuts();
            setShowSections(false);
        }
        Opencast.Initialize.doTest();
    }

    /**
        @memberOf Opencast.Player
        @description Toggle the embed
     */
    function doToggleEmbed()
    {
        if ($("#oc_btn-embed").attr("title") === EMBED)
        {
            showEmbed();
            hideNotes(); 
            hideSlides();
            hideSearch();
            hideShortcuts();
            setShowSections(true);
        }
        else
        {
            hideEmbed();
            setShowSections(false);
        }
        Opencast.Initialize.doTest();
    }
    
    /**
     * 
     * 
     * 
        To Videodisplay
     */
    
    /**
        @memberOf Opencast.Player
        @description Set the media URL.
        @param String mediaURL
     */
    function setMediaURL(mediaURLOne, mediaURLTwo)
    {
        if (mediaURLOne[0] === 'h' || mediaURLOne[0] === 'H' && mediaURLOne[2] === 't' || mediaURLOne[2] === 'T')
        {
            $("#oc-background-progress").attr('className', 'matterhorn-progress-bar-background');
        }
        Videodisplay.setMediaURL(mediaURLOne, mediaURLTwo);
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the captions URL.
        @param String captionsURL
     */
    function setCaptionsURL(captionsURL)
    {
        Videodisplay.setCaptionsURL(captionsURL);
    }   
    
    /**
        @memberOf Opencast.Player
        @description Do skip backward in the video.
     */
    function doSkipBackward() 
    {
        Videodisplay.skipBackward();
    }
    
    /**
        @memberOf Opencast.Player
        @description Do rewind in the video.
     */
    function doRewind()  
    {
        Videodisplay.rewind();
    }
    
    /**
        @memberOf Opencast.Player
        @description Do play the video.
     */
    function doPlay() 
    {
        Videodisplay.play();
    }

    /**
        @memberOf Opencast.Player
        @description Do pause the video.
     */
    function doPause() 
    {
        Videodisplay.pause(); 
    }

    /**
        @memberOf Opencast.Player
        @description Do fast forward the video.
     */
    function doFastForward() 
    {
        Videodisplay.fastForward();
    }
    
    /**
        @memberOf Opencast.Player
        @description Do skip forward in the vido.
     */
    function doSkipForward()  
    {
        Videodisplay.skipForward();
    }

    /**
        @memberOf Opencast.Player
        @description Toggle between play and pause the video.
     */
    function doTogglePlayPause()
    {
        // Checking if btn_play_pause is "play"
        if (getCurrentPlayPauseState() === PAUSING)
        {
            setCurrentPlayPauseState(PLAYING);
            doPlay();
        } 
        else 
        {
            setCurrentPlayPauseState(PAUSING);
            doPause();
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle between mute and unmute
     */
    function doToggleMute() 
    {
        Videodisplay.mute();
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume icon
     */
    function lowSound()
    {
        $("#oc_btn-volume").attr('className', 'oc_btn-volume-low');
        $("#oc_btn-volume").attr({ 
            alt: UNMUTE,
            title: UNMUTE
        });
        $("#oc_btn-volume").attr('aria-pressed', 'false');
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume icon
     */
    function noneSound()
    {
        $("#oc_btn-volume").attr('className', 'oc_btn-volume-none');
        $("#oc_btn-volume").attr({ 
            alt: UNMUTE,
            title: UNMUTE
        });
        $("#oc_btn-volume").attr('aria-pressed', 'false');
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume icon
     */
    function highSound()
    {
        $("#oc_btn-volume").attr('className', 'oc_btn-volume-high');
        $("#oc_btn-volume").attr({ 
            alt: UNMUTE,
            title: UNMUTE
        });
        $("#oc_btn-volume").attr('aria-pressed', 'false');
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume icon
     */
    function muteSound()
    {
        $("#oc_btn-volume").attr('className', 'oc_btn-volume-mute');
        $("#oc_btn-volume").attr({ 
            alt: MUTE,
            title: MUTE
        });
        $("#oc_btn-volume").attr('aria-pressed', 'true');
    }
    
    
    /**
        @memberOf Opencast.Player
        @description Set the cc icon
     */
    function setCCIconOn()
    {
        $("#oc_btn-cc").attr({ 
            alt: CCON,
            title: CCON
        });
        $("#oc_btn-cc").attr("className", "oc_btn-cc-on");
        $("#oc_btn-cc").attr('aria-pressed', 'true');
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the cc icon
     */
    function setCCIconOff()
    {
        $("#oc_btn-cc").attr({ 
            alt: CCOFF,
            title: CCOFF
        });
        $("#oc_btn-cc").attr("className", "oc_btn-cc-off");
        $("#oc_btn-cc").attr('aria-pressed', 'false');
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume slider
        @param Number newVolume
     */
    function setPlayerVolume(newPlayerVolume) 
    {
        Videodisplay.setVolumePlayer(newPlayerVolume);
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle between closed captions on or off.
        @param Boolean cc
     */
    function doToogleClosedCaptions() 
    {
        Videodisplay.closedCaptions();
    }
    
    /**
     * 
     * 
        From Videodisplay
     */
    
    /**
        @memberOf Opencast.Player
        @description Set the play/pause state and change the css style of the play/pause button.
        @param String state 
     */
    function setPlayPauseState(state) 
    {
        if (state === PLAYING) 
        {
            $("#oc_btn-play-pause").attr({ 
                alt: PLAY,
                title: PLAY
            });

            $("#oc_btn-play-pause").attr('className', 'oc_btn-play');
            $("#oc_btn-play-pause").attr('aria-pressed', 'false');

            if (getMouseOverBool() === true)
            {
                $("#oc_btn-play-pause").attr("className", "oc_btn-play-over");
            }
            setCurrentPlayPauseState(PAUSING);
        }
        else 
        {
            $("#oc_btn-play-pause").attr({ 
                alt: PAUSE,
                title: PAUSE
            });

            $("#oc_btn-play-pause").attr("className", "oc_btn-pause");
            $("#oc_btn-play-pause").attr('aria-pressed', 'true');

            if (getMouseOverBool() === true)
            {
                $("#oc_btn-play-pause").attr("className", "oc_btn-pause-over");
            }

            setCurrentPlayPauseState(PLAYING);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the current time of the video.
        @param String text, String playerId 
     */
    function setCurrentTime(text) 
    {
        $("#time-current").text(text);
        $("#slider_seek_Rail").attr("title", "Time " + text);
    }

    /**
        @memberOf Opencast.Player
        @description Set the total time of the video.
        @param String text, String playerId 
     */
    function setTotalTime(text) 
    {
        $("#time-total").text(text);
    }

    /**
        @memberOf Opencast.Player
        @description Set the slider max time and set the duration.
        @param Number time, String playerId 
     */
    function setDuration(time) 
    {
        duration = time;
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the new position of the seek slider.
        @param Number newPosition, String playerId 
     */
    function setPlayhead(newPosition) 
    {
        if (getDragging() === false)
        {
            var newPos = Math.round((newPosition / getDuration()) *  $("#scubber-channel").width());
            $("#draggable").css("left", newPos);
            $("#scrubber").css("left", newPos);
            $("#play-progress").css("width", newPos);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the with of the progress bar.
        @param Number value
     */
    function setProgress(value) 
    {
        $('.load-progress').css("width", (value + "%"));
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume slider
        @param Number newVolume
     */
    function setVolumeSlider(newVolume) 
    {
        Opencast.ariaSlider.changeValueFromVideodisplay(Opencast.ariaSlider.getElementId(SLIDERVOLUME), newVolume);
    }
    
    return {
        PlayPauseMouseOver : PlayPauseMouseOver,
        PlayPauseMouseOut : PlayPauseMouseOut,
        getShowSections : getShowSections,
        getDuration : getDuration,
        setDragging : setDragging,
        doToggleSlides : doToggleSlides,
        doToggleNotes : doToggleNotes,
        doToggleSearch : doToggleSearch,
        doToggleShortcuts : doToggleShortcuts,
        doToggleEmbed : doToggleEmbed,
        setMediaURL : setMediaURL,
        setCaptionsURL : setCaptionsURL,
        lowSound : lowSound,
        noneSound : noneSound,
        highSound : highSound,
        muteSound : muteSound,
        setCCIconOn : setCCIconOn,
        setCCIconOff : setCCIconOff,
        doSkipBackward : doSkipBackward,
        doRewind : doRewind,
        doPlay : doPlay,
        doPause : doPause,
        doFastForward : doFastForward,
        doSkipForward : doSkipForward,
        doTogglePlayPause : doTogglePlayPause,
        doToggleMute : doToggleMute,
        setPlayerVolume : setPlayerVolume,
        doToogleClosedCaptions : doToogleClosedCaptions,
        setPlayPauseState : setPlayPauseState,
        setCurrentTime : setCurrentTime,
        setTotalTime : setTotalTime,
        setDuration : setDuration,
        setPlayhead : setPlayhead,
        setProgress : setProgress,
        setVolumeSlider : setVolumeSlider
    };
}());