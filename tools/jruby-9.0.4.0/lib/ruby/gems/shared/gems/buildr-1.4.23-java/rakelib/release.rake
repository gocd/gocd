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

desc 'Checkout or update dist to local directory'
task 'setup-local-dist-svn' do
  if File.exist?('dist')
    sh 'svn', 'up', 'site'
    sh 'svn', 'revert', '--recursive', 'dist'
  else
    sh 'svn', 'co', 'https://dist.apache.org/repos/dist/release/buildr', 'dist'
  end
end

task 'publish-dist-svn' do
  cd 'dist'
  sh 'svn', 'add', '--force', '.'
  cd '..'
  sh 'svn', 'commit', 'dist', '-m', 'Publish latest release'
end

desc 'Release the next version of buildr from existing staged repository'
task 'release' => %w{setup-local-site-svn setup-local-dist-svn} do
  # First, we need to get all the staged files from Apache to _release.
  mkpath '_release'
  lambda do
    url = "people.apache.org:~/public_html/#{spec.name}/#{spec.version}"
    puts "Populating _release directory from #{url} ..."
    sh 'rsync', '--progress', '--recursive', url, '_release'
    puts '[X] Staged files are now in _release'
  end.call

  # Upload binary and source packages and new Web site
  lambda do
    target = "dist/#{spec.version}"
    puts "Copying packages to #{target}"
    FileUtils.rm_rf(target)
    existing_dirs = `ls dist`.split
    FileUtils.mkdir_p(target)
    sh 'rsync', '--progress', '--recursive', '--delete', "_release/#{spec.version}/dist/", target
    sh 'chmod', '-f', '-R', 'g+w', target
    puts "[X] Copying packages to #{target}"

    puts "[X] Removing existing packages #{existing_dirs.join(', ')}"
    existing_dirs.each do |dir|
      sh 'svn', 'rm', '--force', "dist/#{dir}"
    end
    puts "Publishing #{spec.name}"
    task('publish-dist-svn').invoke
    puts "[X] Publishing #{spec.name}"
  end.call

  # Push gems to Rubyforge.org
  lambda do
    files = FileList["_release/#{spec.version}/dist/*.{gem}"]
    files.each do |f|
      puts "Push gem #{f} to RubyForge.org ... "
      sh 'gem', 'push', f do |ok, res|
          if ok
            puts "[X] Pushed gem #{File.basename(f)} to Rubyforge.org"
          else
            puts 'Could not push gem, please do it yourself!'
            puts %{  gem push #{f}}
          end
        end
    end
    puts '[X] Pushed gems to Rubyforge.org'
  end.call

  # Create an tag for this release.
  lambda do
    version = `git describe --tags --always`.strip
    unless version == spec.version
      sh 'git', 'tag', '-m', "'Release #{spec.version}'", spec.version.to_s do |ok, res|
        if ok
          puts "[X] Tagged this release as #{spec.version} ... "
        else
          puts 'Could not create tag, please do it yourself!'
          puts %{  git tag -m "Release #{spec.version}" #{spec.version} }
        end
      end
    end
  end.call

  # Update CHANGELOG to next release number.
  lambda do
    next_version = spec.version.to_s.split('.').map { |v| v.to_i }.
      zip([0, 0, 1]).map { |a| a.inject(0) { |t,i| t + i } }.join('.')
    modified = "#{next_version} (Pending)\n\n" + File.read('CHANGELOG')
    File.open 'CHANGELOG', 'w' do |file|
      file.write modified
    end
    puts '[X] Updated CHANGELOG and added entry for next release'
  end.call

  # Update source files to next release number.
  lambda do
    next_version = spec.version.to_s.split('.').map { |v| v.to_i }.
      zip([0, 0, 1]).map { |a| a.inject(0) { |t,i| t + i } }.join('.')

    ver_file = "lib/#{spec.name}/version.rb"
    if File.exist?(ver_file)
      modified = File.read(ver_file).sub(/(VERSION\s*=\s*)(['"])(.*)\2/) { |line| "#{$1}#{$2}#{next_version}.dev#{$2}" }
      File.open ver_file, 'w' do |file|
        file.write modified
      end
      puts "[X] Updated #{ver_file} to next release"
    end
  end.call

  # Update doap file for current release.
  lambda do
    doap_file = "doap.rdf"
    release_date = File.read("_release/#{spec.version}/CHANGES").scan(/#{spec.version} \((.*)\)/).flatten[0]
    changes = File.read("_release/#{spec.version}/CHANGES")[/.*?\n(.*)/m, 1]
    doap_entry = <<DOAP
    <release>
      <Version>
        <name>#{spec.version}</name>
        <created>#{release_date}</created>
        <revision>#{spec.version}</revision>
        <dc:description>
#{changes}
        </dc:description>
      </Version>
    </release>
DOAP
    modified = File.read(doap_file).sub(/^    \<category.* \/\>$/) { |category_line| "#{category_line}\n#{doap_entry}" }
    File.open doap_file, 'w' do |file|
      file.write modified
    end
    puts "[X] Updated #{doap_file} for current release"
  end.call

  # Prepare release announcement email.
  lambda do
    changes = File.read("_release/#{spec.version}/CHANGES")[/.*?\n(.*)/m, 1]
    email = <<-EMAIL
To: users@buildr.apache.org, announce@apache.org
Subject: [ANNOUNCE] Apache Buildr #{spec.version} released

#{spec.description}

New in this release:

#{changes.gsub(/^/, '  ')}

To learn more about Buildr and get started:
http://buildr.apache.org/

Thanks!
The Apache Buildr Team

    EMAIL
    File.open 'announce-email.txt', 'w' do |file|
      file.write email
    end
    puts '[X] Created release announce email template in ''announce-email.txt'''
    puts email
  end.call
end

task('clobber') { rm_rf 'dist' }
task('clobber') { rm_rf '_release' }
task('clobber') { rm_rf 'announce-email.txt' }
