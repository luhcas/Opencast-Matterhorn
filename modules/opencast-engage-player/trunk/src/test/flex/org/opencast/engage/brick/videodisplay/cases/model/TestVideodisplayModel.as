package org.opencast.engage.brick.videodisplay.cases.model
{	
	import org.flexunit.Assert;
    import org.opencast.engage.brick.videodisplay.model.VideodisplayModel;
    import com.adobe.strobe.players.MediaPlayerWrapper;
    public class TestVideodisplayModel
    {
        private var videoDisplayModel:VideodisplayModel;

        [Before]
        public function setUp():void
        {
            this.videoDisplayModel = new VideodisplayModel();
        }

        [After]
        public function tearDown():void
        {
            this.videoDisplayModel = null;
        }

        [Test]
        public function testPlayerObject():void
        {
            var player : MediaPlayerWrapper;
            this.videoDisplayModel.player = player;
            Assert.assertEquals("VideodisplayModel needs a player object ", 
                                this.videoDisplayModel.player == null, true );
        }
        
        [Test]
        public function testDuration():void
        {
            var duration : Number;
            this.videoDisplayModel.currentDuration = duration;
            Assert.assertEquals("VideodisplayModel needs a player object ", 
                                this.videoDisplayModel.currentDuration != 0, true );
        }

    }
}

