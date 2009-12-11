/*global $, Videodisplay, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */


/**
    @namespace the global Opencast namespace ToVideodisplay
*/
Opencast.ToVideodisplay = (function () {

    var pressKey = false,
        playing  = "playing",
        pausing  = "pausing",
        mute     = "Mute",
        unmute   = "Unmute",
        ccon     = "Closed Caption On",
        ccoff    = "Closed Caption Off",
        currentPlayPauseState = pausing;

    /**
        @memberOf Opencast.ToVideodisplay
        @description Do seek in the video.
        @param number time
    */
    function doSeek(time) {
        $('#slider').slider('value', time);
        Videodisplay.seek(time);
    }

    /**
        @memberOf Opencast.ToVideodisplay
        @description Do skip backward in the video.
    */
    function doSkipBackward() {
        Videodisplay.skipBackward();
    }

    /**
        @memberOf Opencast.ToVideodisplay
        @description Do rewind in the video.
    */
    function doRewind() {
        Videodisplay.rewind();
    }

    /**
        @memberOf Opencast.ToVideodisplay
        @description Do play the video.
    */
    function doPlay() {
        Videodisplay.play();
    }

    /**
        @memberOf Opencast.ToVideodisplay
        @description Do pause the video.
    */
    function doPause() {
        Videodisplay.pause();
    }
    
    /**
        @memberOf Opencast.ToVideodisplay
        @description Do stop the video.
    */
    function doStop() {
        Videodisplay.stop();
    }
    
    /**
        @memberOf Opencast.ToVideodisplay
        @description Set the current play pause state.
        @param string state
    */
    function doSetCurrentPlayPauseState(state)
    {
        currentPlayPauseState = state;
    }
    
    /**
        @memberOf Opencast.ToVideodisplay
        @description Get the current play pause state.
    */
    function getCurrentPlayPauseState()
    {
        return currentPlayPauseState;
    }
    
    /**
        @memberOf Opencast.ToVideodisplay
        @description Get the pressed key.
    */
    function getPressKey()
    {
        return pressKey;
    }

    /**
        @memberOf Opencast.ToVideodisplay
        @description Toggle between play and pause the video.
    */
    function doTogglePlayPause() {
        // Checking if btn_play_pause is "play"
        if (currentPlayPauseState === pausing) {
            // Changing the volume to 1.0 and the value of the button of btn_volume to "unmute"
            Opencast.FromVideodisplay.setPlayPauseState(playing);
            doPlay();
        } else {
            // Changing the volume to 0.0 and the value of the button of btn_volume to "mute"
            Opencast.FromVideodisplay.setPlayPauseState(pausing);
            doPause();
        }
    }
    
    /**
        @memberOf Opencast.ToVideodisplay
        @description Toggle between play and pause the video.
    */
    function doFastForward() {
        Videodisplay.fastForward();
    }

    /**
        @memberOf Opencast.ToVideodisplay
        @description Do skip forward in the vido.
    */
    function doSkipForward() {
        Videodisplay.skipForward();
    }
    
    /**
        @memberOf Opencast.ToVideodisplay
        @description Change the volume of the video.
    */
    function doSetVolume(value) {
        Videodisplay.setVolume(value);
    }

    /**
        @memberOf Opencast.ToVideodisplay
        @description Toggle between mute an unmute.
    */
    function doToggleVolume() {
        // Checking if btn_volume is "mute"
        if ($("#btn_volume").attr("value") === mute) {
            $("#btn_volume").attr({ 
                value: unmute,
                alt: unmute,
                title: unmute,
                src: "./icons/volume---mute.png"
            });
            Opencast.volume = $('#volume_slider').slider('option', 'value') / 100;
            doSetVolume(0);
        } else {
            $("#btn_volume").attr({ 
                value: mute,
                alt: mute,
                title: mute,
                src: "./icons/volume---high.png"
            });
            doSetVolume(Opencast.volume);
        }
    }
    /**
       @memberOf Opencast.ToVideodisplay
       @description Set the closed caption.
       @param boolean cc
    */
    function doClosedCaptions(cc) {
        Videodisplay.closedCaptions(cc);
    }
    /**
        @memberOf Opencast.ToVideodisplay
        @description Toggle between closed caption on and off.
    */
    function doToogleClosedCaptions() {
        // Checking if btn_cc is "CC off"
        if ($("#btn_cc").attr("value") === ccon) {
            $("#btn_cc").attr({ 
                value: ccoff,
                alt: ccoff,
                title: ccoff,
                src: "./icons/cc_on.png"
            });
            doClosedCaptions(true);
            return;
        } else {
            $("#btn_cc").attr({ 
                value: ccon,
                alt: ccon,
                title: ccon,
                src: "./icons/cc_off.png"
            });
            doClosedCaptions(false);
            return;
        }
    }

    /**
       @description Keylistener.
    */
    var isCtrl = false;
    var isAlt = false;
    
    $(document).keyup(function (e) { 
        if (e.which === 17) 
        {
            isCtrl = false; 
        }
        if (e.which === 18) 
        {
            isAlt = false; 
        }
        pressKey = false;
    }).keydown(function (e) { 
        if (e.which === 17)
        {
            isCtrl = true;
        }
        if (e.which === 18)
        {
            isAlt = true;
        }
        if (isCtrl === true && isAlt === true) {
          
            pressKey = true;
            if (e.which === 80 || e.which === 112 || e.which === 83 || e.which === 115 || e.which === 77 || e.which === 109 || e.which === 85 || e.which === 117  || e.which === 68 || e.which === 100 || e.which === 49 || e.which === 50 || e.which === 51 || e.which === 52 || e.which === 53 || e.which === 67 || e.which === 99 || e.which === 82 || e.which === 114 || e.which === 70 || e.which === 102)
            {
                Videodisplay.passCharCode(e.which);
                
            }
            return false;
        }
    }); 

    return {
        doSeek: doSeek,
        doSkipBackward : doSkipBackward,
        doRewind : doRewind,
        getCurrentPlayPauseState : getCurrentPlayPauseState,
        getPressKey : getPressKey,
        doPlay: doPlay,
        doPause: doPause,
        doStop: doStop,
        doSetCurrentPlayPauseState : doSetCurrentPlayPauseState,
        doTogglePlayPause : doTogglePlayPause,
        doFastForward: doFastForward,
        doSkipForward : doSkipForward,
        doToggleVolume: doToggleVolume,
        doSetVolume: doSetVolume,
        doClosedCaptions: doClosedCaptions,
        doToogleClosedCaptions : doToogleClosedCaptions
    };
}());
