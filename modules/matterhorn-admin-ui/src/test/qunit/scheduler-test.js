module( "FormField", {
        setup: function(){ 
          var doc = $(document.body);
          doc.append('<input type="text" id="textbox" />');
          doc.append('<input type="checkbox" id="checkbox" checked="checked" value="true" />');
        },
        teardown: function(){
          $(document.body).empty();
        }
});

test("FormField creation", function(){
     var field = new FormField();
     ok((field && typeof field === 'object' && field instanceof FormField), "Instantiate empty FormField");
     field = new FormField('textbox', true);
     ok((field && field.fields.textbox && field.fields.textbox[0].type === 'text'), "Add Field");
});
