
var ocPager = ocPager || {};

ocPager.pageSize = 20;
ocPager.currentPageIdx = 0;

ocPager.init = function() {
  // Event: change of pagesize selector
  $('.paging-nav-pagesize-selector').change(function() {
    var val = $(this).val();
    $('.paging-nav-pagesize-selector').val(val);
    ocPager.update(val, ocPager.currentPageIdx);
    Recordings.displayRecordings(Recordings.currentState, true);
  });

  // Event: text entered
  $('.paging-nav-goto').keyup(function(event) {
    if (event.keyCode == 13) {
      var val = $(this).val();
      if ((val !== '') && (!isNaN(val))) {
        ocPager.update(ocPager.pageSize, val-1);
        Recordings.displayRecordings(Recordings.currentState, true);
        $(this).val('');
      }
    }
  });

  // Event: pager nav next clicked
  $('.paging-nav-go-next').click(function() {
    ocPager.update(ocPager.pageSize, ocPager.currentPageIdx+1);
    Recordings.displayRecordings(Recordings.currentState, true);
  });

  // Event: pager nav previous clicked
  $('.paging-nav-go-previous').click(function() {
    ocPager.update(ocPager.pageSize, ocPager.currentPageIdx-1);
    Recordings.displayRecordings(Recordings.currentState, true);
  });

  $('.paging-nav-pagesize-selector').each( function() {
    $(this).val(ocPager.pageSize);
  })
}

ocPager.update = function(size, current) {
  ocPager.pageSize = size;
  var numPages = Math.ceil(Recordings.lastCount/size);     // number of pages
  
  if (current >= numPages) {
    current = numPages - 1;
  }
  if (current < 0) {
    current = 0;
  }
  ocPager.currentPageIdx = current;

  // take care for prev and next links
  if (ocPager.currentPageIdx == 0) {
    $('.paging-mocklink-prev').css('display', 'inline');
    $('.paging-nav-go-previous').css('display', 'none');
  } else {
    $('.paging-mocklink-prev').css('display', 'none');
    $('.paging-nav-go-previous').css('display', 'inline');
  }
  if (ocPager.currentPageIdx >= numPages-1) {
    $('.paging-mocklink-next').css('display','inline');
    $('.paging-nav-go-next').css('display','none');
  } else {
    $('.paging-mocklink-next').css('display','none');
    $('.paging-nav-go-next').css('display','inline');
  }

  // populate UI fields
  $('.paging-nav-current').each(function() {
    $(this).text(ocPager.currentPageIdx+1);
  });
  $('.paging-nav-total').each(function() {
    $(this).text(numPages);
  });
  //alert("pageSize: " + ocPager.pageSize + "\ncurrent: " + ocPager.currentPageIdx + "\nitems: " + Recordings.lastCount + "\npages: " + numPages + "\nstate: " + Recordings.currentState);
}

