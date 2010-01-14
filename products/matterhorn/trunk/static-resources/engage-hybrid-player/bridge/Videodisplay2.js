/**
 * Application "Videodisplay.mxml"
 */

/**
 * The "Videodisplay" javascript namespace. All the functions/variables you
 * have selected under the "Videodisplay.mxml" in the tree will be
 * available as static members of this namespace object.
 */
Videodisplay2 = {};


/**
 * Listen for the instantiation of the Flex application over the bridge
 */
FABridge.addInitializationCallback("b_Videodisplay2", VideodisplayReady);


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
	b_Videodisplay2_root = FABridge["b_Videodisplay2"].root();
	
	// Global functions in the "Videodisplay.mxml" application

	Videodisplay2.play = function() {
		b_Videodisplay2_root.play();
	};

	Videodisplay2.stop = function() {
		b_Videodisplay2_root.stop();
	};

	Videodisplay2.pause = function() {
		b_Videodisplay2_root.pause();
	};
	
	Videodisplay2.passCharCode = function(argInt){
		b_Videodisplay2_root.passCharCode(argInt);
	};
	
	Videodisplay2.passCharCode = function(argInt){
		b_Videodisplay2_root.passCharCode(argInt);
	};
	
	Videodisplay2.setVolume = function(argNumber) {
		b_Videodisplay_root.setVolume(argNumber);
	};

	Videodisplay2.getVolume = function(){
		return b_Videodisplay2_root.getVolume();
	};
	
	Videodisplay2.seek = function(argNumber) {
		b_Videodisplay2_root.seek(argNumber);
	};
	
	Videodisplay2.setLanguage = function(argString) {
		b_Videodisplay2_root.setLanguage(argString);
	};
	
	Videodisplay2.closedCaptions = function(argBool) {
		b_Videodisplay2_root.closedCaptions(argBool);
	};
	
}
