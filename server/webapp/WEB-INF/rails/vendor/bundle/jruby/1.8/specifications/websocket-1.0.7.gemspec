# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "websocket"
  s.version = "1.0.7"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Bernard Potocki"]
  s.date = "2013-01-27"
  s.description = "Universal Ruby library to handle WebSocket protocol"
  s.email = ["bernard.potocki@imanel.org"]
  s.homepage = "http://github.com/imanel/websocket-ruby"
  s.require_paths = ["lib"]
  s.rubygems_version = "1.8.24"
  s.summary = "Universal Ruby library to handle WebSocket protocol"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rspec>, ["~> 2.11"])
    else
      s.add_dependency(%q<rspec>, ["~> 2.11"])
    end
  else
    s.add_dependency(%q<rspec>, ["~> 2.11"])
  end
end
