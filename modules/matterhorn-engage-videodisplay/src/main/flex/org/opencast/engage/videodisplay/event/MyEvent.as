package org.opencast.engage.videodisplay.event
{
   import com.adobe.cairngorm.control.CairngormEvent;

   import flash.events.Event;
   import mx.controls.Alert;
           

   public class MyEvent extends CairngormEvent 
   {
      public static const EVENT_NAME : String = "myEvent";

      public function MyEvent( bubbles : Boolean = false, cancelable : Boolean = false )
      {
         super( EVENT_NAME, bubbles, cancelable );
      }

      public override function clone() : Event
      {
         return new MyEvent( bubbles, cancelable );
      }
   }
}
