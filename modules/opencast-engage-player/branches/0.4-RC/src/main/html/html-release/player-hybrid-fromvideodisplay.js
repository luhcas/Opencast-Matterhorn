/*global $, Videodisplay, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */


/* ------------------------------------------------------------------------
 * the global opencast namespace FromVideodisplay
 * ------------------------------------------------------------------------ */

Opencast.FromVideodisplay = (function () {

    function setPlayhead(newPosition) {
        $('#slider').slider('value', newPosition);
    }

    function setVolume(newVolume) {
        $('#volume_slider').slider('value', newVolume);
    }

    function setCurrentTime(text) {
        document.getElementById("time-current").innerHTML = text;
        //$("time-current").text(text);
    }

    function setTotalTime(text) {
        document.getElementById("time-total").innerHTML = text;
    }

    function setDuration(time) {
        $('#slider').slider('option', 'max', time);
    }

    function setProgress(value) {
        $('.matterhorn-progress-bar').css("width", (value + "%"));
    }
    
    function setCaptions(text) {
    	  document.getElementById("captions").innerHTML = text;
    }
    
    function setCaptionsButton(bool) {
       
        if (bool === true)
        {
            document.getElementById("btn_cc").value = "Closed Caption off";
            document.getElementById("btn_cc").alt = "Closed Caption off";
            document.getElementById("btn_cc").title = "Closed Caption off";
            document.getElementById("btn_cc").src = "./icons/cc_on.png";
        }
        else
        {
            document.getElementById("btn_cc").value = "Closed Caption on";
            document.getElementById("btn_cc").alt = "Closed Caption on";
            document.getElementById("btn_cc").title = "Closed Caption on";
            document.getElementById("btn_cc").src = "./icons/cc_off.png";
        }
    }
    
    var playing = "playing";
    var pausing = "pausing";
    
    function setPlayPauseState(state) {
        if (state === playing) {
            document.getElementById("btn_play_pause").value = "Play";
            document.getElementById("btn_play_pause").alt = "Play";
            document.getElementById("btn_play_pause").title = "play";
          
            if (Opencast.ToVideodisplay.getPressKey() === true)
            {
                document.getElementById("btn_play_pause").className = "btn_play_out";
            }
            else
            {
                document.getElementById("btn_play_pause").className = "btn_play_over";
            }
            
            Opencast.ToVideodisplay.doSetCurrentPlayPauseState(pausing);
        } 
        else {
            document.getElementById("btn_play_pause").value = "Pause";
            document.getElementById("btn_play_pause").alt = "Pause";
            document.getElementById("btn_play_pause").title = "pause";
           
            if (Opencast.ToVideodisplay.getPressKey() === true)
            {
                document.getElementById("btn_play_pause").className = "btn_pause_out";
             
            }
            else
            {
                document.getElementById("btn_play_pause").className = "btn_pause_over";
            }
            
            Opencast.ToVideodisplay.doSetCurrentPlayPauseState(playing);
        }
    }
    
    function setDoToggleVolume() {
        document.getElementById("btn_volume").value = "Unmute";
        document.getElementById("btn_volume").alt = "Unmute";
        document.getElementById("btn_volume").title = "Unmute";
        document.getElementById("btn_volume").src = "./icons/volume---mute.png";
    }
    
    return {
        setPlayhead : setPlayhead,
        setVolume : setVolume,
        setCurrentTime : setCurrentTime,
        setTotalTime: setTotalTime,
        setDuration: setDuration,
        setProgress : setProgress,
        setCaptions : setCaptions,
        setPlayPauseState : setPlayPauseState,
        setCaptionsButton : setCaptionsButton,
        setDoToggleVolume : setDoToggleVolume
    };
}());
