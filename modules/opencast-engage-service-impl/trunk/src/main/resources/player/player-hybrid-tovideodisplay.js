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
            doSetVolume(0.0);
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

    function doSetLanguage(value) {
        Videodisplay.setLanguage(value);
    }


    function setLangugageComboBox(languageComboBox) {
        for (var i = 0; i < languageComboBox.length; i = i + 1) {
            var option = document.createElement('option');
            option.text = languageComboBox[i];
            var cb_item = document.getElementById("cb_lang");
            try {
                cb_item.add(option, null); // standards compliant
            } catch (ex) {
                cb_item.add(option); // IE only
            }
        }
    }

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
        doToogleClosedCaptions : doToogleClosedCaptions,
        doSetLanguage: doSetLanguage,
        setLangugageComboBox : setLangugageComboBox
    };
}());
