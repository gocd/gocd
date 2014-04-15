# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.


require 'digest/md5'
require 'digest/sha1'

begin # Releases upload Gems to RubyForge.
  require 'rubyforge'
rescue LoadError
  task(:setup) { install_gem 'rubyforge' }
end


task :prepare do |task, args|
  # Make sure we're doing a release from checked code.
  lambda do
    puts "Checking there are no local changes ... "
    svn = `svn status`
    fail "Cannot release unless all local changes are in SVN:\n#{svn}" unless svn.empty?
    git = `git status`
    fail "Cannot release unless all local changes are in Git:\n#{git}" if git[/^#\t/]
    puts "[X] There are no local changes, everything is in source control"
  end.call

  # Make sure we have a valid CHANGELOG entry for this release.
  lambda do
    puts "Checking that CHANGELOG indicates most recent version and today's date ... "
    expecting = "#{spec.version} (#{Time.now.strftime('%Y-%m-%d')})"
    header = File.readlines('CHANGELOG').first.chomp
    fail "Expecting CHANGELOG to start with #{expecting}, but found #{header} instead" unless expecting == header
    puts "[x] CHANGELOG indicates most recent version and today's date"
  end.call

  # Need GPG to sign the packages.
  lambda do
    args.gpg or fail "Please run with gpg=<argument for gpg --local-user>"
    fail "No GPG user #{args.gpg}" if `gpg2 --list-keys #{args.gpg}`.empty?
  end.call

  task(:license).invoke
  task(:dependency).invoke

  # Need JRuby, Scala and Groovy installed to run all the specs.
  lambda do
    puts "Checking that we have JRuby, Scala and Groovy available ... "
    sh 'jruby --version'
    sh 'scala -version'
    sh 'groovy -version'
    puts "[X] We have JRuby, Scala and Groovy"
  end.call

  # Need RubyForge to upload new release files.
  lambda do
    puts "[!] Make sure you have admin privileges to make a release on RubyForge"
    rubyforge = RubyForge.new.configure
    rubyforge.login 
    rubyforge.scrape_project(spec.name)
  end.call

  # We will be speccing in one platform, so also spec the other one.
  task(RUBY_PLATFORM =~ /java/ ? 'spec:ruby' : 'spec:jruby').invoke # Test the *other* platform
end


task :stage=>['setup', 'doc:setup', :clobber, :prepare] do |task, args|
  mkpath '_staged'

  # Start by figuring out what has changed.
  lambda do
    puts "Looking for changes between this release and previous one ..."
    pattern = /(^(\d+\.\d+(?:\.\d+)?)\s+\(\d{4}-\d{2}-\d{2}\)\s*((:?^[^\n]+\n)*))/
    changes = File.read('CHANGELOG').scan(pattern).inject({}) { |hash, set| hash[set[1]] = set[2] ; hash }
    current = changes[spec.version.to_s]
    fail "No changeset found for version #{spec.version}" unless current
    File.open '_staged/CHANGES', 'w' do |file|
      file.write "#{spec.version} (#{Time.now.strftime('%Y-%m-%d')})\n"
      file.write current
    end
    puts "[X] Listed most recent changed in _staged/CHANGES"
  end.call

  # Create the packages (gem, tarball) and sign them. This requires user
  # intervention so the earlier we do it the better.
  lambda do
    puts "Creating and signing release packages ..."
    task(:package).invoke
    mkpath '_staged/dist'
    FileList['pkg/*.{gem,zip,tgz}'].each do |source|
      pkg = source.pathmap('_staged/dist/%n%x') 
      cp source, pkg
      bytes = File.open(pkg, 'rb') { |file| file.read }
      File.open(pkg + '.md5', 'w') { |file| file.write Digest::MD5.hexdigest(bytes) << ' ' << File.basename(pkg) }
      File.open(pkg + '.sha1', 'w') { |file| file.write Digest::SHA1.hexdigest(bytes) << ' ' << File.basename(pkg) }
      sh 'gpg2', '--local-user', args.gpg, '--armor', '--output', pkg + '.asc', '--detach-sig', pkg, :verbose=>true
    end
    cp 'etc/KEYS', '_staged'
    puts "[X] Created and signed release packages in _staged/dist"
  end.call

  # The download page should link to the new binaries/sources, and we
  # want to do that before generating the site/documentation.
  lambda do
    puts "Updating download page with links to release packages ... "
    url = "http://www.apache.org/dist/#{spec.name}/#{spec.version}"
    rows = FileList['_staged/dist/*.{gem,tgz,zip}'].map { |pkg|
      name, md5 = File.basename(pkg), Digest::MD5.file(pkg).to_s
      %{| "#{name}":#{url}/#{name} | "#{md5}":#{url}/#{name}.md5 | "Sig":#{url}/#{name}.asc |}
    }
    textile = <<-TEXTILE
h3. #{spec.name} #{spec.version} (#{Time.now.strftime('%Y-%m-%d')})

|_. Package |_. MD5 Checksum |_. PGP |
#{rows.join("\n")}

p>. ("Release signing keys":#{url}/KEYS)
    TEXTILE
    file_name = 'doc/download.textile'
    print "Adding download links to #{file_name} ... "
    modified = File.read(file_name).sub(/^h2\(#dist\).*$/) { |header| "#{header}\n\n#{textile}" }
    File.open file_name, 'w' do |file|
      file.write modified
    end
    puts "[X] Updated #{file_name}"
  end.call


  # Now we can create the Web site, this includes running specs, coverage report, etc.
  # This will take a while, so we want to do it as last step before upload.
  lambda do
    puts "Creating new Web site"
    task(:site).invoke
    cp_r '_site', '_staged/site'
    puts "[X] Created new Web site in _staged/site"
  end.call


  # Move everything over to people.apache.org so we can vote on it.
  lambda do
    url = "people.apache.org:~/public_html/#{spec.name}/#{spec.version}"
    puts "Uploading _staged directory to #{url} ..."
    sh 'rsync', '--progress', '--recursive', '_staged/', url
    puts "[X] Uploaded _staged directory to #{url}"
  end.call
  

  # Prepare a release vote email. In the distant future this will also send the
  # email for you and vote on it.
  lambda do
    # Need to know who you are on Apache, local user may be different (see .ssh/config).
    whoami = `ssh people.apache.org whoami`.strip
    base_url = "http://people.apache.org/~#{whoami}/buildr/#{spec.version}"
    # Need changes for this release only.
    changelog = File.read('CHANGELOG').scan(/(^(\d+\.\d+(?:\.\d+)?)\s+\(\d{4}-\d{2}-\d{2}\)\s*((:?^[^\n]+\n)*))/)
    changes = changelog[0][2]
    previous_version = changelog[1][1]

    email = <<-EMAIL
To: dev@buildr.apache.org
Subject: [VOTE] Buildr #{spec.version} release

We're voting on the source distributions available here:
#{base_url}/dist/

Specifically:
#{base_url}/dist/buildr-#{spec.version}.tgz
#{base_url}/dist/buildr-#{spec.version}.zip

The documentation generated for this release is available here:
#{base_url}/site/
#{base_url}/site/buildr.pdf

The official specification against which this release was tested:
#{base_url}/site/specs.html

Test coverage report:
#{base_url}/site/coverage/index.html


The following changes were made since #{previous_version}:

#{changes.gsub(/^/, '  ')}
    EMAIL
    File.open 'vote-email.txt', 'w' do |file|
      file.write email
    end
    puts "[X] Created release vote email template in 'vote-email.txt'"
    puts email
  end.call

end


task(:clobber) { rm_rf '_staged' }
