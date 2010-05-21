/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace pager
 */
Opencast.segments = ( function() {

  var totalPanels;
  
  
  /**
   * @memberOf Opencast.segments
   * @description Function initializes segments view
   */
  //direction true = right, false = left
  function initialize() {
    
    totalPanels     = $(".scrollContainer").children().size();
    
    var $panels       = $('#slider .scrollContainer > div');
    var $container      = $('#slider .scrollContainer');

    $panels.css({'float' : 'left','position' : 'relative'});
      
    $("#slider").data("currentlyMoving", false);

    $container
      .css('width', ($panels[0].offsetWidth * $panels.length))
      .css('left', "0px");

    var scroll = $('#slider .scroll').css('overflow', 'hidden');

    //when the left/right arrows are clicked
    $(".right").click(function(){ change(true); }); 
    $(".left").click(function(){ change(false); });
  }
  
  /**
   * @memberOf Opencast.segments
   * @description Function initializes segments view
   */
  //direction true = right, false = left
  function change(direction) {
    var leftValue    = parseFloat($(".scrollContainer").css("left"), 10)
    var scrollContainerWidth    = parseFloat( $(".scrollContainer").css("width"), 10);
    var sliderWidth    = parseFloat($(".scroll").css("width"), 10);
    var dif = sliderWidth - scrollContainerWidth;
    
      //if not at the first or last panel
    if((!direction && (leftValue>=0)) || (direction && (leftValue<= dif))) { return false; }  

    //if not currently moving
    if (($("#slider").data("currentlyMoving") == false)) {
      $("#slider").data("currentlyMoving", true);
      var maxMove = (scrollContainerWidth + leftValue) - sliderWidth;
      var movement   = direction ? leftValue - Math.min(sliderWidth, maxMove) : leftValue + Math.min(sliderWidth, leftValue*(-1));

      $(".scrollContainer")
        .stop()
        .animate({
          "left": movement
        }, function() {
          $("#slider").data("currentlyMoving", false);
        });
    }
  }

  return {
    initialize : initialize
  };
}());