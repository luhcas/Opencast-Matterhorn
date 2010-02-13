/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace search
 */
Opencast.search = ( function() {
  /**
   * @memberOf Opencast.search
   * @description Function is called when the document is ready
   */
  $(document).ready( function() {
    testGetCurrentPage();
    renderPager();
  });

  /**
   * @memberOf Opencast.search
   * @description Function renders the pager
   */
  function renderPager() {
    // constants
    var LINK_PREFIX = "pager.html?page=";
    var PREVIOUS_TEXT = "Previous";
    var NEXT_TEXT = "Next";
    var OFFSET = 2;

    // variables
    var text;
    var link;
    var currentPageId;
    var maxPageId;
    var li;
    var spanBeforeSet = false;
    var spanAfterSet = false;

    // get the current page id
    currentPageId = getCurrentPageID();

    // get the max page id
    maxPageId = getMaxPageID();

    // take care for the previous page button
    if (currentPageId <= 1) {
      text = "<span>" + PREVIOUS_TEXT + "</span>";

    } else {
      link = LINK_PREFIX + (currentPageId - 1);
      text = "<a href='" + link + "'>" + PREVIOUS_TEXT + "</a>";
    }
    li = document.createElement('li');
    li.innerHTML = text;
    $('#navigation').append(li);

    // take care for the page buttons
    for ( var i = 1; i <= maxPageId; i++) {
      li = document.createElement('li');
      /* if the span "..." before the current page is not set
       *   and current page id is equal or greater than 5
       *   and the running index is greater than 1
       * then insert a span containing "..."
       * 
       * otherwise if span "..." after the current page is not set
       *   and the running index - 1 is greater the the current page
       *   and the running index is greater than 4
       * then insert a span containing "..."
       */
      if (!spanBeforeSet && currentPageId >= 5 && i > 1) {
        text = "<span>" + "..." + "</span>";
        i = currentPageId - (OFFSET+1);
        spanBeforeSet = true;
        
      }
       else if (!spanAfterSet &&  (i-OFFSET) >currentPageId && i > 4) {
        text = "<span>" + "..." + "</span>";
        i = maxPageId-1;
        spanAfterSet = true;
      } else {
        link = LINK_PREFIX + i;
        if (i == currentPageId) {
          text = "<span>" + i + "</span>";
        } else {
          text = "<a href='" + link + "'>" + i + "</a>";
        }
      }

      li.innerHTML = text;
      $('#navigation').append(li);
    }

    // take care for the next page button
    if (currentPageId >= maxPageId) {
      text = "<span>" + NEXT_TEXT + "</span>";

    } else {
      link = LINK_PREFIX + (++currentPageId);
      text = "<a href='" + link + "'>" + NEXT_TEXT + "</a>";
    }
    li = document.createElement('li');
    li.innerHTML = text;
    $('#navigation').append(li);

  }

  /**
   * @memberOf Opencast.search
   * @description Test the function getCurrentPage
   * @param number time
   */
  function testGetCurrentPage() {
    $('#log').empty().append("Current Page: "+getCurrentPageID());
  }

  /**
   * @memberOf Opencast.search
   * @description Gets the current page ID
   */
  function getCurrentPageID() {
    var value = getGETParameter("page");
    /* if the GET parameter page is not there
     * Assume that we are on page 1
     * otherwise return the page value from the get parameter
     */
    if(value == null)
      return 1; 
    else 
      return value;
  }

  /**
   * @memberOf Opencast.search
   * @description Gets the max page ID
   */
  function getMaxPageID() {
    return 10;
  }

  /**
   * @memberOf Opencast.search
   * @description Get the value of the GET parameter with the passed "name"
   * @param string name
   * @return The value of the GET parameter
   */
  function getGETParameter(name) {
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regexS = "[\\?&]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.href);
    if (results == null)
      return null;
    else
      return results[1];
  }

  return {
    testGetCurrentPage : testGetCurrentPage
  };
}());
