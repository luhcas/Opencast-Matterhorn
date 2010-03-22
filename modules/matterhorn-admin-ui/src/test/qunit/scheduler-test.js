module( "FormField", {
        setup: function(){ 
        },
        teardown: function(){
        }
});

test("FormField creation", function(){
     var field = new FormField();
     ok((field && typeof field === 'object' && field instanceof FormField), "Instantiate empty FormField");
     field.setFormFields('textbox');
     ok((field.fields.textbox && field.fields.textbox[0].type === 'text'), "Add Field");
     field.setFormFieldOpts({required: true, foo: true});
     ok((field.required && field.foo), "FormField Opts set");
});

test("FormField get/set/disp", function(){
     var field = new FormField('textbox');
     same(field.getValue(), 'some text here' , "Run getValue");
});