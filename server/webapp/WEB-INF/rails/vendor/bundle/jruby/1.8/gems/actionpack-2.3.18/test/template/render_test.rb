# encoding: utf-8
require 'abstract_unit'
require 'controller/fake_models'

module RenderTestCases
  def setup_view(paths)
    @assigns = { :secret => 'in the sauce' }
    @view = ActionView::Base.new(paths, @assigns)

    # Reload and register danish language for testing
    I18n.reload!
    I18n.backend.store_translations 'da', {}
    I18n.backend.store_translations 'pt-BR', {}

    # Ensure original are still the same since we are reindexing view paths
    assert_equal ORIGINAL_LOCALES, I18n.available_locales.map(&:to_s).sort
  end

  def test_render_file
    assert_equal "Hello world!", @view.render(:file => "test/hello_world.erb")
  end

  def test_render_file_not_using_full_path
    assert_equal "Hello world!", @view.render(:file => "test/hello_world.erb")
  end

  def test_render_file_without_specific_extension
    assert_equal "Hello world!", @view.render(:file => "test/hello_world")
  end

  def test_render_file_with_localization
    old_locale = I18n.locale
    I18n.locale = :da
    assert_equal "Hey verden", @view.render(:file => "test/hello_world")
  ensure
    I18n.locale = old_locale
  end

  def test_render_file_with_dashed_locale
    old_locale = I18n.locale
    I18n.locale = :"pt-BR"
    assert_equal "Ola mundo", @view.render(:file => "test/hello_world")
  ensure
    I18n.locale = old_locale
  end

  def test_render_implicit_html_template_from_xhr_request
    old_format = @view.template_format
    @view.template_format = :js
    assert_equal "Hello HTML!", @view.render(:file => "test/render_implicit_html_template_from_xhr_request")
  ensure
    @view.template_format = old_format
  end

  def test_render_implicit_html_template_from_xhr_request_with_localization
    old_locale = I18n.locale
    old_format = @view.template_format
    I18n.locale = :da
    @view.template_format = :js
    assert_equal "Hey HTML!\n", @view.render(:file => "test/render_implicit_html_template_from_xhr_request")
  ensure
    I18n.locale = old_locale
    @view.template_format = old_format
  end

  def test_render_file_at_top_level
    assert_equal 'Elastica', @view.render(:file => '/shared')
  end

  def test_render_file_with_full_path
    template_path = File.join(File.dirname(__FILE__), '../fixtures/test/hello_world.erb')
    assert_equal "Hello world!", @view.render(:file => template_path)
  end

  def test_render_file_with_instance_variables
    assert_equal "The secret is in the sauce\n", @view.render(:file => "test/render_file_with_ivar.erb")
  end

  def test_render_file_with_locals
    locals = { :secret => 'in the sauce' }
    assert_equal "The secret is in the sauce\n", @view.render(:file => "test/render_file_with_locals.erb", :locals => locals)
  end

  def test_render_file_not_using_full_path_with_dot_in_path
    assert_equal "The secret is in the sauce\n", @view.render(:file => "test/dot.directory/render_file_with_ivar")
  end

  def test_render_has_access_current_template
    assert_equal "test/template.erb", @view.render(:file => "test/template.erb")
  end

  def test_render_update
    # TODO: You should not have to stub out template because template is self!
    @view.instance_variable_set(:@template, @view)
    assert_equal 'alert("Hello, World!");', @view.render(:update) { |page| page.alert('Hello, World!') }
  end

  def test_render_partial_from_default
    assert_equal "only partial", @view.render("test/partial_only")
  end

  def test_render_partial
    assert_equal "only partial", @view.render(:partial => "test/partial_only")
  end

  def test_render_partial_with_format
    assert_equal 'partial html', @view.render(:partial => 'test/partial')
  end

  def test_render_partial_at_top_level
    # file fixtures/_top_level_partial_only.erb (not fixtures/test)
    assert_equal 'top level partial', @view.render(:partial => '/top_level_partial_only')
  end

  def test_render_partial_with_format_at_top_level
    # file fixtures/_top_level_partial.html.erb (not fixtures/test, with format extension)
    assert_equal 'top level partial html', @view.render(:partial => '/top_level_partial')
  end

  def test_render_partial_with_locals
    assert_equal "5", @view.render(:partial => "test/counter", :locals => { :counter_counter => 5 })
  end

  def test_render_partial_with_locals_from_default
    assert_equal "only partial", @view.render("test/partial_only", :counter_counter => 5)
  end

  def test_render_partial_with_errors
    @view.render(:partial => "test/raise")
    flunk "Render did not raise TemplateError"
  rescue ActionView::TemplateError => e
    assert_match "undefined local variable or method `doesnt_exist'", e.message
    assert_equal "", e.sub_template_message
    assert_equal "1", e.line_number
    assert_equal File.expand_path("#{FIXTURE_LOAD_PATH}/test/_raise.html.erb"), e.file_name
  end

  def test_render_sub_template_with_errors
    @view.render(:file => "test/sub_template_raise")
    flunk "Render did not raise TemplateError"
  rescue ActionView::TemplateError => e
    assert_match "undefined local variable or method `doesnt_exist'", e.message
    assert_equal "Trace of template inclusion: #{File.expand_path("#{FIXTURE_LOAD_PATH}/test/sub_template_raise.html.erb")}", e.sub_template_message
    assert_equal "1", e.line_number
    assert_equal File.expand_path("#{FIXTURE_LOAD_PATH}/test/_raise.html.erb"), e.file_name
  end

  def test_render_object
    assert_equal "Hello: david", @view.render(:partial => "test/customer", :object => Customer.new("david"))
  end

  def test_render_partial_collection
    assert_equal "Hello: davidHello: mary", @view.render(:partial => "test/customer", :collection => [ Customer.new("david"), Customer.new("mary") ])
  end

  def test_render_partial_collection_as
    assert_equal "david david davidmary mary mary",
      @view.render(:partial => "test/customer_with_var", :collection => [ Customer.new("david"), Customer.new("mary") ], :as => :customer)
  end

  def test_render_partial_collection_without_as
    assert_equal "local_inspector,local_inspector_counter,object",
      @view.render(:partial => "test/local_inspector", :collection => [ Customer.new("mary") ])
  end

  def test_render_partial_with_empty_collection_should_return_nil
    assert_nil @view.render(:partial => "test/customer", :collection => [])
  end

  def test_render_partial_with_nil_collection_should_return_nil
    assert_nil @view.render(:partial => "test/customer", :collection => nil)
  end

  def test_render_partial_with_nil_values_in_collection
    assert_equal "Hello: davidHello: Anonymous", @view.render(:partial => "test/customer", :collection => [ Customer.new("david"), nil ])
  end

  def test_render_partial_with_empty_array_should_return_nil
    assert_nil @view.render(:partial => [])
  end

  # TODO: The reason for this test is unclear, improve documentation
  def test_render_partial_and_fallback_to_layout
    assert_equal "Before (Josh)\n\nAfter", @view.render(:partial => "test/layout_for_partial", :locals => { :name => "Josh" })
  end

  # TODO: The reason for this test is unclear, improve documentation
  def test_render_missing_xml_partial_and_raise_missing_template
    @view.template_format = :xml
    assert_raise(ActionView::MissingTemplate) { @view.render(:partial => "test/layout_for_partial") }
  end

  def test_render_inline
    assert_equal "Hello, World!", @view.render(:inline => "Hello, World!")
  end

  def test_render_inline_with_locals
    assert_equal "Hello, Josh!", @view.render(:inline => "Hello, <%= name %>!", :locals => { :name => "Josh" })
  end

  def test_render_fallbacks_to_erb_for_unknown_types
    assert_equal "Hello, World!", @view.render(:inline => "Hello, World!", :type => :bar)
  end

  CustomHandler = lambda do |template|
    "@output_buffer = ''\n" +
      "@output_buffer << 'source: #{template.source.inspect}'\n"
  end

  def test_render_inline_with_compilable_custom_type
    ActionView::Template.register_template_handler :foo, CustomHandler
    assert_equal 'source: "Hello, World!"', @view.render(:inline => "Hello, World!", :type => :foo)
  end

  def test_render_inline_with_locals_and_compilable_custom_type
    ActionView::Template.register_template_handler :foo, CustomHandler
    assert_equal 'source: "Hello, <%= name %>!"', @view.render(:inline => "Hello, <%= name %>!", :locals => { :name => "Josh" }, :type => :foo)
  end

  class LegacyHandler < ActionView::TemplateHandler
    def render(template, local_assigns)
      "source: #{template.source}; locals: #{local_assigns.inspect}"
    end
  end

  def test_render_legacy_handler_with_custom_type
    ActionView::Template.register_template_handler :foo, LegacyHandler
    assert_equal 'source: Hello, <%= name %>!; locals: {:name=>"Josh"}', @view.render(:inline => "Hello, <%= name %>!", :locals => { :name => "Josh" }, :type => :foo)
  end

  def test_render_ignores_templates_with_malformed_template_handlers
    %w(malformed malformed.erb malformed.html.erb malformed.en.html.erb).each do |name|
      assert_raise(ActionView::MissingTemplate) { @view.render(:file => "test/malformed/#{name}") }
    end
  end

  def test_template_with_malformed_template_handler_is_reachable_through_its_exact_filename
    assert_equal "Don't render me!", @view.render(:file => 'test/malformed/malformed.html.erb~')
  end

  def test_render_with_layout
    assert_equal %(<title></title>\nHello world!\n),
      @view.render(:file => "test/hello_world.erb", :layout => "layouts/yield")
  end

  def test_render_with_nested_layout
    assert_equal %(<title>title</title>\n<div id="column">column</div>\n<div id="content">content</div>\n),
      @view.render(:file => "test/nested_layout.erb", :layout => "layouts/yield")
  end
