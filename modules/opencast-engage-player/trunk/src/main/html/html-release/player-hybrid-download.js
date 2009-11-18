/*global $, Videodisplay*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */


/* ------------------------------------------------------------------------
 * the global opencast namespace
 * ------------------------------------------------------------------------ */
var Opencast = Opencast || {};

Opencast.volume = 1.0;


function doUnmute() {
    if (document.getElementById("btn_volume").value === "Unmute") {
        
       /*
        $("btn_volume").attr("value","Mute");
        $("btn_volume").attr("alt","Mute");
        $("btn_volume").attr("title","Mute");
        $("btn_volume").attr("src","./icons/volume---high.png");
        */
        
        document.getElementById("btn_volume").value = "Mute";
        document.getElementById("btn_volume").alt = "Mute";
        document.getElementById("btn_volume").title = "Mute";
        document.getElementById("btn_volume").src = "./icons/volume---high.png";
        
        //$("btn_volume").attr("alt","Mute";)
        //$("btn_volume").attr("value","";)
    } 
}

function mouseOver() {


    if (Opencast.ToVideodisplay.getCurrentPlayPauseState() === "playing") {
        document.getElementById("btn_play_pause").className = "btn_pause_over";
    }
    else if (Opencast.ToVideodisplay.getCurrentPlayPauseState() === "pausing") {
        document.getElementById("btn_play_pause").className = "btn_play_over";
    }
 
}

function mouseOut() {
  
    if (Opencast.ToVideodisplay.getCurrentPlayPauseState() === "playing") {
        document.getElementById("btn_play_pause").className = "btn_pause_out";
    }
    else if (Opencast.ToVideodisplay.getCurrentPlayPauseState() === "pausing") {
        document.getElementById("btn_play_pause").className = "btn_play_out";
    }
  
}





$(document).ready(function () {
    $("#slider").slider();
    $('#slider').slider('option', 'animate', false);
    $('#slider').slider('option', 'min', 0);
    $('#slider').bind('slidechange', function (event, ui) {
        if (ui.value === 0)
        {
            Opencast.ToVideodisplay.doTogglePlayPause();
        }
    });
    $('#slider').bind('slide', function (event, ui) {
        Videodisplay.seek(ui.value);
    });
    
    $('#volume_slider').slider();
    $('#volume_slider').slider('option', 'min', 0);
    $('#volume_slider').slider('option', 'max', 100);
    $('#volume_slider').slider({
        steps: 10
    });
    $('#volume_slider').slider('value', 100);
    $('#volume_slider').bind('slide', function (event, ui) {
        Opencast.ToVideodisplay.doSetVolume(ui.value / 100);
        Opencast.volume = ui.value / 100;
    });
    $('#volume_slider').bind('slidechange', function (event, ui) {
        if (ui.value !== 0) 
        {
            doUnmute();
        }
    });  
});



