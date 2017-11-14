require 'test_helper'

class RenderTemplateTest < ActionView::TestCase
  test "supports specifying templates with a Regexp" do
    render(template: "test/hello_world")
    assert_template %r{\Atest/hello_world\Z}
  end

  test "supports specifying partials" do
    controller.controller_path = "test"
    render(template: "test/calling_partial_with_layout")
    assert_template partial: "_partial_for_use_in_layout"
  end

  test "supports specifying locals (passing)" do
    controller.controller_path = "test"
    render(template: "test/calling_partial_with_layout")
    assert_template partial: "_partial_for_use_in_layout", locals: { name: "David" }
  end

  test "supports specifying locals (failing)" do
    controller.controller_path = "test"
    render(template: "test/calling_partial_with_layout")
    e = assert_raise ActiveSupport::TestCase::Assertion do
      assert_template partial: "_partial_for_use_in_layout", locals: { name: "Somebody Else" }
    end
    assert_match(/Somebody Else.*David/m, e.message)
  end

  test 'supports different locals on the same partial' do
    controller.controller_path = "test"
    render(template: "test/render_two_partials")
    assert_template partial: '_partial', locals: { 'first' => '1' }
    assert_template partial: '_partial', locals: { 'second' => '2' }
  end

  test 'raises descriptive error message when template was not rendered' do
    controller.controller_path = "test"
    render(template: "test/hello_world_with_partial")
    e = assert_raise ActiveSupport::TestCase::Assertion do
      assert_template partial: 'i_was_never_rendered', locals: { 'did_not' => 'happen' }
    end
    assert_match "i_was_never_rendered to be rendered but it was not.", e.message
    assert_match 'Expected ["/test/partial"] to include "i_was_never_rendered"', e.message
  end

  test 'specifying locals works when the partial is inside a directory with underline prefix' do
    controller.controller_path = "test"
    render(template: 'test/render_partial_inside_directory')
    assert_template partial: 'test/_directory/_partial_with_locales', locals: { 'name' => 'Jane' }
  end

  test 'specifying locals works when the partial is inside a directory without underline prefix' do
    controller.controller_path = "test"
    render(template: 'test/render_partial_inside_directory')
    assert_template partial: 'test/_directory/partial_with_locales', locals: { 'name' => 'Jane' }
  end
end
