# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "highline"
  s.version = "1.6.2"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["James Edward Gray II"]
  s.date = "2011-05-13"
  s.description = "A high-level IO library that provides validation, type conversion, and more for\ncommand-line interfaces. HighLine also includes a complete menu system that can\ncrank out anything from simple list selection to complete shells with just\nminutes of work.\n"
  s.email = "james@grayproductions.net"
  s.extra_rdoc_files = ["README", "INSTALL", "TODO", "CHANGELOG", "LICENSE"]
  s.files = ["README", "INSTALL", "TODO", "CHANGELOG", "LICENSE"]
  s.homepage = "http://highline.rubyforge.org"
  s.rdoc_options = ["--title", "HighLine Documentation", "--main", "README"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "highline"
  s.rubygems_version = "1.8.24"
  s.summary = "HighLine is a high-level command-line IO library."

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
