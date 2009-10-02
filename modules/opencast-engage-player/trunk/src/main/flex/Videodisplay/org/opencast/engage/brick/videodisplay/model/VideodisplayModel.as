package org.opencast.engage.brick.videodisplay.model
{
	import com.adobe.strobe.players.MediaPlayerWrapper;
	[Bindable]
	public class VideodisplayModel
	{
		public var player : MediaPlayerWrapper;
		public var currentDuration : Number;
		public var currentPlayhead : Number;
		public var currentPlayerState : String;
		// Current Caption Set
		public var currentCaptionSet : Array;
		// ------- Constructor -------
		public function VideodisplayModel()
		{
			//implement as singelton
		}
	}
}
