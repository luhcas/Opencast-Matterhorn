/**
 * @fileOverview Functions for the Admin App
 * @name AdminUI
 */

/**
 @namespace Holds all functions for the Admin App
*/
var AdminUI = AdminUI || {};

/** Initialises the event handlers for the buttons of the recordings pages */
AdminUI.init = function() {

    $('#button_schedule').click( function() {
        window.location.href = '../../scheduler/ui/index.html';
    });

    $('#button_upload').click( function() {
        window.location.href = '../../ingest/ui/index.html';
    })
}