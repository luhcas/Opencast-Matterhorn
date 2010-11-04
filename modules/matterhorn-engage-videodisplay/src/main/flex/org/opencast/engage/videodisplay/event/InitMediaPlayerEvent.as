package org.opencast.engage.videodisplay.event
{
   import com.adobe.cairngorm.control.CairngormEvent;
   
   import flash.events.Event;

   public class InitMediaPlayerEvent extends CairngormEvent 
   {
      public static const EVENT_NAME : String = "InitMediaPlayerEvent";
      
      private var _mediaURLOne : String;
      private var _mediaURLTwo : String;

      public function InitMediaPlayerEvent( mediaURLOne : String, mediaURLTwo : String, bubbles : Boolean = false, cancelable : Boolean = false )
      {
         super( EVENT_NAME, bubbles, cancelable );
         this._mediaURLOne = mediaURLOne;
         this._mediaURLTwo = mediaURLTwo;
      }

      public override function clone() : Event
      {
         return new InitMediaPlayerEvent( _mediaURLOne, _mediaURLTwo, bubbles, cancelable );
      }
   }
}
