package org.opencast.engage.brick.videodisplay.cases.model
{	
	import com.adobe.strobe.players.MediaPlayerWrapper;
	
	import mx.collections.ArrayCollection;
	
	import org.flexunit.Assert;
	import org.opencast.engage.brick.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.brick.videodisplay.vo.LanguageVO;
    
    public class TestVideodisplayModel
    {
    	
        private var videoDisplayModel:VideodisplayModel;
        private var languageVO:LanguageVO;

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
            Assert.assertTrue("VideodisplayModel needs a player object ", 
                                this.videoDisplayModel.player = player, true );                   
                          
        }
        
        
        
        [Test]
        public function testModelAttributes_currentDuration():void
        {
            var currentDuration : Number;
            this.videoDisplayModel.currentDuration = currentDuration;
            Assert.assertEquals("VideodisplayModel needs a currentDuration object ", 
                                this.videoDisplayModel.currentDuration != 0, true );
        }
        
        [Test]
        public function testModelAttributes_currentPlayhead():void
        {
           	var currentPlayhead : Number;
            this.videoDisplayModel.currentPlayhead = currentPlayhead;
            Assert.assertTrue("VideodisplayModel needs currentPlayhead attribute ", 
                                this.videoDisplayModel.currentPlayhead = currentPlayhead, true );
        }
        
        [Test]
        public function testModelAttributes_currentPlayerState():void
        {
           var currentPlayerState : String;
            this.videoDisplayModel.currentPlayerState = currentPlayerState;
            Assert.assertTrue("VideodisplayModel needs currentPlayerState attribute ", 
                                this.videoDisplayModel.currentPlayerState = currentPlayerState, true );
        }
        
        [Test]
        public function testModelAttributes_currentCaptionSet():void
        {
            var currentCaptionSet : Array;
            this.videoDisplayModel.currentCaptionSet = currentCaptionSet;
            Assert.assertTrue("VideodisplayModel needs currentCaptionSet ", 
                                this.videoDisplayModel.currentCaptionSet = currentCaptionSet, true );
        }
        
        [Test]
        public function testModelAttributes_oldSubtitle():void
        {
            var oldSubtitle : String = '';
            this.videoDisplayModel.oldSubtitle = oldSubtitle;
            Assert.assertEquals("VideodisplayModel needs a initial string ", 
                                this.videoDisplayModel.oldSubtitle == '', true );                     
        }
        
        [Test]
        public function testModelAttributes_fontSizeCaptions():void
        {
            var fontSizeCaptions : int = 16;
            Assert.assertEquals("VideodisplayModel needs fontSize 16 for Captions ", 
                                this.videoDisplayModel.fontSizeCaptions == fontSizeCaptions, true );                     
        }
        
        [Test]
        public function testModelAttributes_captionsHeight():void
        {
            var captionsHeight : int = 50;
            Assert.assertEquals("VideodisplayModel needs captionsHeight 50 for Captions ", 
                                this.videoDisplayModel.captionsHeight == captionsHeight, true );                     
        }
        
        
        [Test]
        public function testModelAttributes_captionSets():void
        {
           var captionSets : ArrayCollection;
            this.videoDisplayModel.captionSets = captionSets;
            Assert.assertTrue("VideodisplayModel needs captionSets Array ", 
                                this.videoDisplayModel.captionSets = captionSets, true );
        }
        
        
        [Test]
        public function testModelAttributes_languageComboBox():void
        {
           var languageComboBox : Array;
            this.videoDisplayModel.languageComboBox = languageComboBox;
            Assert.assertTrue("VideodisplayModel needs a languageComboBox Array ", 
                                this.videoDisplayModel.languageComboBox = languageComboBox, true );
        }
        
       	[Test]
        public function testModelAttributes_ccBoolean():void
        {
            var ccBoolean : Boolean = true;
            Assert.assertEquals("VideodisplayModel needs ccBoolean = true ", 
                                this.videoDisplayModel.ccBoolean == ccBoolean, true );                     
        }
        
        [Test]
        public function testModelAttributes_languages():void
        {
            var languages : ArrayCollection;
              Assert.assertTrue("VideodisplayModel needs languageVO ", 
                                this.videoDisplayModel.languages = languages, true );                     
        }
       
       
       
       /* [Test]
        public function testModelAttributes_languages():void
        {
            var test:int = 5;
            var test1:int = 5;
            Assert.assertEquals("simple test for flexmojos ", 
                               test1 == test1, true );                     
        }*/

    }
}

