/**
 * @fileOverview Functions for the Admin App
 * @name AdminUI
 */

/**
 @namespace Holds all functions for the Admin App
*/
var AdminUI = AdminUI || {};

/**
 * Initialises the event handlers for the buttons of the recordings pages then calls the updateUI function defined in the page
 */
AdminUI.init = function() {

    $('#button_schedule').click( function() {
        window.location.href = '../../scheduler/ui/index.html';
    });

    $('#button_upload').click( function() {
        window.location.href = '../../ingest/ui/index.html';
    })

    updateUI();
}

/**
 * get and display recording statistics
 */
AdminUI.displayRecordingCounts = function() {
  $.getJSON("../rest/countRecordings",
    function(data) {
      for (key in data) {
        var elm = $('#count-' + key);
        if (elm) {
          elm.text('(' + data[key] + ')');
        }
      }
  });
}

/**
 * convert timestamp to locale date string
 * @param timestamp
 * @return Strng localized String representation of timestamp
 */
AdminUI.makeLocaleDateString = function(timestamp) {
  var date = new Date();
  date.setTime(timestamp);
  return date.toLocaleString();
}