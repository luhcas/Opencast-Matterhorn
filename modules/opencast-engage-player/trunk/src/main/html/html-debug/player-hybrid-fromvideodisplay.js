/*global $, Videodisplay*/
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
    
    var playing = "playing";
    var pausing = "pausing";
    
    function setPlayPauseState(state) {
        if (state === playing) {
            document.getElementById("btn_play_pause").value = "Play";
            document.getElementById("btn_play_pause").alt = "Play";
            document.getElementById("btn_play_pause").title = "play";
            document.getElementById("btn_play_pause").src = "./icons/play---green.png";
            Opencast.ToVideodisplay.doSetCurrentPlayPauseState(pausing);
        } else {
            document.getElementById("btn_play_pause").value = "Pause";
            document.getElementById("btn_play_pause").alt = "Pause";
            document.getElementById("btn_play_pause").title = "pause";
            document.getElementById("btn_play_pause").src = "./icons/pause---green.png";
            Opencast.ToVideodisplay.doSetCurrentPlayPauseState(playing);
        }
    }
    
    function setDoToggleVolume() {
        Opencast.ToVideodisplay.doToggleVolume();
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
        setDoToggleVolume : setDoToggleVolume
    };
}());
