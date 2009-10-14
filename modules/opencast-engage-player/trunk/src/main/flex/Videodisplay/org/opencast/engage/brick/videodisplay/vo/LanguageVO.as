package org.opencast.engage.brick.videodisplay.vo
{
	/**
	* 	LanguageVO
	* 
	*	@author saltevog
	*	@version 1.0
	*/
	[Bindable]
	public class LanguageVO
	{
		public function LanguageVO( short_name : String , long_name : String )
		{
			this.short_name = short_name;
			this.long_name = long_name;
		}

		public var short_name : String;
		public var long_name : String;
		public function toString() : String
		{
			return long_name;
		}
	}
}
