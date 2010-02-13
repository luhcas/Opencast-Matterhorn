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
package org.opencast.engage.videodisplay.control.command
{
    import bridge.ExternalFunction;

    import flash.external.ExternalInterface;

    import org.opencast.engage.videodisplay.control.event.DisplayCaptionEvent;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.opencast.engage.videodisplay.vo.CaptionVO;
    import org.swizframework.Swiz;

    public class DisplayCaptionCommand
    {
        [Autowire]
        public var model:VideodisplayModel;

        /** Constructor */
        public function DisplayCaptionCommand()
        {
            Swiz.autowire( this );
        }

        /** execute
         *
         * The event gives the new position in the video. Find the right captions in the currentCaptionSet and display the captions with the help of the ExternalInterface.
         *
         * @eventType event:DisplayCaptionEvent
         * */
        public function execute( event:DisplayCaptionEvent ):void
        {

            var time:Number = event.position * 1000;
            var tmpCaption:CaptionVO = new CaptionVO();
            var lastPos:int = 0;
            var subtitle:String = '';

            // Find the captions
            if ( model.currentCaptionSet != null )
            {
                for ( var i:int = 0; i < model.currentCaptionSet.length; i++ )
                {
                    tmpCaption = CaptionVO( model.currentCaptionSet[ ( lastPos + i ) % model.currentCaptionSet.length ] );

                    if ( tmpCaption.begin < time && time < tmpCaption.end )
                    {
                        lastPos += i;

                        subtitle = tmpCaption.text;

                        break;
                    }
                }

                // When the learner will see the captions   
                if ( model.ccBoolean )
                {
                    // When the capions are different, than send new captions
                    if ( model.oldSubtitle != subtitle )
                    {
                        model.currentSubtitle = '';
                        ExternalInterface.call( ExternalFunction.SETCAPTIONS, subtitle, model.playerId );
                        model.currentSubtitle = subtitle;
                        model.oldSubtitle = subtitle;

                    }
                }
                else
                {
                    model.currentSubtitle = '';
                    model.oldSubtitle = 'default';
                    ExternalInterface.call( ExternalFunction.SETCAPTIONS, '', model.playerId );
                }
            }
        }
    }
}