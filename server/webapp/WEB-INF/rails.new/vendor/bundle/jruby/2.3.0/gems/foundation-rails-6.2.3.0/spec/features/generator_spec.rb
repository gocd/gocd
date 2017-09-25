require 'spec_helper'

feature 'Foundation install succeeds' do
  scenario 'stylesheets assets files are added' do
    application_css_file = IO.read("#{dummy_app_path}/app/assets/stylesheets/application.css")

    expect(File).to exist("#{dummy_app_path}/app/assets/stylesheets/_settings.scss")
    expect(File).to exist("#{dummy_app_path}/app/assets/stylesheets/foundation_and_overrides.scss")
    expect(application_css_file).to match(/require foundation_and_overrides/)
  end

  scenario 'javascripts assets files are added' do
    application_js_file = IO.read("#{dummy_app_path}/app/assets/javascripts/application.js")

    expect(application_js_file).to match(/require foundation/)
    expect(application_js_file).to match(Regexp.new(Regexp.escape('$(function(){ $(document).foundation(); });')))
  end

  scenario 'layout file loads assets' do
    layout_file = IO.read("#{dummy_app_path}/app/views/layouts/application.html.erb")

    expect(layout_file).to match(/stylesheet_link_tag    "application"/)
    expect(layout_file).to match(/javascript_include_tag "application/)
  end
end
