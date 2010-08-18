/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace search
 */
Opencast.search = ( function() {

  /**
   * @memberOf Opencast.search
   * @description Does the search
   */
  function showResult(value){
    // url: "../../search/rest/episode", // Production
    // url: "episode-segments.xml",      // For Testing
    var mediaPackageId = Opencast.engage.getMediaPackageId();
    $.ajax(
        {
          type: 'GET',
          contentType: 'text/xml',
          url: "../../search/rest/episode",
          data: "id=" + mediaPackageId+"&q="+escape(value),
          dataType: 'xml',
          success: function(xml) 
          {
            var rows = new Array();
            var ids = new Array();
            var sortedIds = new Array();
            var index = 0;

            var title = "";
            if(value !== "")
              title = "Results for &quot;" + unescape(value) +"&quot;<br/>";
            $('#oc_slidetext-left').html(title );

            // reset background color of scrubber segments
            $('.segment-holder').each( function(i) {
              $(this).css("background-color", "");
            });

            $(xml).find('segment').each(function(){
              var relevance = parseInt($(this).attr('relevance'));
              // Select only item with a relevanve value greater than zero
              var segmentIndex = parseInt($(this).attr('index')) + 1;
              if(relevance !== -1 || value === "") {
                  var seconds = parseInt($(this).attr('time')) / 1000;
                  var id = relevance*1000 - index;
                  var row = "";
                  var text = $(this).find('text').text();
                  var markedText = text.replace(new RegExp(value, 'gi'),'<span class="marked">' + value + '</span>');

                  var isHTML = false;
                  var j = 0;
                  for(var i = 0; i < markedText.length; i++) {
                    if(markedText.charAt(i) === "<")
                      isHTML = true;
                    else if(markedText.charAt(i) === ">")
                      isHTML = false;
                    else {
                      if(!isHTML){
                        markedText = setCharAt(markedText, i, text.charAt(j));
                        j++;
                      }
                    }
                  }

                  row += '<tr>';
                  row += '<td class="oc-segments-time">';
                  row += '<a onclick="Opencast.Watch.seekSegment('+ seconds +')" class="segments-time">';
                  row += Opencast.engage.formatSeconds(seconds);
                  row += "</a><br/>";
                  row += markedText;
                  row += "<br/><br/>";
                  row += "</td>";
                  row += "</tr>";

                  sortedIds[index] = id;
                  ids[index] = id;
                  rows[index] = row;
                  index++;

                  if (value !== "") {
                    // Set text inside of the scrubber
                    var rgbValue = 200*1/relevance;
                    $('#segment' + segmentIndex).css("background-color", "rgb(" + rgbValue + "," + rgbValue + "," + rgbValue + ")");
                  }
              }
            }); //close each(

            // Sort array
            if(value !== "")
              sortedIds.sort(Numsort).reverse();

            // Generate Table
            var table = "";
            table += '<br/><table cellspacing="0" cellpadding="0"><tbody>';
            for(i = 0; i < sortedIds.length; i++) {
              for(var j = 0; j < ids.length; j++) {
                if(sortedIds[i] === ids[j])
                  table += rows[j];
              }
            }
            table += "</tbody></table>";

            // Append Table
            $('#oc_slidetext-left').append(table);

          },
          error: function(a, b, c) 
          {
            // Some error while trying to get the search result
          }
        }); //close ajax(
  }

  function setCharAt(str,index,ch) {
    if(index > str.length-1) return str;
    return str.substr(0,index) + ch + str.substr(index+1);
  }

  /**
   * @memberOf Opencast.search
   * @description Initializes the search view
   */
  function initialize() {
    // initialize
    showResult("");
    /*
    var mediaPackageId = Opencast.engage.getMediaPackageId();
    var factor = 8;
    $.ajax(
        {
          type: 'GET',
          contentType: 'text/xml',
          url: "../../search/rest/episode",
          data: "id=" + mediaPackageId,
          dataType: 'xml',
          success: function(xml) 
          {
              $(xml).find('segment').each(function(){
                  var segmentIndex = parseInt($(this).attr('index')) + 1;
                  var text = $(this).find('text').text();
                  var segmentWidth = $('#segment' + segmentIndex).width();
                  var segmentText = text.substring(0, parseInt(segmentWidth/factor));
                  
                  if(text.length > parseInt(segmentWidth/factor)) {
                    segmentText += "...";
                  }
                  // Set text inside of the scrubber
                  $('#segment' + segmentIndex).html(segmentText);
              }); //close each(
          },
          error: function(a, b, c) 
          {
            // Some error while trying to get the search result
          }
        }); //close ajax(
        */
  }

  /**
   * @memberOf Opencast.search
   * @description Comparator
   */
  function Numsort (a, b) {
    return a - b;
  }

  return {
    showResult : showResult,
    initialize : initialize
  };
}());