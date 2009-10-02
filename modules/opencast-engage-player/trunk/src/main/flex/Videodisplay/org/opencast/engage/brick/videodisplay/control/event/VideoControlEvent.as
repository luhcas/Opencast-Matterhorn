package org.opencast.engage.brick.videodisplay.control.event
{
	import flash.events.Event;
	public class VideoControlEvent extends Event
	{
		public static var EVENT_NAME : String = 'VideoControlEvent';
		public static var PLAY : String = "play";
		public static var PAUSE : String = "pause";
		public static var STOP : String = "stop";
		private var _videoControlType : String;
		public function VideoControlEvent(videoControlType : String , bubbles : Boolean = false , cancelable : Boolean = false )
		{
			super(EVENT_NAME , bubbles , cancelable);
			_videoControlType = videoControlType;
		}

		public function get videoControlType() : String
		{
			return _videoControlType;
		}
	}
}
