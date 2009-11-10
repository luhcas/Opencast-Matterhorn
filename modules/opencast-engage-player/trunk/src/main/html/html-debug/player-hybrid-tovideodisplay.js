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
        var mute = "Mute",
            umute = "Unmute",
            button = document.getElementById("btn_volume");
        // Checking if btn_volume is "mute"
        if (button.value === mute) {
            //Changing the volume to 1.0 and the value of the button of btn_volume to "unmute"
            button.value = "mute";
            button.title = "mute";
            button.alt = "mute";
            button.src = "./icons/volume---mute.png";
            doSetVolume(0.0);
        } else {
            // Changing the volume to 0.0 and the value of the button of btn_volume to "mute"
            button.value = "unmute";
            button.alt = "unmute";
            button.title = "unmute";
            button.src = "./icons/volume---high.png";
            doSetVolume(1.0);
        }
    }

    function doClosedCaptions(cc) {
        Videodisplay.closedCaptions(cc);
    }

    function doToogleClosedCaptions() {
        var on = "cc on",
            off = "cc off",
            button = document.getElementById("btn_cc");
        // Checking if btn_cc is "CC off"
        if (button.value === off) {
            button.value = "close captions off";
            button.alt = "close captions off";
            button.title = "close captions off";
            button.src = "./icons/cc_on.png";
            doClosedCaptions(true);
        } else {
            button.value = "close captions on";
            button.alt = "close captions on";
            button.title = "close captions on";
            button.src = "./icons/cc_off.png";
            doClosedCaptions(false);
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
