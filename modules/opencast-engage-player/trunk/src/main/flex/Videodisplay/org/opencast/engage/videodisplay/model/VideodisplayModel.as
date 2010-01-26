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
package org.opencast.engage.videodisplay.model
{
	import mx.collections.ArrayCollection;
	import mx.controls.ProgressBar;
	import org.opencast.engage.videodisplay.control.util.TimeCode;
	import org.opencast.engage.videodisplay.state.MediaState;
	import org.opencast.engage.videodisplay.vo.LanguageVO;
	import org.osmf.media.MediaPlayer;
	[Bindable]
	public class VideodisplayModel
	{
        
        /** Constructor */		
		public function VideodisplayModel()
        {
        }
        
        // audioURLaudioURL
        public var audioURL:String = "";

		// An Array with different caption data
		public var captionSets : ArrayCollection = new ArrayCollection();

		// Height of the captions
		public var captionsHeight : int = 50;

		// Close Caption Boolean
		public var ccBoolean : Boolean = false;
		
		// CC Button
		public var ccButtonBool:Boolean = false;
		
		// Current Caption Set
		public var currentCaptionSet : Array;
	
	    // Current Duration
		public var currentDuration : Number;
		
		// Current Player State
		public var currentPlayerState : String;
		
		// Current Playhead
		public var currentPlayhead : Number;

		// The current Subtitle
		public var currentSubtitle : String = '';
        
        // error 
        public var error:Error;
		
		// Skip Fast Forward -change time
		public var fastForwardTime:Number = 10;

		// Captions font size
		public var fontSizeCaptions : int = 12;
		
		// Fullscreen Mode
		public var fullscreenMode:Boolean = false;

		// An Array width the language from the dfxp file
		public var languageComboBox : Array = new Array();

		// Array of LanguageVO
		public var languages : ArrayCollection = new ArrayCollection( [new LanguageVO('de' , "German") , new LanguageVO('en' , "English") , new LanguageVO('es' , "Spain")] );
        
        // mediaState
        public var mediaState:String = MediaState.VIDEO;

		// The old Subtitle
		public var oldSubtitle : String = '';
		
		// Media Player Wrapper
		public var player : MediaPlayer;
		
		// player volume
		public var playerVolume:Number = 1.0;
		
	        
        // Progress Bar
        public var progressBar:ProgressBar = new ProgressBar();
		
		// Rewind Time
		public var rewindTime:Number = 10;

		// Skip Backward Time
		public var skipBackwardTime:Number = 0;
		
		// Forward Time
		public var skipForwardTime:Number = 10;
		
		// Time Code
		public var timeCode:TimeCode = new TimeCode();
        
        // video Volume
        public var videoVolume:Number = 1;
	}
}
