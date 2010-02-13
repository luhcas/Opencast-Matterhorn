/*global $, Player, Videodisplay, VideodisplaySecond, fluid, ariaSliderSecond*/
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
    UNMUTE                = "Unmute: Control + Alt + M",
    MUTE                  = "Mute: Control + Alt + M",
    CCON                  = "Closed Caption On: Control + Alt + C",
    CCOFF                 = "Closed Caption Off: Control + Alt + C",
    SLIDERVOLUME          = "slider_volume_Thumb",
    SLIDERSEEK            = "slider_seek_Thumb",
    FIRSTPLAYER           = "firstPlayer",
    SECONDPLAYER          = "secondPlayer",
    infoBool              = false,
    ccBool                = false,
    pressKey              = false,
    mouseOverBool         = false,
    isCtrl                = false,
    isAlt                 = false,
    captionsBool          = false,
    currentPlayPauseState = PAUSING,
    volume                = 1.0;
    
        
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
        @description Get the infoBool.
     */
    function getInfoBool()
    {
        return infoBool;
    }

    /**
        @memberOf Opencast.Player
        @description Set the infoBool.
        @param Boolean bool
     */
    function setInfoBool(bool)
    {
        infoBool = bool;
    }    
    
    /**
        @memberOf Opencast.Player
        @description Get the ccBool.
     */
    function getccBool()
    {
        return ccBool;
    }

    /**
        @memberOf Opencast.Player
        @description Set the ccBool.
        @param Booelan bool
     */
    function setccBool(bool)
    {
        ccBool = bool;
    } 
    
    /**
        @memberOf Opencast.Player
        @description Get the pressKey.
      */
    function getPressKey()
    {
        return pressKey;
    }

    /**
        @memberOf Opencast.Player
        @description Set the pressKey.
        @param Booelan bool
     */
    function setPressKey(bool)
    {
        pressKey = bool;
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
        @description Get the isCtrl.
     */
    function getIsCtrl()
    {
        return isCtrl;
    }

    /**
        @memberOf Opencast.Player
        @description Set the isCtrl.
        @param Booelan bool
     */
    function setIsCtrl(bool)
    {
        isCtrl = bool;
    }
    
    /**
        @memberOf Opencast.Player
        @description Get the isAlt.
     */
    function getIsAlt()
    {
        return isAlt;
    }

    /**
        @memberOf Opencast.Player
        @description Set the isAlt.
        @param Booelan bool
     */
    function setIsAlt(bool)
    {
        isAlt = bool;
    }

    /**
        @memberOf Opencast.Player
        @description Get the isAlt.
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
        @description Get the captionsBool.
     */
    function getPlayerVolume()
    {
        return volume;
    }

    /**
        @memberOf Opencast.Player
        @description Set the volume.
        @param Number vol
     */
    function setPlayerVolume(vol)
    {
        volume = vol;
    }

    /**
        @memberOf Opencast.Player
        @description Mouse over effect, change the css style.
     */
    function PlayPauseMouseOver() {
        if (getCurrentPlayPauseState() === PLAYING) {
            $("#btn_play_pause").attr("className", "btn_pause_over");
            setMouseOverBool(true);
        }
         else if (getCurrentPlayPauseState() === PAUSING) {
            $("#btn_play_pause").attr("className", "btn_play_over");
            setMouseOverBool(true);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Mouse out effect, change the css style.
     */
    function PlayPauseMouseOut() {
        if (getCurrentPlayPauseState() === PLAYING) {
            $("#btn_play_pause").attr("className", "btn_pause_out");
            setMouseOverBool(false);
        }
        else if (getCurrentPlayPauseState() === PAUSING) {
            $("#btn_play_pause").attr("className", "btn_play_out");
            setMouseOverBool(false);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Remove the alert div.
     */
    function removeOldAlert()
    {
        var oldAlert = $("#alert").get(0);
    
        if (oldAlert)
        {
            document.body.removeChild(oldAlert);
        }
    }

    /**
        @memberOf Opencast.Player
        @description Remove the old alert div and create an new div with the aria role alert.
        @param String alertMessage
     */
    function addAlert(alertMessage)
    {
        removeOldAlert();
        var newAlert = document.createElement("div");
        newAlert.setAttribute("role", "alert");
        newAlert.setAttribute("id", "alert");
        newAlert.setAttribute("class", "oc-offScreen-hidden");
        var msg = document.createTextNode(alertMessage);
        newAlert.appendChild(msg);
        document.body.appendChild(newAlert);
    }
    
    /**
        @memberOf Opencast.Player
        @description When the learner edit the current time.
     */
    function editTime()
    {
        var timeString = $("#editField").attr("value");
        timeString = timeString.replace(/[-\/]/g, ':'); 
        timeString = timeString.replace(/[^0-9: ]/g, ''); 
        timeString = timeString.replace(/ +/g, ' '); 
        var time = timeString.split(':');

        try
        {
            var seekHour = parseInt(time[0], 10);
            var seekMinutes = parseInt(time[1], 10);
            var seekSeconds = parseInt(time[2], 10);
            
            if (seekHour > 99 || seekMinutes > 59 || seekSeconds > 59)
            {
                addAlert('Wrong Time enter like this: HH:MM:SS');
            } 
            else 
            {
                var seek = (seekHour * 60 * 60) + (seekMinutes * 60) + (seekSeconds);
                Videodisplay.seek(seek);
                VideodisplaySecond.seek(seek);
            }
        }
        catch (exception) 
        {
            addAlert('Wrong Time enter like this: HH:MM:SS');
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle between Keyboard Shurtcuts visible or unvisible.
        @param String playerId 
     */
    function toggleInfo(playerId) 
    {
        if (playerId === FIRSTPLAYER)
        {
            if (getInfoBool() === false)
            {
                $("#infoBlock").attr("className", "oc_infoDisplayBlock");
                addAlert($("#infoBlock").text());
                setInfoBool(true);
            }
            else if (getInfoBool() === true)
            {
                $("#infoBlock").attr("className", "oc_infoDisplayNone");
                setInfoBool(false);
            }
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Do unmute the volume of the video.
     */
    function doUnmute() {
        if ($("#btn_volume").attr("title") === UNMUTE) {  
            $("#btn_volume").attr({ 
                alt: MUTE,
                title: MUTE
            });
            
            $("#btn_volume").attr("className", "oc-btn-volume-high");
        } 
    }
    
    /**
        @memberOf Opencast.Player
        @description When the learner press a key.
        @param Event evt
     */
    function editTimeKeyListener(evt) {
        var charCode;
        if (evt && evt.which)
        {
            evt = evt;
            charCode = evt.which;
        }
        else
        {
            evt = event;
            charCode = evt.keyCode;
        }
    
        if (charCode === 13) // return
        {
            editTime(); 
        }
    }
    
    /**
     * 
     * 
        To Videodisplay
     */

    /**
        @memberOf Opencast.Player
        @description Do skip backward in the video.
     */
    function doSkipBackward() 
    {
        Videodisplay.skipBackward();
        VideodisplaySecond.skipBackward();
    }

    /**
        @memberOf Opencast.Player
        @description Do rewind in the video.
     */
    function doRewind() 
    {
        Videodisplay.rewind();
        VideodisplaySecond.rewind();
    }

    /**
        @memberOf Opencast.Player
        @description Do play the video.
     */
    function doPlay() 
    {
        Videodisplay.play();
        VideodisplaySecond.play();
    }

    /**
        @memberOf Opencast.Player
        @description Do pause the video.
     */
    function doPause() 
    {
        Videodisplay.pause();
        VideodisplaySecond.pause();
    }

    /**
        @memberOf Opencast.Player
        @description Do stop the video.
     */
    function doStop() 
    {
        Videodisplay.stop();
        VideodisplaySecond.stop();
    }
    
    /**
        @memberOf Opencast.Player
        @description Do fast forward the video.
     */
    function doFastForward() 
    {
        Videodisplay.fastForward();
        VideodisplaySecond.fastForward();
    }

    /**
        @memberOf Opencast.Player
        @description Do skip forward in the vido.
     */
    function doSkipForward() 
    {
        Videodisplay.skipForward();
        VideodisplaySecond.skipForward();
    }

    /**
        @memberOf Opencast.Player
        @description Set the closed caption true or false.
        @param Boolean cc
     */
    function doClosedCaptions(cc) 
    {
        Videodisplay.closedCaptions(cc);
        VideodisplaySecond.closedCaptions(cc);
    }
    
    /**
        @memberOf Opencast.Player
        @description Change the volume of the video.
     */
    function doSetVolume(value) 
    {
        Videodisplay.setVolume(value);
        VideodisplaySecond.setVolume(value);
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
        @description Change the css style, when the learner press the closed captions button on.
     */
    function setClosedCaptionsOn() 
    {
        doClosedCaptions(true);
        $("#btn_cc").attr({ 
            alt: CCON,
            title: CCON
        });
    
        $("#btn_cc").attr("className", "oc-btn-cc-on");
        $("#btn_cc").attr('aria-pressed', 'true');
    }
    
    /**
        @memberOf Opencast.Player
        @description Change the css style, when the learner press the closed captions button off.
     */
    function setClosedCaptionsOff() 
    {
        doClosedCaptions(false);
        $("#btn_cc").attr({ 
            alt: CCOFF,
            title: CCOFF
        });
        
        $("#btn_cc").attr("className", "oc-btn-cc-off");
        $("#btn_cc").attr('aria-pressed', 'false');
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle between closed captions on or off.
        @param Boolean cc
     */
    function doToogleClosedCaptions() 
    {
        if (getCaptionsBool() === true)
        {   
            // Checking if btn_cc is "CC off"
            if ($("#btn_cc").attr("title") === CCOFF) 
            {
                setccBool(true);
                Videodisplay.setccBool(true);
                VideodisplaySecond.setccBool(true);
                setClosedCaptionsOn();
            } 
            else 
            {
                setccBool(false);
                Videodisplay.setccBool(false);
                VideodisplaySecond.setccBool(false);
                setClosedCaptionsOff();
            }
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle between mute an unmute.
     */
    function doToggleVolume() 
    {
        // Checking if btn_volume is "mute"
        if ($("#btn_volume").attr('title') === UNMUTE) 
        {
            $("#btn_volume").attr({ 
                alt: MUTE,
                title: MUTE
            });
           
            $("#btn_volume").attr('className', 'oc-btn-volume-mute');
            $("#btn_volume").attr('aria-pressed', 'true');
        
            doSetVolume(0);
            
            // When the Button cc is not press before
            if (getccBool() === false && getCaptionsBool() === true)
            {
                setClosedCaptionsOn();
            }
        } 
        else 
        {
            $("#btn_volume").attr({ 
                alt: UNMUTE,
                title: UNMUTE
            });
            $("#btn_volume").attr('className', 'oc-btn-volume-high');
            $("#btn_volume").attr('aria-pressed', 'false');
        
            doSetVolume(getPlayerVolume());
            // When the Button cc is not press before
            if (getccBool() === false && getCaptionsBool() === true)
            {
                setClosedCaptionsOff();
            }
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the media URL.
        @param String mediaURL
     */
    function setMediaURL(mediaURL)
    {
        if (mediaURL[0] === 'h' || mediaURL[0] === 'H' && mediaURL[2] === 't' || mediaURL[2] === 'T')
        {
            $("#oc-background-progress").attr('className', 'matterhorn-progress-bar-background');
        }
        Videodisplay.setMediaURL(mediaURL);
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the captions URL.
        @param String captionsURL
     */
    function setCaptionsURL(captionsURL)
    {
        Videodisplay.setCaptionsURL(captionsURL);
        setCaptionsBool(true);
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the player Id.
        @param String playerId
     */
    function setPlayerId(playerId)
    {
        Videodisplay.setPlayerId(playerId);
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the media URL.
        @param String mediaURL
     */
    function setSecondMediaURL(mediaURL)
    {
        if (mediaURL[0] === 'h' || mediaURL[0] === 'H')
        {
            $("#oc-background-progress").attr('className', 'matterhorn-progress-bar-background');
        }
        VideodisplaySecond.setMediaURL(mediaURL);
    }

    /**
        @memberOf Opencast.Player
        @description Set the captions URL.
        @param String captionsURL
     */
    function setSecondCaptionsURL(captionsURL)
    {
        VideodisplaySecond.setCaptionsURL(captionsURL);
        setCaptionsBool(true);
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the player Id.
        @param String playerId
     */
    function setSecondPlayerId(playerId)
    {
        VideodisplaySecond.setPlayerId(playerId);
    }
    
    /**
        @memberOf Opencast.Player
        @description Keylistener.
     */
    $(document).keyup(function (e) 
    { 
        if (e.which === 17) 
        {
            setIsCtrl(false); 
        }
        if (e.which === 18) 
        {
            setIsAlt(false); 
        }
        setPressKey(false);
    }).keydown(function (e) 
    { 
        if (e.which === 17)
        {
            setIsCtrl(true);
        }
        if (e.which === 18)
        {
            setIsAlt(true);
        }
        if (getIsCtrl() === true && getIsAlt() === true) 
        {
            setPressKey(true);
        
            if (e.which === 77 || e.which === 109) // press m or M
            {
                doToggleVolume();
            }
            if (e.which === 80 || e.which === 112 || e.which === 83 || e.which === 84 || e.which === 116 || e.which === 115 || e.which === 77 || e.which === 109 || e.which === 85 || e.which === 117  || e.which === 68 || e.which === 100 || e.which === 48 || e.which === 49 || e.which === 50 || e.which === 51 || e.which === 52 || e.which === 53 || e.which === 54  || e.which === 55 || e.which === 56 || e.which === 57 || e.which === 67 || e.which === 99 || e.which === 82 || e.which === 114 || e.which === 70 || e.which === 102 || e.which === 83 || e.which === 115 || e.which === 73 || e.which === 105)
            {
                Videodisplay.passCharCode(e.which);
                VideodisplaySecond.passCharCode(e.which);
            }
            if (e.which === 85 || e.which === 68) // press arrow up or down
            {
                setPlayerVolume(Videodisplay.getVolume());
            }
        
            return false;
        }
    
    }); 

    /**
     * 
     * 
        From Videodisplay
     */

    /**
        @memberOf Opencast.Player
        @description Set the new position of the seek slider.
        @param Number newPosition, String playerId 
     */
    function setPlayhead(newPosition, playerId) 
    {
        if (playerId === FIRSTPLAYER)
        {
            Opencast.ariaSlider.changeValueFromVideodisplay(Opencast.ariaSlider.getElementId(SLIDERSEEK), newPosition);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the new position of the seek slider.
        @param Number newPosition
     */
    function setPlayheadFullscreen(newPosition) 
    {
	    Opencast.ariaSlider.changeValue(Opencast.ariaSlider.getElementId(SLIDERSEEK), newPosition);
    }
    
    /**
        @memberOf Opencast.Player
        @description Change the css style of the mute/unmute button.
     */
    function setDoUnmute() 
    {
        if ($("#btn_volume").attr("title") === MUTE) 
        {  
            $("#btn_volume").attr({ 
                alt: UNMUTE,
                title: UNMUTE
            });
            
            $("#btn_volume").attr("className", "oc-btn-volume-high");
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the new position of the volume slider.
        @param Number newVolume, String playerId 
     */
    function setVolume(newVolume, playerId) 
    {
        if (playerId === FIRSTPLAYER)
        {
            if (newVolume !== 0)
            {
                setDoUnmute();
            }
            Opencast.ariaSlider.changeValueFromVideodisplay(Opencast.ariaSlider.getElementId(SLIDERVOLUME), newVolume);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume.
        @param Number newVolume, String playerId
     */
    function setOpencastVolume(newVolume, playerId) 
    {
        if (playerId === FIRSTPLAYER)
        {
            setPlayerVolume(newVolume); 
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the current time of the video.
        @param String text, String PlayerId 
     */
    function setCurrentTime(text, playerId) 
    {
        if (playerId === FIRSTPLAYER)
        {
            $("#time-current").text(text);
            $("#slider_seek_Rail").attr("title", "Time " + text);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the total time of the video.
        @param String text, String playerId 
     */
    function setTotalTime(text, playerId) 
    {
        if (playerId === FIRSTPLAYER)
        {
            $("#time-total").text(text);
        }
    }

    /**
        @memberOf Opencast.Player
        @description Set the slider max time.
        @param Number time, String playerId 
     */
    function setDuration(time, playerId) 
    {
        if (playerId === FIRSTPLAYER)
        {
            $('#slider').slider('option', 'max', time);
            Opencast.ariaSlider.getElementId(SLIDERSEEK).setAttribute('aria-valuemax', time);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the with of the progress bar.
        @param Number value, String playerId 
     */
    function setProgress(value, playerId) 
    {
        if (playerId === FIRSTPLAYER)
        {
            $('.matterhorn-progress-bar').css("width", (value + "%"));
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle the closed caption button between on or off and change the css style from the closed captions button.
        @param Boolean bool, String playerId 
     */
    function setCaptionsButton(bool, playerId) 
    {
        if (playerId === FIRSTPLAYER)
        {
            if (bool === true)
            {
                $("#btn_cc").attr({ 
                    alt: CCON,
                    title: CCON
                });
                $("#btn_cc").attr("className", "oc-btn-cc-on");
                $("#btn_cc").attr('aria-pressed', 'true');
                setccBool(true);
            }
            else
            {
                $("#btn_cc").attr({ 
                    alt: CCOFF,
                    title: CCOFF
                });
                $("#btn_cc").attr("className", "oc-btn-cc-off");
                $("#btn_cc").attr('aria-pressed', 'false');
                setccBool(false);
            }
        }
    }
    
    
    /**
        @memberOf Opencast.Player
        @description Set the play/pause state and change the css style of the play/pause button.
        @param String state 
     */
    function setPlayPauseState(state) 
    {
        if (state === PLAYING) {
            $("#btn_play_pause").attr({ 
                alt: PLAY,
                title: PLAY
            });
       
            $("#btn_play_pause").attr('className', 'btn_play_out');
            $("#btn_play_pause").attr('aria-pressed', 'false');
        
            if (getMouseOverBool() === true)
            {
                $("#btn_play_pause").attr("className", "btn_play_over");
            }
            setCurrentPlayPauseState(PAUSING);
        } 
        else {
            $("#btn_play_pause").attr({ 
                alt: PAUSE,
                title: PAUSE
            });
       
            $("#btn_play_pause").attr("className", "btn_pause_out");
            $("#btn_play_pause").attr('aria-pressed', 'true');
        
            if (getMouseOverBool() === true)
            {
                $("#btn_play_pause").attr("className", "btn_pause_over");
            }
        
            setCurrentPlayPauseState(PLAYING);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle the volume between mute or unmute.
        @param String playerId
     */
    function setDoMute(playerId) 
    {
        if (playerId === FIRSTPLAYER)
        {
            if (Videodisplay.getVolume() !== 0)
            {
                setPlayerVolume(Videodisplay.getVolume());
            }
            doToggleVolume();
        }
    }	
    
    /**
        @memberOf Opencast.Player
        @description Set the captions in html
        @param Html text, String playerId 
     */
    function setCaptions(text, playerId) 
    {
        if (playerId === FIRSTPLAYER)
        {
            var elm = document.createElement('li');
            elm.innerHTML = text;        
            $('#captions').empty().append(elm); 
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description addAlert in html code.
        @param Html text, String playerId 
     */
    function hearTimeInfo(alertMessage, playerId)
    {
        if (playerId === FIRSTPLAYER)
        {
            addAlert(alertMessage);
        }
    }
    return {
        getCurrentPlayPauseState : getCurrentPlayPauseState,
        setCurrentPlayPauseState : setCurrentPlayPauseState,
        getInfoBool : getInfoBool,
        setInfoBool : setInfoBool,
        getccBool : getccBool,
        setccBool : setccBool,
        getPressKey : getPressKey,
        setPressKey : setPressKey,
        getMouseOverBool : getMouseOverBool,
        setMouseOverBool : setMouseOverBool,
        getIsCtrl : getIsCtrl,
        setIsCtrl : setIsCtrl,
        getIsAlt : getIsAlt,
        setIsAlt : setIsAlt,
        getCaptionsBool : getCaptionsBool,
        setCaptionsBool : setCaptionsBool,
        getPlayerVolume : getPlayerVolume,
        setPlayerVolume : setPlayerVolume,
        PlayPauseMouseOver : PlayPauseMouseOver,
        PlayPauseMouseOut : PlayPauseMouseOut,
        removeOldAlert : removeOldAlert,
        addAlert : addAlert,
        editTime : editTime,
        toggleInfo : toggleInfo,
        doUnmute : doUnmute,
        editTimeKeyListener : editTimeKeyListener,
        doSkipBackward : doSkipBackward,
        doRewind : doRewind,
        doPlay : doPlay,
        doPause : doPause,
        doStop : doStop,
        doFastForward : doFastForward,
        doSkipForward : doSkipForward,
        doClosedCaptions : doClosedCaptions,
        doSetVolume : doSetVolume,
        doTogglePlayPause : doTogglePlayPause,
        setClosedCaptionsOn : setClosedCaptionsOn,
        setClosedCaptionsOff : setClosedCaptionsOff,
        doToogleClosedCaptions : doToogleClosedCaptions,
        doToggleVolume : doToggleVolume,
        setPlayhead : setPlayhead,
        setPlayheadFullscreen : setPlayheadFullscreen,
        setDoUnmute : setDoUnmute,
        setVolume : setVolume,
        setOpencastVolume : setOpencastVolume,
        setCurrentTime : setCurrentTime,
        setTotalTime : setTotalTime,
        setDuration : setDuration,
        setProgress : setProgress,
        setCaptionsButton : setCaptionsButton,
        setPlayPauseState : setPlayPauseState,
        setDoMute : setDoMute,
        setCaptions : setCaptions,
        hearTimeInfo : hearTimeInfo,
        setMediaURL : setMediaURL,
        setCaptionsURL : setCaptionsURL,
        setSecondMediaURL : setSecondMediaURL,
        setSecondCaptionsURL : setSecondCaptionsURL,
        setPlayerId : setPlayerId,
        setSecondPlayerId : setSecondPlayerId
    };
}());