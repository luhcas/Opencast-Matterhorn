/**
 * @fileOverview Display popup help for form fields
 * @name Popup Help
 */

/**
 @namespace Holds all the functions neccessary for displaying the popup help
*/
var popupHelp = popupHelp || {};

/** Displays a popup help box for a certain element in the page.
 *  @param element {DOMElement} Element for which the help box should be displayed
 *  @param event {Event} Event to retrieve the current mouse position from
 */
popupHelp.displayHelp = function( element, event ) {
    var id = element.prev().attr('id');
    var title,text;
    if (popupHelp.helpTexts[id]) {
        title = popupHelp.helpTexts[id][0];
        text = popupHelp.helpTexts[id][1];
    } else {
        title = 'Sorry';
        text = "No help defined for field " + id;
    }
    $('#helpTitle').text(title);
    $('#helpText').text(text);
    var help = $('#helpBox');
    help.css({
        left:event.pageX,
        top:event.pageY
    });
    help.fadeIn('fast');
}

/** Hides the popup help box */
popupHelp.resetHelp = function() {
    $('#helpBox').fadeOut('fast');
}
