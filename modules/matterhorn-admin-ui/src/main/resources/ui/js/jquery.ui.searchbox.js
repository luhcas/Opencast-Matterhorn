(function($) {
  $.widget("ui.searchbox", {

    options : {
      border : false,
      searchText : '',
      idleTextColor : 'silver',
      textColor : 'black',
      textBackground : 'white',
      searchIcon : 'ui-icon-search',
      clearIcon : 'ui-icon-close',
      options : null,
      selectedOption : null
    },

    _version : 'searchbox v0.1',

    /*    _css :
    '.ui-searchbox {padding:5px;position:relative;} \n' +
    '.ui-searchbox .searchbox-search-icon {float:left;margin-right:5px;position:absolute;top:50%;margin-top:-8px;cursor:pointer;} \n' +
    '.ui-searchbox .searchbox-text-container {float:left;margin-right:5px;position:absolute;top:50%;margin-top:-8px;} \n' +
    '.ui-searchbox select {float:right;}' + 
    '.ui-searchbox .searchbox-text-container .searchbox-clear-icon {float:right;top:50%;margin-top:-8px;cursor:pointer;} \n' +
    '.ui-searchbox .searchbox-text-container .searchbox-text-input {float:left;border:none;padding-left:7px;}',
*/
    _css :
    '.ui-searchbox {padding:5px;} \n' +
    '.ui-searchbox .searchbox-search-icon {float:left;cursor:pointer;margin-right:3px;} \n' +
    '.ui-searchbox .searchbox-text-container {float:left;} \n' +
    '.ui-searchbox select {float:right;}' +
    '.ui-searchbox .searchbox-text-container .searchbox-clear-icon {float:right;} \n' +
    '.ui-searchbox .searchbox-text-container .searchbox-text-input {float:left;border:none;}',

    _markup :
    '<span class="searchbox-search-icon ui-icon"></span>' +
    '<span class="searchbox-text-container ui-corner-all ui-helper-clearfix">' +
    '  <input type="text" class="searchbox-text-input ui-corner-all">' +
    '  <span class="searchbox-clear-icon ui-icon"></span>' +
    '</span>',

    clear : function() {
      if (this.options.clear !== undefined) {
        this.options.clear();
      }
    },
    
    search : function(t) {
      if (this.options.search !== undefined && $.isFunction(this.options.search)) {
        if (this.options.options !== undefined) {
          this.options.search(this.options._input.val(), this.element.find('select').val());
        } else {
          this.options.search(this.options._input.val());
        }
      }
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

      // options dropdown
      if (this.options.options != null) {
        var dropdown = $('<select></select>');
        var selected = this.options.selectedOption
        $.each(this.options.options, function(key, val) {
          var opt = $('<option></option>').attr('value', key).text(val);
          if (selected != null && key == selected) {
            opt.attr('selected', 'selected');
          }
          opt.appendTo(dropdown);
        });
        this.element.append(dropdown);
      }

      this.element.find('.searchbox-search-icon').addClass(this.options.searchIcon)
      .click(function(event) {
        var t = self.element.find('input').val();
        if (t == '') {
          self.element.find('input').focus();
        } else {
          self.search(t);
        }
      });

      // correct layout
      var textboxwidth = this.element.innerWidth() - this.element.find('.searchbox-search-icon').outerWidth(true) - 5;
      if (dropdown !== undefined) {
        textboxwidth -= dropdown.outerWidth(true) + 9;
        this.element.find('.searchbox-text-container').height(dropdown.outerHeight());
      }
      this.element.find('.searchbox-text-container').css('width', textboxwidth);
      var inputheight = this.element.find('.searchbox-text-container input').outerHeight();
      var inputContainerHeight = this.element.find('.searchbox-text-container').innerHeight();
      this.element.find('.searchbox-text-container input').css('width', textboxwidth-16);
      this.element.find('.searchbox-text-container input').css('margin-top', (inputContainerHeight - inputheight) / 2);
      this.element.find('.searchbox-search-icon').css('margin-top', (this.element.innerHeight() - 10 - 16) / 2);
      this.element.find('.searchbox-clear-icon').css('margin-top', (inputContainerHeight - 16) / 2);


      // text input
      this.options._input = this.element.find('.searchbox-text-input')
      .css({
        'color' : this.options.textColor,
        'background' : this.options.textBackground
      })
      .val(this.options.searchText)
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
