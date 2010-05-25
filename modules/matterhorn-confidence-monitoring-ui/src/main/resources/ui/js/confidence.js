var CAPTURE_AGENT_CONFIDENCE_MONITORING_URL = "/confidence/rest";
var AudioBar = {} || AudioBar;
var Monitor = {} || Monitor;
Monitor.intervalImgId = null;
Monitor.intervalAudioId = null;
Monitor.selectedVideoDevice = null;
Monitor.selectedAudioDevice = null;
Monitor.devices = [];

Monitor.loadDevices = function(){
  //load the devices
  $.get(CAPTURE_AGENT_CONFIDENCE_MONITORING_URL + "/devices", function(data){
    //do stuff to make a device array.
    var devices = $('ns1\\:agent-device', data).toArray();
    log(devices);
    Monitor.devices = [];
    for(d in devices){
   	  var devName = $('name', devices[d]).text();
   	  var devType = $('type', devices[d]).text();
   	  var i = Monitor.devices.push({name: devName, type: devType});
      $('#device_tab').append('<li onclick="Monitor.selectDevice(' + (i - 1) + ')">' + devName + '</li>');
    }
	});
}

Monitor.selectDevice = function(index){
	var device = Monitor.devices[index];
	if(device && device.type && device.name){
		switch(device.type){
			case 'video':
				Monitor.videoDevice(index);
				break;
			case 'audio':
				Monitor.audioDevice(index);
			  break;
			default:
				log('Bad device selected.');
				break;
		}
	}
}

Monitor.videoDevice = function(index){
	if(Monitor.selectedVideoDevice != index){
		Monitor.selectedVideoDevice = index;
		Monitor.updateImg();
		clearInterval(Monitor.intervalImgId);
	}
	Monitor.intervalImgId = setInterval(Monitor.updateImg, 30000); //30 Second refresh on image.
}

Monitor.audioDevice = function(index){
	if(Monitor.selectedAudioDevice != index){
		Monitor.selectedAudioDevice = index;
		Monitor.updateAudio();
		clearInterval(Monitor.intervalAudioId);
	}
	Monitor.intervalAudioId = setInterval(Monitor.updateAudio, 5000); //5 Second refresh on audio
}

Monitor.updateImg = function(){
	log('update image');
  var imgGrab = CAPTURE_AGENT_CONFIDENCE_MONITORING_URL + "/" + Monitor.devices[Monitor.selectedVideoDevice].name;
  var img = new Image();
  img.src = imgGrab;
  $("#image_preview").empty().append(img);
}

Monitor.updateAudio = function(){
	log('update audio');
	$.get(CAPTURE_AGENT_CONFIDENCE_MONITORING_URL + "/audio/" + Monitor.devices[Monitor.selectedAudioDevice].name + "/0",
	function(data){ 
		var a = data.split('\n');
		var dbLevel = parseFloat(a[1]);
		if(dbLevel && dbLevel != 'NaN'){
			AudioBar.setValue(dbLevel);
		}else{
			log('Bad audio levels', dbLevel);
		}
	});
}

AudioBar.setValue = function(dbLevel){
	var level_pct = Math.round((1 - Math.pow(10, dbLevel/20)) * 100);
	log(level_pct, dbLevel)
  $('#left_mask').css('height', level_pct + "%");
  //$('#right_mask').css('height', right_pct + "%");
}

window.log = function(){
  if(window.console){
    try{
      window.console && console.log.apply(console,Array.prototype.slice.call(arguments));
    }catch(e){
      console.log(e);
    }
  }
}