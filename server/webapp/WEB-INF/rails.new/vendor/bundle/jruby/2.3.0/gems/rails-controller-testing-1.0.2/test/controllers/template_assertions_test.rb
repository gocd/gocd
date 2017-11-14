require 'test_helper'

class TemplateAssertionsControllerTest < ActionController::TestCase
  def test_with_invalid_hash_keys_raises_argument_error
    assert_raise(ArgumentError) do
      assert_template foo: "bar"
    end
  end

  def test_with_partial
    get :render_with_partial
    assert_template partial: '_partial'
  end

  def test_file_with_absolute_path_success
    get :render_file_absolute_path
    assert_template file: File.expand_path('../../dummy/README.rdoc', __FILE__)
  end

  def test_file_with_relative_path_success
    get :render_file_relative_path
    assert_template file: 'README.rdoc'
  end

  def test_with_file_failure
    get :render_file_absolute_path

    assert_raise(ActiveSupport::TestCase::Assertion) do
      assert_template :file => 'test/hello_world'
    end

    get :render_file_absolute_path

    assert_raise(ActiveSupport::TestCase::Assertion) do
      assert_template file: nil
    end
  end

  def test_with_nil_passes_when_no_template_rendered
    get :render_nothing
    assert_template nil
  end

  def test_with_nil_fails_when_template_rendered
    get :render_with_template

    assert_raise(ActiveSupport::TestCase::Assertion) do
      assert_template nil
    end
  end

  def test_with_empty_string_fails_when_template_rendered
    get :render_with_template

    assert_raise(ActiveSupport::TestCase::Assertion) do
      assert_template ""
    end
  end

  def test_with_empty_string_fails_when_no_template_rendered
    get :render_nothing

    assert_raise(ActiveSupport::TestCase::Assertion) do
      assert_template ""
    end
  end

  def test_passes_with_correct_string
    get :render_with_template
    assert_template 'hello_world'
    assert_template 'test/hello_world'
  end

  def test_passes_with_correct_symbol
    get :render_with_template
    assert_template :hello_world
  end

  def test_fails_with_incorrect_string
    get :render_with_template

    assert_raise(ActiveSupport::TestCase::Assertion) do
      assert_template 'hello_planet'
    end
  end

  def test_fails_with_incorrect_string_that_matches
    get :render_with_template

    assert_raise(ActiveSupport::TestCase::Assertion) do
      assert_template 'est/he'
    end
  end

  def test_fails_with_repeated_name_in_path
    get :render_with_template_repeating_in_path

    assert_raise(ActiveSupport::TestCase::Assertion) do
      assert_template 'test/hello'
    end
  end

  def test_fails_with_incorrect_symbol
    get :render_with_template

    assert_raise(ActiveSupport::TestCase::Assertion) do
      assert_template :hello_planet
    end
  end

  def test_fails_with_incorrect_symbol_that_matches
    get :render_with_template

    assert_raise(ActiveSupport::TestCase::Assertion) do
      assert_template :"est/he"
    end
  end

  def test_fails_with_wrong_layout
    get :render_with_layout

    assert_raise(ActiveSupport::TestCase::Assertion) do
      assert_template layout: "application"
    end
  end

  def test_fails_expecting_no_layout
    get :render_with_layout

    assert_raise(ActiveSupport::TestCase::Assertion) do
      assert_template layout: nil
    end
  end

  def test_fails_expecting_not_known_layout
    get :render_with_layout

    assert_raise(ArgumentError) do
      assert_template layout: 1
    end
  end

  def test_passes_with_correct_layout
    get :render_with_layout
    assert_template layout: "layouts/standard"
  end

  def test_passes_with_layout_and_partial
    get :render_with_layout_and_partial
    assert_template layout: "layouts/standard"
    assert_template partial: "test/_partial"
  end

  def test_passed_with_no_layout
    get :render_with_template
    assert_template layout: nil
  end

  def test_passed_with_no_layout_false
    get :render_with_template
    assert_template layout: false
  end

  def test_passes_with_correct_layout_without_layouts_prefix
    get :render_with_layout
    assert_template layout: "standard"
  end

  def test_passes_with_correct_layout_symbol
    get :render_with_layout
    assert_template layout: :standard
  end

  def test_assert_template_reset_between_requests
    get :render_with_template
    assert_template 'test/hello_world'

    get :render_nothing
    assert_template nil

    get :render_with_partial
    assert_template partial: 'test/_partial'

    get :render_nothing
    assert_template partial: nil

    get :render_with_layout
    assert_template layout: 'layouts/standard'

    get :render_nothing
    assert_template layout: nil

    get :render_file_relative_path
    assert_template file: 'README.rdoc'

    get :render_nothing
    assert_template file: nil
  end

  def test_locals_option_to_assert_template_is_not_supported
    get :render_nothing

    warning_buffer = StringIO.new
    $stderr = warning_buffer

    assert_template partial: 'customer_greeting', locals: { greeting: 'Bonjour' }
    assert_includes warning_buffer.string, "the :locals option to #assert_template is only supported in a ActionView::TestCase\n"
  ensure
    $stderr = STDERR
  end
end
