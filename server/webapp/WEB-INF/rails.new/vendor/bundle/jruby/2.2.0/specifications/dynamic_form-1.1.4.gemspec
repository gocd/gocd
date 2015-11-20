# -*- encoding: utf-8 -*-
# stub: dynamic_form 1.1.4 ruby lib

Gem::Specification.new do |s|
  s.name = "dynamic_form"
  s.version = "1.1.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Joel Moss"]
  s.date = "2011-04-08"
  s.description = "DynamicForm holds a few helper methods to help you deal with your Rails3 models. It includes the stripped out methods from Rails 2; error_message_on and error_messages_for. It also brings in the functionality of the custom-err-messages plugin, which provides more flexibility over your model error messages."
  s.email = "joel@developwithstyle.com"
  s.extra_rdoc_files = ["README.md"]
  s.files = ["README.md"]
  s.homepage = "http://codaset.com/joelmoss/dynamic-form"
  s.rdoc_options = ["--charset=UTF-8"]
  s.rubygems_version = "2.4.8"
  s.summary = "DynamicForm holds a few helper methods to help you deal with your Rails3 models"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<mocha>, [">= 0"])
    else
      s.add_dependency(%q<mocha>, [">= 0"])
    end
  else
    s.add_dependency(%q<mocha>, [">= 0"])
  end
end
