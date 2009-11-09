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
        // $('#volume_slider').slider('value', newVolume);
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
        $('.matterhorn-progress-bar').css("width", value + "%");
    }
    
     function setCaptions(text) {
        document.getElementById("captions").innerHTML = text;
    }
    
     return {
       	setPlayhead : setPlayhead,
       	setVomume : setVolume,
       	setCurrentTime : setCurrentTime,
       	setTotalTime: setTotalTime,
       	setDuration: setDuration,
        setProgress : setProgress,
        setCaptions : setCaptions
     };
}());
	
	
	
	