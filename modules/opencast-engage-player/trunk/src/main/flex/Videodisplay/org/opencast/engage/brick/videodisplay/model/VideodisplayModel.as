package org.opencast.engage.brick.videodisplay.model
{
	import com.adobe.strobe.players.MediaPlayerWrapper;
	import mx.collections.ArrayCollection;
	import mx.core.Application;
	import org.opencast.engage.brick.videodisplay.vo.LanguageVO;
	[Bindable]
	public class VideodisplayModel
	{
		public var player : MediaPlayerWrapper;
		public var currentDuration : Number;
		public var currentPlayhead : Number;
		public var currentPlayerState : String;
		// Current Caption Set
		public var currentCaptionSet : Array;
		// The old Subtitle
		public var oldSubtitle : String = '';
		// The current Subtitle
		public var currentSubtitle : String = '';
		// Font Size of the Captions
		public var fontSizeCaptions : int = 16;
		// Height of the captions
		public var captionsHeight : int = 50;
		// An Array with the different captions
		public var captionSets : ArrayCollection = new ArrayCollection();
		// Array of LanguageVO
		public var languages : ArrayCollection = new ArrayCollection( [new LanguageVO('de' , 'German') , new LanguageVO('en' , 'English') , new LanguageVO('es' , 'Spain')] );
		// The width of the Videodisplay
		public var videodisplayWidth : int = Application.application.width;
		// ------- Constructor -------
		public function VideodisplayModel()
		{
			//implement as singelton
		}
	}
}
