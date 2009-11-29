/*global $, Videodisplay*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */


/**
    @namespace the global Opencast namespace
*/
var Opencast = Opencast || {};

Opencast.volume = 1.0;
Opencast.mouseOverBool = false;

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
    });
    $('#volume_slider').bind('slidechange', function (event, ui) {
        if (ui.value !== 0) 
        {
            Opencast.global.doUnmute();
        }
    });
    
    var simpleEdit = fluid.inlineEdit("#simpleEdit", {
        selectors : {
            text: ".editableText",
            editContainer: "#editorContainer",
            edit: "#editField"
        },
        useTooltip : true,
        tooltipDelay : 500
    });
    

});

Opencast.global = (function () {

    var playing = "playing",
    pausing     = "pausing",
    unmute      = "Unmute",
    mute        = "mute";

    /**
        @memberOf Opencast.global
        @description Do unmute the volume of the video.
    */
    function doUnmute() {
        if ($("#btn_volume").attr("value") === unmute) {  
            $("#btn_volume").attr({ 
                value: mute,
                alt: mute,
                title: mute
            });
            $("#btn_volume").attr("className", "oc-btn-volume-high");
        } 
    }
    
    /**
        @memberOf Opencast.global
        @description Mouse over effect.
    */
    function mouseOver() {
        if (Opencast.ToVideodisplay.getCurrentPlayPauseState() === playing) {
            $("#btn_play_pause").attr("className", "btn_pause_over");
            Opencast.mouseOverBool = true;
        }
         else if (Opencast.ToVideodisplay.getCurrentPlayPauseState() === pausing) {
            $("#btn_play_pause").attr("className", "btn_play_over");
            Opencast.mouseOverBool = true;
        }
    }
    /**
        @memberOf Opencast.global
        @description Mouse out effect.
    */
    function mouseOut() {
        if (Opencast.ToVideodisplay.getCurrentPlayPauseState() === playing) {
            $("#btn_play_pause").attr("className", "btn_pause_out");
            Opencast.mouseOverBool = false;
        }
        else if (Opencast.ToVideodisplay.getCurrentPlayPauseState() === pausing) {
            $("#btn_play_pause").attr("className", "btn_play_out");
            Opencast.mouseOverBool = false;
        }
    }
    
   
    return {
        doUnmute : doUnmute,
        mouseOver : mouseOver,
        mouseOut : mouseOut
    };
}());

