# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "net-sftp"
  s.version = "2.0.5"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.2") if s.respond_to? :required_rubygems_version=
  s.authors = ["Jamis Buck"]
  s.date = "2010-08-19"
  s.description = "A pure Ruby implementation of the SFTP client protocol"
  s.email = "netsftp@solutious.com"
  s.extra_rdoc_files = ["CHANGELOG.rdoc", "lib/net/sftp/constants.rb", "lib/net/sftp/errors.rb", "lib/net/sftp/operations/dir.rb", "lib/net/sftp/operations/download.rb", "lib/net/sftp/operations/file.rb", "lib/net/sftp/operations/file_factory.rb", "lib/net/sftp/operations/upload.rb", "lib/net/sftp/packet.rb", "lib/net/sftp/protocol/01/attributes.rb", "lib/net/sftp/protocol/01/base.rb", "lib/net/sftp/protocol/01/name.rb", "lib/net/sftp/protocol/02/base.rb", "lib/net/sftp/protocol/03/base.rb", "lib/net/sftp/protocol/04/attributes.rb", "lib/net/sftp/protocol/04/base.rb", "lib/net/sftp/protocol/04/name.rb", "lib/net/sftp/protocol/05/base.rb", "lib/net/sftp/protocol/06/attributes.rb", "lib/net/sftp/protocol/06/base.rb", "lib/net/sftp/protocol/base.rb", "lib/net/sftp/protocol.rb", "lib/net/sftp/request.rb", "lib/net/sftp/response.rb", "lib/net/sftp/session.rb", "lib/net/sftp/version.rb", "lib/net/sftp.rb", "README.rdoc"]
  s.files = ["CHANGELOG.rdoc", "lib/net/sftp/constants.rb", "lib/net/sftp/errors.rb", "lib/net/sftp/operations/dir.rb", "lib/net/sftp/operations/download.rb", "lib/net/sftp/operations/file.rb", "lib/net/sftp/operations/file_factory.rb", "lib/net/sftp/operations/upload.rb", "lib/net/sftp/packet.rb", "lib/net/sftp/protocol/01/attributes.rb", "lib/net/sftp/protocol/01/base.rb", "lib/net/sftp/protocol/01/name.rb", "lib/net/sftp/protocol/02/base.rb", "lib/net/sftp/protocol/03/base.rb", "lib/net/sftp/protocol/04/attributes.rb", "lib/net/sftp/protocol/04/base.rb", "lib/net/sftp/protocol/04/name.rb", "lib/net/sftp/protocol/05/base.rb", "lib/net/sftp/protocol/06/attributes.rb", "lib/net/sftp/protocol/06/base.rb", "lib/net/sftp/protocol/base.rb", "lib/net/sftp/protocol.rb", "lib/net/sftp/request.rb", "lib/net/sftp/response.rb", "lib/net/sftp/session.rb", "lib/net/sftp/version.rb", "lib/net/sftp.rb", "README.rdoc"]
  s.homepage = "http://net-ssh.rubyforge.org/sftp"
  s.rdoc_options = ["--line-numbers", "--inline-source", "--title", "Net-sftp", "--main", "README.rdoc"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "net-ssh"
  s.rubygems_version = "1.8.24"
  s.summary = "A pure Ruby implementation of the SFTP client protocol"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<net-ssh>, [">= 2.0.9"])
    else
      s.add_dependency(%q<net-ssh>, [">= 2.0.9"])
    end
  else
    s.add_dependency(%q<net-ssh>, [">= 2.0.9"])
  end
end
