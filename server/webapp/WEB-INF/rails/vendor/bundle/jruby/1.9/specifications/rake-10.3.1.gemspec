# -*- encoding: utf-8 -*-
# stub: rake 10.3.1 ruby lib

Gem::Specification.new do |s|
  s.name = "rake"
  s.version = "10.3.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.3.2") if s.respond_to? :required_rubygems_version=
  s.authors = ["Eric Hodel", "Jim Weirich"]
  s.cert_chain = ["-----BEGIN CERTIFICATE-----\nMIIDNjCCAh6gAwIBAgIBAjANBgkqhkiG9w0BAQUFADBBMRAwDgYDVQQDDAdkcmJy\nYWluMRgwFgYKCZImiZPyLGQBGRYIc2VnbWVudDcxEzARBgoJkiaJk/IsZAEZFgNu\nZXQwHhcNMTQwMzI0MjEwNTQ1WhcNMTUwMzI0MjEwNTQ1WjBBMRAwDgYDVQQDDAdk\ncmJyYWluMRgwFgYKCZImiZPyLGQBGRYIc2VnbWVudDcxEzARBgoJkiaJk/IsZAEZ\nFgNuZXQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCbbgLrGLGIDE76\nLV/cvxdEzCuYuS3oG9PrSZnuDweySUfdp/so0cDq+j8bqy6OzZSw07gdjwFMSd6J\nU5ddZCVywn5nnAQ+Ui7jMW54CYt5/H6f2US6U0hQOjJR6cpfiymgxGdfyTiVcvTm\nGj/okWrQl0NjYOYBpDi+9PPmaH2RmLJu0dB/NylsDnW5j6yN1BEI8MfJRR+HRKZY\nmUtgzBwF1V4KIZQ8EuL6I/nHVu07i6IkrpAgxpXUfdJQJi0oZAqXurAV3yTxkFwd\ng62YrrW26mDe+pZBzR6bpLE+PmXCzz7UxUq3AE0gPHbiMXie3EFE0oxnsU3lIduh\nsCANiQ8BAgMBAAGjOTA3MAkGA1UdEwQCMAAwCwYDVR0PBAQDAgSwMB0GA1UdDgQW\nBBS5k4Z75VSpdM0AclG2UvzFA/VW5DANBgkqhkiG9w0BAQUFAAOCAQEACQFPYbmk\nP51YZtd2sTMJkwhMs3RtLy+MqFpITMoPUjK1gvPw6NyzotvW0WkiU3KXIOem4l8s\nlDqWPIGobRf+Hvzp92hX/CEDGOSMbVBzuLZ4NAQftpvF91FE8KYCvrO+Nj8ei2X/\n+R7biySvcvWhCsIuBawVW6RxZdxaEHVZrbTX9cqGTAfnWhOIpsGJ/vNOofu1jIrw\ndAwolOAbVCvXy7lPI7oFtVzGu18RT7NW6Q4frd28V0Qs4shmW+ckdlneSzN1hVft\npfkQPF5Ezsi73pEpFN93Fy21NKCYQH1jCwWeKUF29MIMGd6kE3ZmHW/7fz5GwKIM\nLs5SgY48a0l7Hw==\n-----END CERTIFICATE-----\n"]
  s.date = "2014-04-17"
  s.description = "Rake is a Make-like program implemented in Ruby. Tasks and dependencies are\nspecified in standard Ruby syntax.\n\nRake has the following features:\n\n* Rakefiles (rake's version of Makefiles) are completely defined in\n  standard Ruby syntax.  No XML files to edit.  No quirky Makefile\n  syntax to worry about (is that a tab or a space?)\n\n* Users can specify tasks with prerequisites.\n\n* Rake supports rule patterns to synthesize implicit tasks.\n\n* Flexible FileLists that act like arrays but know about manipulating\n  file names and paths.\n\n* A library of prepackaged tasks to make building rakefiles easier. For example,\n  tasks for building tarballs and publishing to FTP or SSH sites.  (Formerly\n  tasks for building RDoc and Gems were included in rake but they're now\n  available in RDoc and RubyGems respectively.)\n\n* Supports parallel execution of tasks."
  s.email = ["drbrain@segment7.net", ""]
  s.executables = ["rake"]
  s.extra_rdoc_files = ["History.rdoc", "Manifest.txt", "README.rdoc", "doc/command_line_usage.rdoc", "doc/glossary.rdoc", "doc/proto_rake.rdoc", "doc/rakefile.rdoc", "doc/rational.rdoc", "doc/release_notes/rake-0.4.14.rdoc", "doc/release_notes/rake-0.4.15.rdoc", "doc/release_notes/rake-0.5.0.rdoc", "doc/release_notes/rake-0.5.3.rdoc", "doc/release_notes/rake-0.5.4.rdoc", "doc/release_notes/rake-0.6.0.rdoc", "doc/release_notes/rake-0.7.0.rdoc", "doc/release_notes/rake-0.7.1.rdoc", "doc/release_notes/rake-0.7.2.rdoc", "doc/release_notes/rake-0.7.3.rdoc", "doc/release_notes/rake-0.8.0.rdoc", "doc/release_notes/rake-0.8.2.rdoc", "doc/release_notes/rake-0.8.3.rdoc", "doc/release_notes/rake-0.8.4.rdoc", "doc/release_notes/rake-0.8.5.rdoc", "doc/release_notes/rake-0.8.6.rdoc", "doc/release_notes/rake-0.8.7.rdoc", "doc/release_notes/rake-0.9.0.rdoc", "doc/release_notes/rake-0.9.1.rdoc", "doc/release_notes/rake-0.9.2.2.rdoc", "doc/release_notes/rake-0.9.2.rdoc", "doc/release_notes/rake-0.9.3.rdoc", "doc/release_notes/rake-0.9.4.rdoc", "doc/release_notes/rake-0.9.5.rdoc", "doc/release_notes/rake-0.9.6.rdoc", "doc/release_notes/rake-10.0.0.rdoc", "doc/release_notes/rake-10.0.1.rdoc", "doc/release_notes/rake-10.0.2.rdoc", "doc/release_notes/rake-10.0.3.rdoc", "doc/release_notes/rake-10.1.0.rdoc", "MIT-LICENSE"]
  s.files = ["bin/rake", "History.rdoc", "Manifest.txt", "README.rdoc", "doc/command_line_usage.rdoc", "doc/glossary.rdoc", "doc/proto_rake.rdoc", "doc/rakefile.rdoc", "doc/rational.rdoc", "doc/release_notes/rake-0.4.14.rdoc", "doc/release_notes/rake-0.4.15.rdoc", "doc/release_notes/rake-0.5.0.rdoc", "doc/release_notes/rake-0.5.3.rdoc", "doc/release_notes/rake-0.5.4.rdoc", "doc/release_notes/rake-0.6.0.rdoc", "doc/release_notes/rake-0.7.0.rdoc", "doc/release_notes/rake-0.7.1.rdoc", "doc/release_notes/rake-0.7.2.rdoc", "doc/release_notes/rake-0.7.3.rdoc", "doc/release_notes/rake-0.8.0.rdoc", "doc/release_notes/rake-0.8.2.rdoc", "doc/release_notes/rake-0.8.3.rdoc", "doc/release_notes/rake-0.8.4.rdoc", "doc/release_notes/rake-0.8.5.rdoc", "doc/release_notes/rake-0.8.6.rdoc", "doc/release_notes/rake-0.8.7.rdoc", "doc/release_notes/rake-0.9.0.rdoc", "doc/release_notes/rake-0.9.1.rdoc", "doc/release_notes/rake-0.9.2.2.rdoc", "doc/release_notes/rake-0.9.2.rdoc", "doc/release_notes/rake-0.9.3.rdoc", "doc/release_notes/rake-0.9.4.rdoc", "doc/release_notes/rake-0.9.5.rdoc", "doc/release_notes/rake-0.9.6.rdoc", "doc/release_notes/rake-10.0.0.rdoc", "doc/release_notes/rake-10.0.1.rdoc", "doc/release_notes/rake-10.0.2.rdoc", "doc/release_notes/rake-10.0.3.rdoc", "doc/release_notes/rake-10.1.0.rdoc", "MIT-LICENSE"]
  s.homepage = "https://github.com/jimweirich/rake"
  s.licenses = ["MIT"]
  s.rdoc_options = ["--main", "README.rdoc"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.8.7")
  s.rubyforge_project = "rake"
  s.rubygems_version = "2.1.9"
  s.summary = "Rake is a Make-like program implemented in Ruby"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<minitest>, ["~> 5.2"])
      s.add_development_dependency(%q<rdoc>, ["~> 4.0"])
      s.add_development_dependency(%q<hoe>, ["~> 3.7"])
    else
      s.add_dependency(%q<minitest>, ["~> 5.2"])
      s.add_dependency(%q<rdoc>, ["~> 4.0"])
      s.add_dependency(%q<hoe>, ["~> 3.7"])
    end
  else
    s.add_dependency(%q<minitest>, ["~> 5.2"])
    s.add_dependency(%q<rdoc>, ["~> 4.0"])
    s.add_dependency(%q<hoe>, ["~> 3.7"])
  end
end
