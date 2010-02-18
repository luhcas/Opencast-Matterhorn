module("play/pause state");

test("module without setup/teardown (default)", function() {
	same(typeof Opencast, "object");
	same(typeof Opencast.Player, "object");
});

test("default play/pause state", function() {
	same(Opencast.Player.getCurrentPlayPauseState(), "pausing");
});

test("setting and getting play/pause state", function() {
	Opencast.Player.setCurrentPlayPauseState("playing");
	same(Opencast.Player.getCurrentPlayPauseState(), "playing");
});

module("mute/unmute");

test("sample test", function () {
	ok(true);
});
