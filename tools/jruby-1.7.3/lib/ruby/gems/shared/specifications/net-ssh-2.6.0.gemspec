# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "net-ssh"
  s.version = "2.6.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Jamis Buck", "Delano Mandelbaum"]
  s.date = "2012-09-19"
  s.description = "Net::SSH: a pure-Ruby implementation of the SSH2 client protocol. It allows you to write programs that invoke and interact with processes on remote servers, via SSH2."
  s.email = ["net-ssh@solutious.com"]
  s.extra_rdoc_files = ["README.rdoc", "THANKS.rdoc", "CHANGELOG.rdoc"]
  s.files = ["README.rdoc", "THANKS.rdoc", "CHANGELOG.rdoc"]
  s.homepage = "http://github.com/net-ssh/net-ssh"
  s.rdoc_options = ["--line-numbers", "--title", "Net::SSH: a pure-Ruby implementation of the SSH2 client protocol.", "--main", "README.rdoc"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "net-ssh"
  s.rubygems_version = "1.8.24"
  s.summary = "Net::SSH: a pure-Ruby implementation of the SSH2 client protocol."

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<jruby-pageant>, [">= 1.1.1"])
    else
      s.add_dependency(%q<jruby-pageant>, [">= 1.1.1"])
    end
  else
    s.add_dependency(%q<jruby-pageant>, [">= 1.1.1"])
  end
end
