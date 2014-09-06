# encoding: utf-8
require 'phantomjs'

begin
  require 'capybara/poltergeist'
rescue => LoadError 
  raise "Poltergeist support requires the poltergeist gem to be available."
end

Phantomjs.path # Force install on require
Capybara.register_driver :poltergeist do |app|
  Capybara::Poltergeist::Driver.new(app, :phantomjs => Phantomjs.path)
end