/*FABridge, VideodisplayReady*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

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
 * Hook here all the code that must run as soon as the "Videodisplay" class
 * finishes its instantiation over the bridge.
 *
 * However, using the "VideodisplayReady()" is the safest way, as it will 
 * let Ajax know that involved Flex classes are available for use.
 */
function VideodisplayReady() 
{

    // Initialize the "root" object. This represents the actual 
    // "Videodisplay.mxml" flex application.
    var b_Videodisplay_root = FABridge['b_Videodisplay'].root().getFlexAjaxBridge();
    
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
    
    Videodisplay.stopRewind = function () {
        b_Videodisplay_root.stopRewind();
    };
    
    Videodisplay.fastForward = function () {
        b_Videodisplay_root.fastForward();
    };
    
    Videodisplay.stopFastForward = function () {
        b_Videodisplay_root.stopFastForward();
    };
    
    Videodisplay.skipForward = function () {
        b_Videodisplay_root.skipForward();
    };
    
    Videodisplay.passCharCode = function (argInt) {
        b_Videodisplay_root.passCharCode(argInt);
    };
    
    Videodisplay.seek = function (argNumber) {
        var progress = Opencast.engage.getLoadProgress();
        if(progress === -1)
            return b_Videodisplay_root.seek(argNumber);
        else {
            var seekValue = Math.min(argNumber, progress);
            return b_Videodisplay_root.seek(seekValue);
        }
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
    
    Videodisplay.setMediaURL = function (argCoverOne, argCoverTwo, argStringOne, argStringTwo, argMimetypeOne, argMimetypeTwo, argPlayerstyle, slideLength) {
        b_Videodisplay_root.setMediaURL(argCoverOne, argCoverTwo, argStringOne, argStringTwo, argMimetypeOne, argMimetypeTwo, argPlayerstyle, slideLength);
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
    
    Videodisplay.setMediaResolution = function (argWidthMediaOne, argHeightMediaOne, argWidthMediaTwo, argHeightMediaTwo, argMultiMediaContainerLeft) {
        return b_Videodisplay_root.setMediaResolution(argWidthMediaOne, argHeightMediaOne, argWidthMediaTwo, argHeightMediaTwo, argMultiMediaContainerLeft);
    };
    
    
    
    
    
    b_Videodisplay_root.onBridgeReady();
}


/**
 * Listen for the instantiation of the Flex application over the bridge
 */
FABridge.addInitializationCallback("b_Videodisplay", VideodisplayReady);

