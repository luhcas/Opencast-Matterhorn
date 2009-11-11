/*global $, Videodisplay*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */


/* ------------------------------------------------------------------------
 * the global opencast namespace
 * ------------------------------------------------------------------------ */
var Opencast = Opencast || {};

function doUnmute() {
    if (document.getElementById("btn_volume").value === "Unmute") {
        document.getElementById("btn_volume").value = "Mute";
        document.getElementById("btn_volume").alt = "Mute";
        document.getElementById("btn_volume").title = "Mute";
        document.getElementById("btn_volume").src = "./icons/volume---high.png";
    } 
}

Opencast.volume = 1.0;

$(document).ready(function () {
    $("#slider").slider();
    $('#slider').slider('option', 'animate', false);
    $('#slider').slider('option', 'min', 0);
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
        doUnmute();
    });
});

