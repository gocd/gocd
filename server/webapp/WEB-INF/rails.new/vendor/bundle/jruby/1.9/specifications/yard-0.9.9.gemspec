# -*- encoding: utf-8 -*-
# stub: yard 0.9.9 ruby lib

Gem::Specification.new do |s|
  s.name = "yard"
  s.version = "0.9.9"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.metadata = { "yard.run" => "yri" } if s.respond_to? :metadata=
  s.require_paths = ["lib"]
  s.authors = ["Loren Segal"]
  s.date = "2017-04-23"
  s.description = "    YARD is a documentation generation tool for the Ruby programming language.\n    It enables the user to generate consistent, usable documentation that can be\n    exported to a number of formats very easily, and also supports extending for\n    custom Ruby constructs such as custom class level definitions.\n"
  s.email = "lsegal@soen.ca"
  s.executables = ["yard", "yardoc", "yri"]
  s.files = ["bin/yard", "bin/yardoc", "bin/yri"]
  s.homepage = "http://yardoc.org"
  s.licenses = ["MIT"]
  s.post_install_message = "--------------------------------------------------------------------------------\nAs of YARD v0.9.2:\n\nRubyGems \"--document=yri,yard\" hooks are now supported. You can auto-configure\nYARD to automatically build the yri index for installed gems by typing:\n\n    $ yard config --gem-install-yri\n\nSee `yard config --help` for more information on RubyGems install hooks.\n\nYou can also add the following to your .gemspec to have YARD document your gem\non install:\n\n    spec.metadata[\"yard.run\"] = \"yri\" # use \"yard\" to build full HTML docs.\n\n--------------------------------------------------------------------------------\n"
  s.rubygems_version = "2.4.8"
  s.summary = "Documentation tool for consistent and usable documentation in Ruby."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version
end
