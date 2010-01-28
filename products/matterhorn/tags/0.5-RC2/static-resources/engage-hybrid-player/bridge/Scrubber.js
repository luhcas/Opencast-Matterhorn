/**
 * WARNING! THIS IS A GENERATED FILE, AND WILL BE RE-GENERATED EACH TIME THE
 * AJAXBRIDGE IS RUN.
 *
 * You should keep your javascript code inside this file as light as possible, 
 * and rather keep the body of your Ajax application in separate *.js files. 
 *
 * Do make a backup of your changes, before re-generating this file (AjaxBridge 
 * will display a warning message to you).
 *
 * Please refer to the built-in documentation inside the AjaxBridge application 
 * for help on using this file.
 */
 
 
/**
 * Application "Scrubber.mxml"
 */

/**
 * The "Scrubber" javascript namespace. All the functions/variables you
 * have selected under the "Scrubber.mxml" in the tree will be
 * available as static members of this namespace object.
 */
Scrubber = {};


/**
 * Listen for the instantiation of the Flex application over the bridge
 */
FABridge.addInitializationCallback("b_Scrubber", ScrubberReady);


/**
 * Hook here all the code that must run as soon as the "Scrubber" class
 * finishes its instantiation over the bridge.
 *
 * For basic tasks, such as running a Flex method on the click of a javascript
 * button, chances are that both Ajax and Flex may well have loaded before the 
 * user actually clicks the button.
 *
 * However, using the "ScrubberReady()" is the safest way, as it will 
 * let Ajax know that involved Flex classes are available for use.
 */
function ScrubberReady() {

	// Initialize the "root" object. This represents the actual 
	// "Scrubber.mxml" flex application.
	b_Scrubber_root = FABridge["b_Scrubber"].root();
	

	// Global variables in the "Scrubber.mxml" application (converted 
	// to getters and setters)
	
	Scrubber.setPlayhead = function(argNumber) {
		b_Scrubber_root.setPlayhead(argNumber);
	};
	
	Scrubber.setDuration = function(argNumber) {
		b_Scrubber_root.setDuration(argNumber);
	};
	
	Scrubber.scrubberStopDrag = function() {
		b_Scrubber_root.scrubberStopDrag();
	};
}
