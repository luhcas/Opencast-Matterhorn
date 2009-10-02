package org.opencast.engage.brick.videodisplay.control.event
{
	import flash.events.Event;
	public class SetVolumeEvent extends Event
	{
		public static var EVENT_NAME : String = 'SetVolumeEvent';
		private var _volume : Number;
		public function SetVolumeEvent(volume : Number , bubbles : Boolean = false , cancelable : Boolean = false )
		{
			super(EVENT_NAME , bubbles , cancelable);
			_volume = volume;
		}

		public function get volume() : Number
		{
			return _volume;
		}
	}
}
