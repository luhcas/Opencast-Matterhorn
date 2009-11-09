/*global $, Videodisplay*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */


/* ------------------------------------------------------------------------
 * the global opencast namespace ToVideodisplay
 * ------------------------------------------------------------------------ */

Opencast.ToVideodisplay = (function () {
	
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
    
    var playing = "playing";
    var pausing = "pausing";
    var currentPlayPauseState = pausing;

   	function setPlayPauseState(state) {
  		if (state == playing) {
    		document.getElementById("btn_play_pause").value = "Play";
		    document.getElementById("btn_play_pause").alt = "Play";
		     document.getElementById("btn_play_pause").title = "play";
		    document.getElementById("btn_play_pause").src = "./icons/play---green.png";
		    currentPlayPauseState = pausing;
  		} else {
		    document.getElementById("btn_play_pause").value = "Pause";
		    document.getElementById("btn_play_pause").alt = "Pause";
		    document.getElementById("btn_play_pause").title = "pause";
		    document.getElementById("btn_play_pause").src = "./icons/pause---green.png";
		    currentPlayPauseState = playing;
  		}
	}

	function doTogglePlayPause() {
        // Checking if btn_play_pause is "play"
        if (currentPlayPauseState == pausing) {
            // Changing the volume to 1.0 and the value of the button of btn_volume to "unmute"
            setPlayPauseState(playing);
            doPlay();
            return;
        } else {
            // Changing the volume to 0.0 and the value of the button of btn_volume to "mute"
            setPlayPauseState(pausing);
            doPause();
            return;
        }
    }
    
    function doFastForward() {
        Videodisplay.fastForward();
    }
    
    function doSkipForward() {
        Videodisplay.skipForward();
    }
    
    function doToggleVolume() {
        var mute = "Mute";
        var umute = "Unmute";
        // Checking if btn_volume is "mute"
        if (document.getElementById("btn_volume").value === mute) {
            //Changing the volume to 1.0 and the value of the button of btn_volume to "unmute"
            document.getElementById("btn_volume").value = "mute";
            document.getElementById("btn_volume").title = "mute";
            document.getElementById("btn_volume").alt = "mute";
            document.getElementById("btn_volume").src = "./icons/volume---mute.png";
            doSetVolume(0.0);
        } else {
            // Changing the volume to 0.0 and the value of the button of btn_volume to "mute"
            document.getElementById("btn_volume").value = "unmute";
            document.getElementById("btn_volume").alt = "unmute";
            document.getElementById("btn_volume").title = "unmute";
            document.getElementById("btn_volume").src = "./icons/volume---high.png";
            doSetVolume(1.0);
        }
    }
    
    function doSetVolume(value) {
        Videodisplay.setVolume(value);
    }
    
     function doClosedCaptions(cc) {
        Videodisplay.closedCaptions(cc);
    }
    
    function doToogleClosedCaptions() {
        var on = "cc on";
        var off = "cc off";
        // Checking if btn_cc is "CC off"
        if (document.getElementById("btn_cc").value === off) {
            document.getElementById("btn_cc").value = "close captions off";
            document.getElementById("btn_cc").alt = "close captions off";
             document.getElementById("btn_cc").title = "close captions off";
            document.getElementById("btn_cc").src = "./icons/cc_on.png";
            doClosedCaptions(true);
            return;
        } else {
            document.getElementById("btn_cc").value = "close captions on";
            document.getElementById("btn_cc").alt = "close captions on";
            document.getElementById("btn_cc").title = "close captions on";
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
        doTogglePlayPause : doTogglePlayPause,
        setPlayPauseState : setPlayPauseState,
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