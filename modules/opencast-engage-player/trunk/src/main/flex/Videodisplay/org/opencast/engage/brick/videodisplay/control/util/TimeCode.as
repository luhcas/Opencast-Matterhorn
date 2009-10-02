package org.opencast.engage.brick.videodisplay.control.util
{
	public class TimeCode
	{
		private var _showHours : Boolean;
		public function TimeCode()
		{
			_showHours = true;
		}

		public function showHours(show : Boolean) : void
		{
			_showHours = show;
		}

		public function getTC(seconds : Number) : String
		{
			if(typeof(seconds) != "number")
				return "00:";
			var result : String = "";
			var myTime : Date = new Date(2007 , 0 , 1 , 0 , 0 , seconds);
			// hours
			if (_showHours == true)
			{
				if (myTime.getHours() < 10)
				{
					result = "0" + myTime.getHours() + ":";
				}else
				{
					result = myTime.getHours() + ":";
				}
			}

			// minutes
			if (myTime.getMinutes() < 10)
			{
				result += "0" + myTime.getMinutes() + ":";
			}else
			{
				result += myTime.getMinutes() + ":";
			}

			// seconds
			if (myTime.getSeconds() < 10)
			{
				result += "0" + myTime.getSeconds();
			}else
			{
				result += myTime.getSeconds();
			}

			return result;
		}

		public function getTCompressed(seconds : Number) : String
		{
			if(typeof(seconds) != "number")
				return "00:";
			var result : String = "";
			var myTime : Date = new Date(2007 , 0 , 1 , 0 , 0 , seconds);
			// hours
			if (_showHours == true)
			{
				if (myTime.getHours() < 10)
				{
					if(myTime.getHours() != 0)
						result = "0" + myTime.getHours() + ":";
				}else
				{
					result = myTime.getHours() + ":";
				}
			}

			// minutes
			if (myTime.getMinutes() < 10)
			{
				result += "0" + myTime.getMinutes() + ":";
			}else
			{
				result += myTime.getMinutes() + ":";
			}

			// seconds
			if (myTime.getSeconds() < 10)
			{
				result += "0" + myTime.getSeconds();
			}else
			{
				result += myTime.getSeconds();
			}

			return result;
		}
	}
}
