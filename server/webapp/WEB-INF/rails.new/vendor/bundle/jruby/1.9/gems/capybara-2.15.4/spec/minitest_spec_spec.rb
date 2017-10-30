# frozen_string_literal: true
require 'spec_helper'
require 'capybara/minitest'
require 'capybara/minitest/spec'

class MinitestSpecTest < Minitest::Spec
  include ::Capybara::DSL
  include ::Capybara::Minitest::Assertions

  before do
    visit('/form')
  end
  after do
    Capybara.reset_sessions!
  end

  it "supports text expectations" do
    page.must_have_text('Form', minimum: 1)
    page.wont_have_text('Not a form')
    form = find(:css, 'form', text: 'Title')
    form.must_have_text('Customer Email')
    form.wont_have_text('Some other email')
  end

  it "supports current_path expectations" do
    page.must_have_current_path('/form')
    page.wont_have_current_path('/form2')
  end

  it "supports title expectations" do
    visit('/with_title')
    page.must_have_title('Test Title')
    page.wont_have_title('Not the title')
  end

  it "supports xpath expectations" do
    page.must_have_xpath('.//input[@id="customer_email"]')
    page.wont_have_xpath('.//select[@id="not_form_title"]')
    page.wont_have_xpath('.//input[@id="customer_email"]') { |el| el[:id] == "not_customer_email" }
    select = find(:select, 'form_title')
    select.must_have_xpath('.//option[@class="title"]')
    select.must_have_xpath('.//option', count: 1) { |option| option[:class] != 'title' && !option.disabled?}
    select.wont_have_xpath('.//input[@id="customer_email"]')
  end

  it "support css expectations" do
    page.must_have_css('input#customer_email')
    page.wont_have_css('select#not_form_title')
    el = find(:select, 'form_title')
    el.must_have_css('option.title')
    el.wont_have_css('input#customer_email')
  end

  it "supports link expectations" do
    visit('/with_html')
    page.must_have_link('A link')
    page.wont_have_link('Not on page')
  end

  it "supports button expectations" do
    page.must_have_button('fresh_btn')
    page.wont_have_button('not_btn')
  end

  it "supports field expectations" do
    page.must_have_field('customer_email')
    page.wont_have_field('not_on_the_form')
  end

  it "supports select expectations" do
    page.must_have_select('form_title')
    page.wont_have_select('not_form_title')
  end

  it "supports checked_field expectations" do
    page.must_have_checked_field('form_pets_dog')
    page.wont_have_checked_field('form_pets_cat')
  end

  it "supports unchecked_field expectations" do
    page.must_have_unchecked_field('form_pets_cat')
    page.wont_have_unchecked_field('form_pets_dog')
  end

  it "supports table expectations" do
    visit('/tables')
    page.must_have_table('agent_table')
    page.wont_have_table('not_on_form')
  end

  it "supports match_selector expectations" do
    find(:field, 'customer_email').must_match_selector(:field, 'customer_email')
    find(:select, 'form_title').wont_match_selector(:field, 'customer_email')
  end

  it "supports match_css expectations" do
    find(:select, 'form_title').must_match_css('select#form_title')
    find(:select, 'form_title').wont_match_css('select#form_other_title')
  end

  it "supports match_xpath expectations" do
    find(:select, 'form_title').must_match_xpath('.//select[@id="form_title"]')
    find(:select, 'form_title').wont_match_xpath('.//select[@id="not_on_page"]')
  end

  it "handles failures" do
    page.must_have_select('non_existing_form_title')
  end
end

RSpec.describe 'capybara/minitest/spec' do
  before do
    Capybara.current_driver = :rack_test
    Capybara.app = TestApp
  end

  it "should support minitest spec" do
    output = StringIO.new
    reporter = Minitest::SummaryReporter.new(output)
    reporter.start
    MinitestSpecTest.run reporter, {}
    reporter.report
    expect(output.string).to include("16 runs, 39 assertions, 1 failures, 0 errors, 0 skips")
    #Make sure error messages are displayed
    expect(output.string).to include('expected to find visible select box "non_existing_form_title" that is not disabled but there were no matches')
  end
end
