/*FABridge*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
 * Application "Videodisplay.mxml"
 */

/**
 * The "Videodisplay" javascript namespace. All the functions/variables you
 * have selected under the "Videodisplay.mxml" in the tree will be
 * available as static members of this namespace object.
 */
var Videodisplay = Videodisplay || {};


/**
 * Listen for the instantiation of the Flex application over the bridge
 */
FABridge.addInitializationCallback("b_Videodisplay", VideodisplayReady);


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
	b_Videodisplay_root = FABridge["b_Videodisplay"].root().getFlexAjaxBridge();
	
	// Global functions in the "Videodisplay.mxml" application

	Videodisplay.play = function () {
		return b_Videodisplay_root.play();
	};

	Videodisplay.stop = function () {
		b_Videodisplay_root.stop();
	};

	Videodisplay.pause = function () {
		return b_Videodisplay_root.pause();
	};
	
	Videodisplay.skipBackward = function () {
		b_Videodisplay_root.skipBackward();
	};
	
	Videodisplay.rewind = function () {
		b_Videodisplay_root.rewind();
	};
	
	Videodisplay.fastForward = function () {
		b_Videodisplay_root.fastForward();
	};
	
	Videodisplay.skipForward = function () {
		b_Videodisplay_root.skipForward();
	};
	
	Videodisplay.passCharCode = function (argInt) {
		b_Videodisplay_root.passCharCode(argInt);
	};
	
	Videodisplay.seek = function (argNumber) {
		return b_Videodisplay_root.seek(argNumber);
	};
	
	Videodisplay.mute = function () {
		return b_Videodisplay_root.mute();
	};
	
	Videodisplay.setVolumeSlider = function (argNumber) {
		b_Videodisplay_root.setVolumeSlider(argNumber);
	};
	
	Videodisplay.setVolumePlayer = function (argNumber) {
		b_Videodisplay_root.setVolumePlayer(argNumber);
	};
	
	Videodisplay.closedCaptions = function () {
		b_Videodisplay_root.closedCaptions();
	};
	
	Videodisplay.setMediaURL = function (argStringOne, argStringTwo) {
		b_Videodisplay_root.setMediaURL(argStringOne, argStringTwo);
	};
	
	Videodisplay.setCaptionsURL = function (argString) {
		b_Videodisplay_root.setCaptionsURL(argString);
	};
	
	Videodisplay.videoSizeControl = function (argSizeLeft, argSizeRight) {
		b_Videodisplay_root.videoSizeControl(argSizeLeft, argSizeRight);
	};
	
	Videodisplay.getViewState = function () {
		return b_Videodisplay_root.getViewState();
	};
	
	b_Videodisplay_root.onBridgeReady();
}
