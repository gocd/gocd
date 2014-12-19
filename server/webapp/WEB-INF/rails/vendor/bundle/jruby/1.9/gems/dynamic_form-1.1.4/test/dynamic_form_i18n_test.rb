require 'test_helper'

class DynamicFormI18nTest < Test::Unit::TestCase
  include ActionView::Context
  include ActionView::Helpers::DynamicForm

  attr_reader :request

  def setup
    @object = stub :errors => stub(:count => 1, :full_messages => ['full_messages'])
    @object.stubs :to_model => @object
    @object.stubs :class => stub(:model_name => stub(:human => ""))

    @object_name = 'book_seller'
    @object_name_without_underscore = 'book seller'

    stubs(:content_tag).returns 'content_tag'

    I18n.stubs(:t).with(:'header', :locale => 'en', :scope => [:activerecord, :errors, :template], :count => 1, :model => '').returns "1 error prohibited this  from being saved"
    I18n.stubs(:t).with(:'body', :locale => 'en', :scope => [:activerecord, :errors, :template]).returns 'There were problems with the following fields:'
  end

  def test_error_messages_for_given_a_header_option_it_does_not_translate_header_message
    I18n.expects(:t).with(:'header', :locale => 'en', :scope => [:activerecord, :errors, :template], :count => 1, :model => '').never
    error_messages_for(:object => @object, :header_message => 'header message', :locale => 'en')
  end

  def test_error_messages_for_given_no_header_option_it_translates_header_message
    I18n.expects(:t).with(:'header', :locale => 'en', :scope => [:activerecord, :errors, :template], :count => 1, :model => '').returns 'header message'
    error_messages_for(:object => @object, :locale => 'en')
  end

  def test_error_messages_for_given_a_message_option_it_does_not_translate_message
    I18n.expects(:t).with(:'body', :locale => 'en', :scope => [:activerecord, :errors, :template]).never
    error_messages_for(:object => @object, :message => 'message', :locale => 'en')
  end

  def test_error_messages_for_given_no_message_option_it_translates_message
    I18n.expects(:t).with(:'body', :locale => 'en', :scope => [:activerecord, :errors, :template]).returns 'There were problems with the following fields:'
    error_messages_for(:object => @object, :locale => 'en')
  end
end