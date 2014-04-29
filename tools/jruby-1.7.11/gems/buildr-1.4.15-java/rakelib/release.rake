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

desc "Release the next version of buildr from existing staged repository"
task 'release' => %w{setup-local-site-svn} do
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
    target = "people.apache.org:/www/www.apache.org/dist/#{spec.name}/#{spec.version}"
    puts 'Uploading packages to www.apache.org/dist ...'
    host, remote_dir = target.split(':')
    sh 'ssh', host, 'rm', '-rf', remote_dir rescue nil
    sh 'ssh', host, 'mkdir', remote_dir
    sh 'rsync', '--progress', '--recursive', '--delete', "_release/#{spec.version}/dist/", target
    sh 'ssh', 'people.apache.org', 'chmod', '-f', '-R', 'g+w', "#{remote_dir}/*"
    puts '[X] Uploaded packages to www.apache.org/dist'

    puts "Uploading new site to #{spec.name}.apache.org ..."
    sh 'rsync', '--progress', '--recursive', '--exclude', '.svn', '--delete', "_release/#{spec.version}/site/", 'site'
    task('publish-site-svn').invoke
    puts "[X] Uploaded new site to #{spec.name}.apache.org"
  end.call

  # Upload binary and source packages to RubyForge.
  lambda do
    # update rubyforge projects, processors, etc. in local config
    sh 'rubyforge', 'config'
    files = FileList["_release/#{spec.version}/dist/*.{gem,tgz,zip}"]
    puts "Uploading #{spec.version} to RubyForge ... "
    rubyforge = RubyForge.new.configure
    rubyforge.login
    rubyforge.userconfig.merge!('release_changes'=>"_release/#{spec.version}/CHANGES",  'preformatted' => true)
    rubyforge.add_release spec.rubyforge_project.downcase, spec.name.downcase, spec.version.to_s, *files

    puts "Posting news to RubyForge ... "
    changes = File.read("_release/#{spec.version}/CHANGES")[/.*?\n(.*)/m, 1]
    rubyforge.post_news spec.rubyforge_project.downcase, "Buildr #{spec.version} released",
      "#{spec.description}\n\nNew in Buildr #{spec.version}:\n#{changes.gsub(/^/, '  ')}\n"
    puts "[X] Uploaded gems and source files to #{spec.name}.rubyforge.org"
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

  # Create an SVN tag for this release.
  lambda do
    info = `svn info` + `git svn info` # Using either svn or git-svn
    if url = info[/^URL:/] && info.scan(/^URL: (.*)/)[0][0]
      new_url = url.sub(/(trunk$)|(branches\/\w*)$/, "tags/#{spec.version}")
      unless url == new_url
        sh 'svn', 'copy', url, new_url, '-m', "Release #{spec.version}" do |ok, res|
          if ok
            puts "[X] Tagged this release as tags/#{spec.version} ... "
          else
            puts 'Could not create tag, please do it yourself!'
            puts %{  svn copy #{url} #{new_url} -m "Release #{spec.version}"}
          end
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

task('clobber') { rm_rf '_release' }
task('clobber') { rm_rf 'announce-email.txt' }