end

module TemplatesSetupTeardown
  def setup_view_paths_for(new_cache_template_loading)
    @previous_cache_template_loading, ActionView::Base.cache_template_loading = ActionView::Base.cache_template_loading, new_cache_template_loading
    view_paths = new_cache_template_loading ? CACHED_VIEW_PATHS : ActionView::Base.process_view_paths(CACHED_VIEW_PATHS.map(&:to_s))
    assert_equal(new_cache_template_loading ? ActionView::Template::EagerPath : ActionView::ReloadableTemplate::ReloadablePath, view_paths.first.class)
    setup_view(view_paths)
  end
  
  def teardown
    ActionView::Base.cache_template_loading = @previous_cache_template_loading
  end
end

class CachedRenderTest < Test::Unit::TestCase
  include TemplatesSetupTeardown
  include RenderTestCases

  def setup
    setup_view_paths_for(cache_templates = true)
  end
end

class ReloadableRenderTest < Test::Unit::TestCase
  include TemplatesSetupTeardown
  include RenderTestCases

  def setup
    setup_view_paths_for(cache_templates = false)
  end

  if '1.9'.respond_to?(:force_encoding)
    def test_render_utf8_template_with_magic_comment
      with_external_encoding Encoding::ASCII_8BIT do
        result = @view.render(:file => "test/utf8_magic.html.erb", :layouts => "layouts/yield")
        assert_equal Encoding::UTF_8, result.encoding
        assert_equal "Русский текст\n\nUTF-8\nUTF-8\nUTF-8\n", result
      end
    end

    def test_render_utf8_template_with_default_external_encoding
      with_external_encoding Encoding::UTF_8 do
        result = @view.render(:file => "test/utf8.html.erb", :layouts => "layouts/yield")
        assert_equal Encoding::UTF_8, result.encoding
        assert_equal "Русский текст\n\nUTF-8\nUTF-8\nUTF-8\n", result
      end
    end

    def test_render_utf8_template_with_incompatible_external_encoding
      with_external_encoding Encoding::SJIS do
        begin
          result = @view.render(:file => "test/utf8.html.erb", :layouts => "layouts/yield")
          flunk 'Should have raised incompatible encoding error'
        rescue ActionView::TemplateError => error
          assert_match 'invalid byte sequence in Shift_JIS', error.original_exception.message
        end
      end
    end

    def test_render_utf8_template_with_partial_with_incompatible_encoding
      with_external_encoding Encoding::SJIS do
        begin
          result = @view.render(:file => "test/utf8_magic_with_bare_partial.html.erb", :layouts => "layouts/yield")
          flunk 'Should have raised incompatible encoding error'
        rescue ActionView::TemplateError => error
          assert_match 'invalid byte sequence in Shift_JIS', error.original_exception.message
        end
      end
    end

    def with_external_encoding(encoding)
      old, Encoding.default_external = Encoding.default_external, encoding
      yield
    ensure
      Encoding.default_external = old
    end
  end
end

