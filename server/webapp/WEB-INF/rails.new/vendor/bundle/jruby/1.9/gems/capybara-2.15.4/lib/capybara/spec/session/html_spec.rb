# frozen_string_literal: true
Capybara::SpecHelper.spec '#html' do
  it "should return the unmodified page body" do
    @session.visit('/')
    expect(@session.html).to include('Hello world!')
  end

  it "should return the current state of the page", requires: [:js] do
    @session.visit('/with_js')
    expect(@session.html).to include('I changed it')
    expect(@session.html).not_to include('This is text')
  end
end

Capybara::SpecHelper.spec '#source' do
  it "should return the unmodified page source" do
    @session.visit('/')
    expect(@session.source).to include('Hello world!')
  end

  it "should return the current state of the page", requires: [:js] do
    @session.visit('/with_js')
    expect(@session.source).to include('I changed it')
    expect(@session.source).not_to include('This is text')
  end
end

Capybara::SpecHelper.spec '#body' do
  it "should return the unmodified page source" do
    @session.visit('/')
    expect(@session.body).to include('Hello world!')
  end

  it "should return the current state of the page", requires: [:js] do
    @session.visit('/with_js')
    expect(@session.body).to include('I changed it')
    expect(@session.body).not_to include('This is text')
  end
end
