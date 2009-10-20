package org.opencast.engage.brick.videodisplay.control.event
{
	import flash.events.Event;
	public class ClosedCaptionsEvent extends Event
	{
		public static var EVENT_NAME : String = 'ClosedCaptionsEvent';
		private var _ccBoolean : Boolean;
		public function ClosedCaptionsEvent(ccBoolean : Boolean , bubbles : Boolean = false , cancelable : Boolean = false)
		{
			super(EVENT_NAME , bubbles , cancelable);
			_ccBoolean = ccBoolean;
		}

		public function get ccBoolean() : Boolean
		{
			return _ccBoolean;
		}
	}
}
