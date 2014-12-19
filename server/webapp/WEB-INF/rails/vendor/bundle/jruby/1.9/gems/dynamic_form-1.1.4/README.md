DynamicForm
===========

DynamicForm holds a few helpers method to help you deal with your Rails3 models, they are:

* `input(record, method, options = {})`
* `form(record, options = {})`
* `error_message_on(object, method, options={})`
* `error_messages_for(record, options={})`

It also adds `f.error_messages` and `f.error_messages_on` to your form builders.

Read `/lib/action_view/helpers/dynamic_form.rb` for details of each method.

---

DynamicErrors
=============

DynamicForm also includes DynamicErrors, which is a port of the custom-err-messages plugin,
but built to work with Rails3. It gives you the option to not have your custom validation
error message prefixed with the attribute name. Ordinarily, if you have, say:

    validates_acceptance_of :accepted_terms, :message => 'Please accept the terms of service'

You'll get the following error message:

    Accepted terms Please accept the terms of service

This plugin allows you to omit the attribute name for specific messages. All you have to do
is begin the message with a '^' character. Example:

    validates_acceptance_of :accepted_terms, :message => '^Please accept the terms of service'
    
Nigel Ramsay added the ability to specify a proc to generate the message.

    validates_presence_of :assessment_answer_option_id, 
      :message => Proc.new { |aa| "#{aa.label} (#{aa.group_label}) is required" }

    which gives an error message like: Rate (Accuracy) is required

---

Installation
------------

DynamicForm can be installed as a gem in your `Gemfile`:

    gem 'dynamic_form'
    
or as a plugin by running this command:

    rails plugin install git://github.com/joelmoss/dynamic_form.git

---

Copyright (c) 2010 David Heinemeier Hansson, released under the MIT license
