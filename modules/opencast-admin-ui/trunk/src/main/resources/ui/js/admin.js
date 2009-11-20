
var AdminUI = AdminUI || {};

AdminUI.init = function() {

    $('#button_schedule').click( function() {
        window.location.href = '../../scheduler/ui/index.html';
    });

    $('#button_upload').click( function() {
        window.location.href = '../../ingest/ui/index.html';
    })
}