/**
 * Application "Videodisplay.mxml"
 */

/**
 * The "Videodisplay" javascript namespace. All the functions/variables you
 * have selected under the "Videodisplay.mxml" in the tree will be
 * available as static members of this namespace object.
 */
VideodisplaySecond = {};


/**
 * Listen for the instantiation of the Flex application over the bridge
 */
FABridge.addInitializationCallback("b_VideodisplaySecond", VideodisplayReady);


/**
 * Hook here all the code that must run as soon as the "Videodisplay" class
 * finishes its instantiation over the bridge.
 *
 * However, using the "VideodisplayReady()" is the safest way, as it will 
 * let Ajax know that involved Flex classes are available for use.
 */
function VideodisplayReady() {

	// Initialize the "root" object. This represents the actual 
	// "Videodisplay.mxml" flex application.
	b_VideodisplaySecond_root = FABridge["b_VideodisplaySecond"].root().getFlexAjaxBridge();
	
	// Global functions in the "Videodisplay.mxml" application

	VideodisplaySecond.play = function() {
		b_VideodisplaySecond_root.play();
	};

	VideodisplaySecond.stop = function() {
		b_VideodisplaySecond_root.stop();
	};

	VideodisplaySecond.pause = function() {
		b_VideodisplaySecond_root.pause();
	};
	
	VideodisplaySecond.skipBackward = function() {
		b_VideodisplaySecond_root.skipBackward();
	};
	
	VideodisplaySecond.rewind = function() {
		b_VideodisplaySecond_root.rewind();
	};
	
	VideodisplaySecond.fastForward = function() {
		b_VideodisplaySecond_root.fastForward();
	};
	
	VideodisplaySecond.skipForward = function() {
		b_VideodisplaySecond_root.skipForward();
	};
	
	VideodisplaySecond.passCharCode = function(argInt){
		b_VideodisplaySecond_root.passCharCode(argInt);
	};
	
	VideodisplaySecond.setVolume = function(argNumber) {
		b_VideodisplaySecond_root.setVolume(argNumber);
	};

	VideodisplaySecond.getVolume = function(){
		return b_VideodisplaySecond_root.getVolume();
	};
	
	VideodisplaySecond.seek = function(argNumber) {
		b_VideodisplaySecond_root.seek(argNumber);
	};
	
	VideodisplaySecond.setLanguage = function(argString) {
		b_VideodisplaySecond_root.setLanguage(argString);
	};
	
	VideodisplaySecond.closedCaptions = function(argBool) {
		b_VideodisplaySecond_root.closedCaptions(argBool);
	};
	
	VideodisplaySecond.setccBool = function(ccBool) {
		b_VideodisplaySecond_root.setccBool(ccBool);
	};

	VideodisplaySecond.setMediaURL = function(argString) {
		b_VideodisplaySecond_root.setMediaURL(argString);
	};
	
	VideodisplaySecond.setCaptionsURL = function(argString) {
		b_VideodisplaySecond_root.setCaptionsURL(argString);
	};
	
	VideodisplaySecond.setPlayerId = function(argString) {
		b_VideodisplaySecond_root.setPlayerId(argString);
	};
	
}
