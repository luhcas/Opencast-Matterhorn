/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace segments
 */
Opencast.segments = ( function() {

  var totalPanels;
  var segmentTimes;
  var beforeSlide = 0;
  var currentSlide = 0;
  var nextSlide = 0;

  function getSecondsBeforeSlide(){
    return beforeSlide;
  }

  function getSecondsNextSlide(){
    return nextSlide;
  }

  /**
   * @memberOf Opencast.segments
   * @description Initializes the segments view
   */
  function initialize() {

    totalPanels     = $(".scrollContainer").children().size();

    var $panels       = $('#slider .scrollContainer > div');
    var $container      = $('#slider .scrollContainer');

    if ($panels !== undefined){

      $panels.css({'float' : 'left','position' : 'relative'});

      $("#slider").data("currentlyMoving", false);

       if($panels[0] !== undefined){
          $container
            .css('width', ($panels[0].offsetWidth * $panels.length))
            .css('left', "0px");
       }
      var scroll = $('#slider .scroll').css('overflow', 'hidden');

      //when the left/right arrows are clicked
      $(".right").click(function(){ change(true); }); 
      $(".left").click(function(){ change(false); });
    }

    var segmentTimes = new Array(); 
    $('.segments-time').each( function(i) {
      var seconds= $(this).html();
      segmentTimes[i] = seconds;
    });

    // Hide Slide Tab, if there are no slides
    if(segmentTimes.length === 0)
      Opencast.Player.doToggleSlides();

    $(document).everyTime(500, function(index) {
      var currentPosition = parseInt(Opencast.Player.getCurrentPosition());
      var last = 0;
      var cur = 0;
      var ibefore = 0;

      // last segment
      if (segmentTimes[segmentTimes.length-1] <= currentPosition){
        var ibefore = Math.max(segmentTimes.length-2,0);
        beforeSlide = segmentTimes[ibefore];
        currentSlide = segmentTimes[segmentTimes.length-1];;
        nextSlide = currentSlide;
      } else{
        for (i in segmentTimes)
        {
          cur = segmentTimes[i];
          if (last <= currentPosition && currentPosition < cur){
            ibefore = Math.max(parseInt(i)-2,0);
            beforeSlide = segmentTimes[ibefore];
            currentSlide = last;
            nextSlide = segmentTimes[i];
            break;
          }
          last = cur;
        }
      }
    }, 0);

  }

  /**
   * @memberOf Opencast.segments
   * @description Function initializes segments view
   * @param boolean direction true = right, false = left
   */
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
    getSecondsBeforeSlide : getSecondsBeforeSlide,
    getSecondsNextSlide : getSecondsNextSlide,
    initialize : initialize
  };
}());