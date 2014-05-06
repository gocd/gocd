require 'abstract_unit'

# The view_paths array must be set on Base and not LayoutTest so that LayoutTest's inherited
# method has access to the view_paths array when looking for a layout to automatically assign.
old_load_paths = ActionController::Base.view_paths

ActionView::Template::register_template_handler :mab,
  lambda { |template| template.source.inspect }

ActionController::Base.view_paths = [ File.dirname(__FILE__) + '/../fixtures/layout_tests/' ]

class LayoutTest < ActionController::Base
  def self.controller_path; 'views' end
  self.view_paths = ActionController::Base.view_paths.dup
end

# Restore view_paths to previous value
ActionController::Base.view_paths = old_load_paths

class ProductController < LayoutTest
end

class ItemController < LayoutTest
end

class ThirdPartyTemplateLibraryController < LayoutTest
end

module ControllerNameSpace
end

class ControllerNameSpace::NestedController < LayoutTest
end

class MultipleExtensions < LayoutTest
end

class LayoutAutoDiscoveryTest < ActionController::TestCase
  def setup
    @request.host = "www.nextangle.com"
  end

  def test_application_layout_is_default_when_no_controller_match
    @controller = ProductController.new
    get :hello
    assert_equal 'layout_test.rhtml hello.rhtml', @response.body
  end

  def test_controller_name_layout_name_match
    @controller = ItemController.new
    get :hello
    assert_equal 'item.rhtml hello.rhtml', @response.body
  end

  def test_third_party_template_library_auto_discovers_layout
    @controller = ThirdPartyTemplateLibraryController.new
    get :hello
    assert_equal 'layouts/third_party_template_library.mab', @controller.active_layout.to_s
    assert_equal 'layouts/third_party_template_library', @response.layout
    assert_response :success
    assert_equal 'Mab', @response.body
  end

  def test_namespaced_controllers_auto_detect_layouts
    @controller = ControllerNameSpace::NestedController.new
    get :hello
    assert_equal 'layouts/controller_name_space/nested', @controller.active_layout.to_s
    assert_equal 'controller_name_space/nested.rhtml hello.rhtml', @response.body
  end

  def test_namespaced_controllers_auto_detect_layouts
    @controller = MultipleExtensions.new
    get :hello
    assert_equal 'layouts/multiple_extensions.html.erb', @controller.active_layout.to_s
    assert_equal 'multiple_extensions.html.erb hello.rhtml', @response.body.strip
  end
end

class DefaultLayoutController < LayoutTest
end

class AbsolutePathLayoutController < LayoutTest
  layout File.expand_path(File.expand_path(__FILE__) + '/../../fixtures/layout_tests/layouts/layout_test.rhtml')
end

class AbsolutePathWithoutLayoutsController < LayoutTest
  # Absolute layout path without 'layouts' in it.
  layout File.expand_path(File.expand_path(__FILE__) + '/../../fixtures/layout_tests/abs_path_layout.rhtml')
end

class HasOwnLayoutController < LayoutTest
  layout 'item'
end

class PrependsViewPathController < LayoutTest
  def hello
    prepend_view_path File.dirname(__FILE__) + '/../fixtures/layout_tests/alt/'
    render :layout => 'alt'
  end
end

class SetsLayoutInRenderController < LayoutTest
  def hello
    render :layout => 'third_party_template_library'
  end
end

class RendersNoLayoutController < LayoutTest
  def hello
    render :layout => false
  end
end

class LayoutSetInResponseTest < ActionController::TestCase
  def test_layout_set_when_using_default_layout
    @controller = DefaultLayoutController.new
    get :hello
    assert_equal 'layouts/layout_test', @response.layout
  end

  def test_layout_set_when_set_in_controller
    @controller = HasOwnLayoutController.new
    get :hello
    assert_equal 'layouts/item', @response.layout
  end

  def test_layout_set_when_using_render
    @controller = SetsLayoutInRenderController.new
    get :hello
    assert_equal 'layouts/third_party_template_library', @response.layout
  end

  def test_layout_is_not_set_when_none_rendered
    @controller = RendersNoLayoutController.new
    get :hello
    assert_nil @response.layout
  end

  def test_exempt_from_layout_honored_by_render_template
    ActionController::Base.exempt_from_layout :rhtml
    @controller = RenderWithTemplateOptionController.new

    get :hello
    assert_equal "alt/hello.rhtml", @response.body.strip

  ensure
    ActionController::Base.exempt_from_layout.delete(/\.rhtml$/)
  end

  def test_layout_is_picked_from_the_controller_instances_view_path
    @controller = PrependsViewPathController.new
    get :hello
    assert_equal 'layouts/alt', @response.layout
  end

  def test_absolute_pathed_layout
    @controller = AbsolutePathLayoutController.new
    get :hello
    assert_equal "layout_test.rhtml hello.rhtml", @response.body.strip
  end

  def test_absolute_pathed_layout_without_layouts_in_path
    @controller = AbsolutePathWithoutLayoutsController.new
    get :hello
    assert_equal "abs_path_layout.rhtml hello.rhtml", @response.body.strip
  end
end

class RenderWithTemplateOptionController < LayoutTest
  def hello
    render :template => 'alt/hello'
  end
end

class SetsNonExistentLayoutFile < LayoutTest
  layout "nofile.rhtml"
end

class LayoutExceptionRaised < ActionController::TestCase
  def test_exception_raised_when_layout_file_not_found
    @controller = SetsNonExistentLayoutFile.new
    get :hello
    assert_kind_of ActionView::MissingTemplate, @response.template.instance_eval { @exception }
  end
end

class LayoutStatusIsRendered < LayoutTest
  def hello
    render :status => 401
  end
end

class LayoutStatusIsRenderedTest < ActionController::TestCase
  def test_layout_status_is_rendered
    @controller = LayoutStatusIsRendered.new
    get :hello
    assert_response 401
  end
end

unless RUBY_PLATFORM =~ /(:?mswin|mingw|bccwin)/
  class LayoutSymlinkedTest < LayoutTest
    layout "symlinked/symlinked_layout"
  end

  class LayoutSymlinkedIsRenderedTest < ActionController::TestCase
    def test_symlinked_layout_is_rendered
      @controller = LayoutSymlinkedTest.new
      get :hello
      assert_response 200
      assert_equal "layouts/symlinked/symlinked_layout", @response.layout
    end
  end
end

