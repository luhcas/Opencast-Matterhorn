package org.opencast.engage.brick.videodisplay.control.event
{
	import flash.events.Event;
	
	public class DisplayCaptionEvent extends Event
	{
		public static var EVENT_NAME:String = 'DisplayCaptionEvent';
		
		private var _newPosition:Number;
		
		public function DisplayCaptionEvent(newPosition:Number, bubbles:Boolean=false, cancelable:Boolean=false)
		{
			super(EVENT_NAME, bubbles, cancelable);
			
			_newPosition = newPosition;
		}
		
		public function get position():Number
		{
			return _newPosition;
		}
	}
}