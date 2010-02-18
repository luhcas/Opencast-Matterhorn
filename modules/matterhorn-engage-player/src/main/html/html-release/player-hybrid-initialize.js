/*global $, Player, Videodisplay, VideodisplaySecond, fluid, ariaSliderSecond*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
    @namespace the global Opencast namespace
*/
var Opencast = Opencast || {};

/**
    @namespace FlashVersion
*/
Opencast.Initialize = (function () 
{
    /**
        @memberOf Opencast.Player
        @description Keylistener.
     */
    function keyboardListener() {
    
        $(document).keyup(function (event) {

            if (event.altKey === true && event.ctrlKey === true) 
            {
                if (event.which === 77 || event.which === 109) // press m or M
                {
                    Opencast.Player.doToggleVolume();
                }
                if (event.which === 80 || event.which === 112 || event.which === 83 || event.which === 84 || event.which === 116 || event.which === 115 || event.which === 77 || event.which === 109 || event.which === 85 || event.which === 117  || event.which === 68 || event.which === 100 || event.which === 48 || event.which === 49 || event.which === 50 || event.which === 51 || event.which === 52 || event.which === 53 || event.which === 54  || event.which === 55 || event.which === 56 || event.which === 57 || event.which === 67 || event.which === 99 || event.which === 82 || event.which === 114 || event.which === 70 || event.which === 102 || event.which === 83 || event.which === 115 || event.which === 73 || event.which === 105)
                {
                    Videodisplay.passCharCode(event.which);
                }
                if (event.which === 85 || event.which === 68) // press arrow up or down
                {
                    Opencast.Player.setPlayerVolume(Videodisplay.getVolume(), Opencast.Player.FIRSTPLAYER);
                }
                event.preventDefault();
            }
        });
    }

    $(document).ready(function () {
 
        Opencast.ariaSlider.init();
        keyboardListener();
        var simpleEdit = fluid.inlineEdit("#simpleEdit", {
            selectors : {
                text: ".editableText",
                editContainer: "#editorContainer",
                edit: "#editField"
            },
            useTooltip : true,
            tooltipDelay : 500
        });
  
        $("#editorContainer").attr("className", "oc_editTime");
        $("#editField").attr("className", "oc_editTime");
        
        $("#btn_cc").attr('role', 'button');
        $("#btn_cc").attr('aria-pressed', 'false'); 
    
        $("#btn_volume").attr('role', 'button');
        $("#btn_volume").attr('aria-pressed', 'false');
    
        $("#btn_play_pause").attr('role', 'button');
        $("#btn_play_pause").attr('aria-pressed', 'false');

        $("#btn_skip_backward").attr('role', 'button');
        $("#btn_skip_backward").attr('aria-labelledby', 'Skip Backward');

        $("#btn_rewind").attr('role', 'button');
        $("#btn_rewind").attr('aria-labelledby', 'Rewind');

        $("#btn_fast_forward").attr('role', 'button');
        $("#btn_fast_forward").attr('aria-labelledby', 'Fast Forward');

        $("#btn_skip_forward").attr('role', 'button');
        $("#btn_skip_forward").attr('aria-labelledby', 'Skip Forward');

        $("#time-current").attr('role', 'timer');
        $("#time-total").attr('role', 'timer');

    });
    
    return {
       
    };
}());

