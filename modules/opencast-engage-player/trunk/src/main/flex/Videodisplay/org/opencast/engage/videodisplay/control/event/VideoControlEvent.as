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
package org.opencast.engage.videodisplay.control.event
{
	import flash.events.Event;
	public class VideoControlEvent extends Event
	{
		public static var EVENT_NAME : String 	= 'VideoControlEvent';
		public static var PLAY : String 		= "play";
		public static var PAUSE : String 		= "pause";
		public static var STOP : String 		= "stop";
		public static var SKIPBACKWARD : String = "skipBackward";
		public static var REWIND : String 		= "rewind";
		public static var FASTFORWARD : String 	= "fastForward";
		public static var SKIPFORWARD : String 	= "skipForward";
		
		
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
