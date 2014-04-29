# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

module Buildr

  # Signs the packages using gpg and uploads signatures as part of the upload process.
  #
  # Require explicitly using <code>require "buildr/apg"</code>. This will result in all
  # packages being signed. The user must specify the GPG_USER environment key to identify
  # the key to use and may specify GPG_PASS if the key needs a password to access. e.g.
  #
  #  $ GPG_USER=user@example.com GPG_PASSWD=secret buildr clean upload
  #
  module GPG
    class << self

      def sign_task(pkg)
        raise "ENV['GPG_USER'] not specified" unless ENV['GPG_USER']
        asc_filename = pkg.to_s + '.asc'
        return if file(asc_filename).prerequisites.include?(pkg.to_s)
        file(asc_filename => [pkg.to_s]) do
          info "GPG signing #{pkg.to_spec}"

          cmd = []
          cmd << 'gpg'
          cmd << '--local-user'
          cmd << ENV['GPG_USER']
          cmd << '--armor'
          cmd << '--output'
          cmd << pkg.to_s + '.asc'
          if ENV['GPG_PASS']
            cmd << '--passphrase'
            cmd << ENV['GPG_PASS']
          end
          cmd << '--detach-sig'
          cmd << '--batch'
          cmd << '--yes'
          cmd << pkg
          trace(cmd.join(' '))
          `#{cmd.join(' ')}`
          raise "Unable to generate signature for #{pkg}" unless File.exist?(asc_filename)
        end
      end

      def sign_and_upload(project, pkg)
        project.task(:upload).enhance do
          artifact = Buildr.artifact(pkg.to_spec_hash.merge(:type => "#{pkg.type}.asc"))
          artifact.from(sign_task(pkg))
          artifact.invoke
          artifact.upload
        end
      end

      def sign_and_upload_all_packages(project)
        project.packages.each { |pkg| Buildr::GPG.sign_and_upload(project, pkg) }
        project.packages.map { |pkg| pkg.pom }.uniq.each { |pom| Buildr::GPG.sign_and_upload(project, pom) }
      end
    end

    module ProjectExtension
      include Extension

      after_define do |project|
        Buildr::GPG.sign_and_upload_all_packages(project)
      end
    end
  end
end

class Buildr::Project
  include Buildr::GPG::ProjectExtension
end
