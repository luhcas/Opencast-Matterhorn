/*global $, Player, Videodisplay, fluid, Scrubber*/
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
                	Opencast.Player.doToggleMute();
                }
                if (event.which === 80 || event.which === 112 || event.which === 83 || event.which === 84 || event.which === 116 || event.which === 115 || event.which === 85 || event.which === 117  || event.which === 68 || event.which === 100 || event.which === 48 || event.which === 49 || event.which === 50 || event.which === 51 || event.which === 52 || event.which === 53 || event.which === 54  || event.which === 55 || event.which === 56 || event.which === 57 || event.which === 67 || event.which === 99 || event.which === 82 || event.which === 114 || event.which === 70 || event.which === 102 || event.which === 83 || event.which === 115 || event.which === 73 || event.which === 105)
                {
                    Videodisplay.passCharCode(event.which);
                }
                event.preventDefault();
            }
        });
    }

    $(document).ready(function () {
    	
        keyboardListener();
        init();
        var simpleEdit = fluid.inlineEdit("#simpleEdit", {
            selectors : {
                text: ".editableText",
                editContainer: "#editorContainer",
                edit: "#editField"
            },
            useTooltip : true,
            tooltipDelay : 500
        });
        
        Opencast.ariaSlider.init();
        //  Will be done in the onPlayerReady function
        //   Opencast.Scrubber.init();
        
        $("#editorContainer").attr("className", "oc_editTime");
        $("#editField").attr("className", "oc_editTime");
        
        $("#oc_btn-cc").attr('role', 'button');
        $("#oc_btn-cc").attr('aria-pressed', 'false'); 
    
        $("#oc_btn-volume").attr('role', 'button');
        $("#oc_btn-volume").attr('aria-pressed', 'false');
    
        $("#oc_btn-play-pause").attr('role', 'button');
        $("#oc_btn-play-pause").attr('aria-pressed', 'false');

        $("#oc_btn-skip-backward").attr('role', 'button');
        $("#oc_btn-skip-backward").attr('aria-labelledby', 'Skip Backward');

        $("#oc_btn-rewind").attr('role', 'button');
        $("#oc_btn-rewind").attr('aria-labelledby', 'Rewind: Control + Alt + R');

        $("#oc_btn-fast-forward").attr('role', 'button');
        $("#oc_btn-fast-forward").attr('aria-labelledby', 'Fast Forward: Control + Alt + F');

        $("#oc_btn-skip-forward").attr('role', 'button');
        $("#oc_btn-skip-forward").attr('aria-labelledby', 'Skip Forward');

        $("#time-current").attr('role', 'timer');
        $("#time-total").attr('role', 'timer');
        
        
        $("#oc_btn-slides").attr('role', 'button');
        $("#oc_btn-slides").attr('aria-pressed', 'false'); 
        
        
    });
    
    
    /*
     * 
     * http://www.roytanck.com
     * Roy Tanck
     * http://www.this-play.nl/tools/resizer.html
     * 
     * */
    function reportSize() {
		  myWidth = 0, myHeight = 0;
		  if( typeof( window.innerWidth ) == 'number' ) {
		    //Non-IE
		    myWidth = window.innerWidth;
		    myHeight = window.innerHeight;
		  } else {
		    if( document.documentElement &&
		        ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
		      //IE 6+ in 'standards compliant mode'
		      myWidth = document.documentElement.clientWidth;
		      myHeight = document.documentElement.clientHeight;
		    } else {
		      if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
		        //IE 4 compatible
		        myWidth = document.body.clientWidth;
		        myHeight = document.body.clientHeight;
		      }
		    }
		  }
		}
    
		function doTest(){
			reportSize();
			
			if (myHeight >600)
			{
				$('#oc_body').css("height", (myHeight-50 + "px"));
				if(Opencast.Player.getShowSections() === false)
				{
					$('#oc_flash-player').css("height", (myHeight-138 + "px"));
				}
				else
				{
					$('#oc_flash-player').css("height", (myHeight-258 + "px"));
				}
				
			}
		}
		
		function init(){
			window.onresize = doTest;
			doTest();
		}
		
    
    return {
    	doTest : doTest
       
    };
}());

