package org.opencast.engage.brick.videodisplay.vo
{
	/**
	* 	CaptionVO
	* 
	*	@author jemden
	*	@version 1.0
	*/
	[Bindable]
	public class CaptionVO
	{
		public function CaptionVO()
		{
			super();
		}

		public var begin : Number;
		public var end : Number;
		public var text : String;
		public function toString() : String
		{
			return "\n" + begin + " " + end + " " + text + "\n";
		}
	}
}
