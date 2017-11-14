class TemplateAssertionsController < ActionController::Base
  def render_nothing
    head :ok
  end

  def render_with_template
    render template: "test/hello_world"
  end

  def render_with_template_repeating_in_path
    render template: "test/hello/hello"
  end

  def render_with_partial
    render partial: 'test/partial'
  end

  def render_file_absolute_path
    render file: File.expand_path('../../../README.rdoc', __FILE__)
  end

  def render_file_relative_path
    render file: 'test/dummy/README.rdoc'
  end

  def render_with_layout
    @variable_for_layout = 'hello'
    render "test/hello_world", layout: "layouts/standard"
  end

  def render_with_layout_and_partial
    render "test/hello_world_with_partial", layout: "layouts/standard"
  end
end
