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
	import mx.core.Application;

	import org.opencast.engage.videodisplay.control.util.OpencastMediaPlayer;
	import org.opencast.engage.videodisplay.control.util.TimeCode;
	import org.opencast.engage.videodisplay.state.CoverState;
	import org.opencast.engage.videodisplay.state.MediaState;
	import org.opencast.engage.videodisplay.state.SoundState;
	import org.opencast.engage.videodisplay.state.VideoSizeState;
	import org.opencast.engage.videodisplay.state.VideoState;
	import org.opencast.engage.videodisplay.vo.LanguageVO;
	import org.osmf.containers.MediaContainer;
	import org.osmf.layout.LayoutMetadata;

	[Bindable]
	public class VideodisplayModel
	{

		/**
		 * Constructor
		 */
		public function VideodisplayModel()
		{
		}

		// MULTIPLAYER
		public var MULTIPLAYER:String="Multiplayer";

		// SINGLEPLAYER
		public var SINGLEPLAYER:String="Singleplayer";

		// SINGLEPLAYERWITHSLIDES
		public var SINGLEPLAYERWITHSLIDES:String="SingleplayerWithSlides";

		// AUDIOPLAYER
		public var AUDIOPLAYER:String="Audioplayer";

		// HTML
		public var HTML:String='html';

		// RTMP
		public var RTMP:String='rtmp';

		// playerMode
		public var playerMode:String='';

		// slideLength
		public var slideLength:int;

		// audioURLaudioURL
		public var audioURL:String="";

		// An Array with different caption data
		public var captionSets:ArrayCollection;

		// Height of the captions
		public var captionsHeight:int=50;

		// Close Caption Boolean
		public var ccBoolean:Boolean=false;

		// Close Caption Button Boolean
		public var ccButtonBoolean:Boolean=false;

		// CC Button
		public var ccButtonBool:Boolean=false;

		// Current Caption Set
		public var currentCaptionSet:Array;

		// Current Duration
		public var currentDuration:Number=0;

		// durationPlayerOne
		public var durationPlayerOne:Number;

		// durationPlayerTwo
		public var durationPlayerTwo:Number;

		// Current Duration String
		public var currentDurationString:String='';

		// Current Player State
		public var currentPlayerState:String;

		// Current Playhead
		public var currentPlayhead:Number=0;

		// Current PlayheadSingle
		public var currentPlayheadSingle:Number=0;

		// currentPlayheadPlayerOne
		public var currentPlayheadPlayerOne:Number=0;

		// currentPlayheadPlayerTwo
		public var currentPlayheadPlayerTwo:Number=0;

		// The current Subtitle
		public var currentSubtitle:String='';

		// endIndexSubtitle
		public var endIndexSubtitle:int=90;

		// errorId
		public var errorId:String=''

		// errorMessage
		public var errorMessage:String='';

		// errorDetail
		public var errorDetail:String='';

		// Skip Fast Forward -change time
		public var fastForwardTime:Number=10;

		// Captions font size
		public var fontSizeCaptions:int=12;

		// Fullscreen Mode
		public var fullscreenMode:Boolean=false;

		// An Array width the language from the dfxp file
		public var languageComboBox:Array=new Array();

		// Array of LanguageVO
		public var languages:ArrayCollection=new ArrayCollection([new LanguageVO('de', "German"), new LanguageVO('en', "English"), new LanguageVO('es', "Spain")]);

		// mediaState
		public var mediaState:String=MediaState.MEDIA;

		// videoState
		public var videoState:String=VideoState.COVER;

		// videoSizeState
		public var videoSizeState:String=VideoSizeState.CENTER;

		// The old Subtitle
		public var oldSubtitle:String='';

		// mediaPlayer
		public var mediaPlayer:OpencastMediaPlayer;

		// MediaContainer
		public var mediaContainer:MediaContainer;

		// mediaContainerOne
		public var mediaContainerOne:MediaContainer;

		// mediaContainerTwo
		public var mediaContainerTwo:MediaContainer;

		// layoutMetadataOne
		public var layoutMetadataOne:LayoutMetadata;

		// layoutMetadataTwo
		public var layoutMetadataTwo:LayoutMetadata;

		// player volume
		public var playerVolume:Number=1.0;

		// Progress Bar
		public var progressBar:ProgressBar=new ProgressBar();

		// Rewind Time
		public var rewindTime:Number=10;

		// Time Code
		public var timeCode:TimeCode=new TimeCode();

		// video Volume
		public var videoVolume:Number=1;

		// captionsURL
		public var captionsURL:String='';

		// bytesTotal
		public var bytesTotal:Number=0;

		// bytesTotalOne
		public var bytesTotalOne:Number=0;

		// bytesTotalTwo
		public var bytesTotalTwo:Number=0;

		// bytesLoaded
		public var bytesLoaded:Number=0;

		// bytesLoadedOne
		public var bytesLoadedOne:Number=0;

		// bytesLoadedTwo
		public var bytesLoadedTwo:Number=0;

		// progress
		public var progress:Number=0;

		// progressFullscreen
		public var progressFullscreen:Number=0;

		// progressMediaOne
		public var progressMediaOne:Number=0;

		// progressMediaTwo
		public var progressMediaTwo:Number=0;

		// soundState
		public var soundState:String=SoundState.VOLUMEMAX;

		// loader
		public var loader:Boolean=false;

		// mediaTypeSingle
		public var mediaTypeSingle:String;

		// mediaTypeOne
		public var mediaTypeOne:String;

		// mediaTypeTwo
		public var mediaTypeTwo:String;

		// mediaType
		public var mediaType:String;

		// mediaContainerLeftWidth
		public var mediaContainerLeftWidth:int=(Application.application.width - 10) / 2;

		// mediaContainerRightWidth
		public var mediaContainerRightWidth:int=(Application.application.width - 10) / 2;

		// coverURLOne
		public var coverURLOne:String='';


		// coverURLTwo
		public var coverURLTwo:String='';

		// coverURLSingle
		public var coverURLSingle:String='';

		// mediaOneWidth
		public var mediaOneWidth:int=0;

		// mediaOneHeight
		public var mediaOneHeight:int=0;

		// formatMediaOne
		public var formatMediaOne:Number=0;

		// mediaTwoWidth
		public var mediaTwoWidth:int=0;

		// mediaTwoHeight
		public var mediaTwoHeight:int=0;

		// formatMediaTwo
		public var formatMediaTwo:Number=0;

		// mediaWidth
		public var mediaWidth:int=0;

		// multiMediaContainerLeft
		public var multiMediaContainerLeft:int=0;

		// multiMediaContainerLeftFullscreen
		public var multiMediaContainerLeftNormalscreen:int=0;

		// multiMediaContainerRight
		public var multiMediaContainerRight:int=0;

		// multiMediaContainerRightFullscreen
		public var multiMediaContainerRightNormalscreen:int=0;

		// multiMediaContainerBottom
		public var multiMediaContainerBottom:int=0;

		// stateSinglePlayer
		public var stateSinglePlayer:String='';

		// statePlayerOne
		public var statePlayerOne:String='';

		// previewPlayer
		public var previewPlayer:Boolean=false;

		// statePlayerOne
		public var statePlayerTwo:String='';

		// startPlay
		public var startPlay:Boolean=false;

		// startSeek
		public var startSeek:Number=0;

		// startPlayOne
		public var startPlayOne:Boolean=false;

		// startPlayTwo
		public var startPlayTwo:Boolean=false;

		// startPlaySingle
		public var startPlaySingle:Boolean=false;

		// singleState
		public var singleState:String='';

		// coverState
		public var coverState:String=CoverState.ONECOVER;

		// onBufferingChangeMediaOne
		public var onBufferingChangeMediaOne:Boolean;

		// onBufferChangeMediaOneTime
		public var onBufferChangeMediaOneTime:Number;

		// onBufferingChangeMediaTwo
		public var onBufferingChangeMediaTwo:Boolean;

		// onBufferChangeMediaTwoTime
		public var onBufferChangeMediaTwoTime:Number;

		// fullscreenThumbDrag
		public var fullscreenThumbDrag:Boolean=false;

		// currentSeekPosition
		public var currentSeekPosition:Number=0;

		// fullscreenProgressWidth
		public var fullscreenProgressWidth:Number=575;

		// playerSeekBool
		public var playerSeekBool:Boolean=false;

	}
}


