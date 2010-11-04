package org.opencast.engage.videodisplay.control
{
   import org.opencast.engage.videodisplay.event.InitMediaPlayerEvent;
   import com.adobe.cairngorm.control.FrontController;
   import org.opencast.engage.videodisplay.command.InitMediaPlayerCommand;
   import org.opencast.engage.videodisplay.command.MyCommand;
   import org.opencast.engage.videodisplay.event.MyEvent;

   public class VideodisplayController extends FrontController
   {
      private static var UUID : String = "649f31a1-192d-4015-bd6d-8c632687cc71";

      public function VideodisplayController()
      {
         addCommand( MyEvent.EVENT_NAME, MyCommand );
         initialiseCommands();
      }

      private function initialiseCommands() : void
      {
         addCommand( InitMediaPlayerEvent.EVENT_NAME, InitMediaPlayerCommand );
      }
   }
}
