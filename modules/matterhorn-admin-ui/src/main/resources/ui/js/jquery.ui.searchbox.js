(function($) {
  $.widget("ui.searchbox", {

    options : {
      border : false,
      idleText : 'search',
      idleTextColor : 'silver',
      textColor : 'black',
      textBackground : 'white',
      searchIcon : 'ui-icon-search',
      clearIcon : 'ui-icon-close'
    },

    _version : 'searchbox v0.1',

    _css :
    '.ui-searchbox {padding:5px;position:relative;} \n' +
    '.ui-searchbox .searchbox-search-icon {float:left;margin-right:5px;position:absolute;top:50%;margin-top:-8px;cursor:pointer;} \n' +
    '.ui-searchbox .searchbox-text-container {float:right;} \n' +
    '.ui-searchbox .searchbox-text-container .searchbox-clear-icon {position:absolute;right:5px;top:50%;margin-top:-8px;cursor:pointer;} \n' +
    '.ui-searchbox .searchbox-text-container .searchbox-text-input {float:left;border:none;padding-left:7px;}',

    _markup :
    '<span class="searchbox-search-icon ui-icon"></span>' +
    '<span class="searchbox-text-container ui-corner-all ui-helper-clearfix">' +
    '  <input type="text" class="searchbox-text-input ui-corner-all">' +
    '  <span class="searchbox-clear-icon ui-icon"></span>' +
    '</span>',

    clear : function() {
      this._trigger('clear');
    },
    
    search : function(t) {
//      var callback = this.options.search;
//      if ($.isFunction(callback))
    },

    /** creation function *
     */
    _create : function() {
      var self = this;

      // inject CSS if this hasn't been done yet
      this._ensureCSS();

      // prepare container
      var width = this.element.width();
      if (width < 140) {
        width = 140;
      }
      this.element.addClass('ui-searchbox ui-widget ui-state-default ui-corner-all ui-helper-clearfix');
      if (this.options.border) {
        this.element.css('border', this.options.border);
      }

      // prepare markup
      $(this._markup).appendTo(this.element);
      this.element.find('.searchbox-text-container').css('background', this.options.textBackground);

      this.element.find('.searchbox-clear-icon')
      .addClass(this.options.clearIcon)
      .click(function(event) {
        self.element.find('input').val('');
        self.clear();
      });

      this.element.find('.searchbox-search-icon').addClass(this.options.searchIcon)
      .click(function(event) {
        var t = self.element.find('input').val();
        if (t == '') {
          self.element.find('input').focus();
        } else {
          self.search(t);
        }
      });

      // text input
      this.options._input = this.element.find('.searchbox-text-input')
      .css({
        'width' : width - 22,
        'color' : this.options.textColor,
        'background' : this.options.textBackground
      })
      .keydown(function(event) {
        if (event.keyCode) {
          var input = self.element.find('input');
          if (event.keyCode == $.ui.keyCode.ENTER) {
            self.search(input.val());
          } 
        }
      });
    },

    _ensureCSS : function() {
      if ($('head').data('ui-searchbox-css') !== this._version) {
        $('<style type="text/css"></style>')
        .attr('title', 'CSS for ' + this._version)
        .text(this._css)
        .appendTo('head');
      }
    }

  });
})(jQuery);
