/*global $, Videodisplay, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */


/* ------------------------------------------------------------------------
 * the global opencast namespace ToVideodisplay
 * ------------------------------------------------------------------------ */

Opencast.ToVideodisplay = (function () {

    var playing = "playing",
        pausing = "pausing",
        currentPlayPauseState = pausing;

    function doSeek(time) {
        $('#slider').slider('value', time);
        Videodisplay.seek(time);
    }

    function doSkipBackward() {
        Videodisplay.skipBackward();
    }

    function doRewind() {
        Videodisplay.rewind();
    }

    function doPlay() {
        Videodisplay.play();
    }

    function doPause() {
        Videodisplay.pause();
    }

    function doStop() {
        Videodisplay.stop();
    }

    function doSetCurrentPlayPauseState(state)
    {
        currentPlayPauseState = state;
    }

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

    function doFastForward() {
        Videodisplay.fastForward();
    }

    function doSkipForward() {
        Videodisplay.skipForward();
    }

    function doSetVolume(value) {
        Videodisplay.setVolume(value);
    }

    function doToggleVolume() {
        var mute = "Mute";
       
       
       
        // Checking if btn_volume is "mute"
        if (document.getElementById("btn_volume").value === mute) {
            //Changing the volume to 1.0 and the value of the button of btn_volume to "unmute"
            document.getElementById("btn_volume").value = "Unmute";
            document.getElementById("btn_volume").alt = "Unmute";
            document.getElementById("btn_volume").title = "Unmute";
            document.getElementById("btn_volume").src = "./icons/volume---mute.png";
            Opencast.volume = $('#volume_slider').slider('option', 'value') / 100;
            doSetVolume(0);
        } else {
            // Changing the volume to 0.0 and the value of the button of btn_volume to "mute"
            document.getElementById("btn_volume").value = "Mute";
            document.getElementById("btn_volume").alt = "Mute";
            document.getElementById("btn_volume").title = "Mute";
            document.getElementById("btn_volume").src = "./icons/volume---high.png";
            doSetVolume(Opencast.volume);
        }
    }
    
    function doClosedCaptions(cc) {
        Videodisplay.closedCaptions(cc);
    }

	function doToogleClosedCaptions() {
        var on = "Closed Caption on";
        // Checking if btn_cc is "CC off"
        if (document.getElementById("btn_cc").value === on) {
            document.getElementById("btn_cc").value = "Closed Caption off";
            document.getElementById("btn_cc").alt = "Closed Caption off";
            document.getElementById("btn_cc").title = "Closed Caption off";
            document.getElementById("btn_cc").src = "./icons/cc_on.png";
            doClosedCaptions(true);
            return;
        } else {
            document.getElementById("btn_cc").value = "Closed Caption on";
            document.getElementById("btn_cc").alt = "Closed Caption on";
            document.getElementById("btn_cc").title = "Closed Caption on";
            document.getElementById("btn_cc").src = "./icons/cc_off.png";
            doClosedCaptions(false);
            return;
        }
    }

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
