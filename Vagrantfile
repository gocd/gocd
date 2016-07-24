# -*- mode: ruby -*-
# vi: set ft=ruby :

# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version (we support older styles for
# backwards compatibility). Please don't change it unless you know what
# you're doing.
Vagrant.configure(2) do |config|
  config.vm.synced_folder "~/.m2", "/home/vagrant/.m2"
  config.vm.synced_folder "~/.gradle", "/home/vagrant/.gradle"

  packages = []

  packages << 'java-1.7.0-openjdk-devel'
  packages << %w(zip unzip gzip bzip2) # compression tools
  packages << %w(git)
  packages << %w(createrepo yum-utils rpm-build yum-utils) #for building rpm packages
  packages << %w(dpkg-devel dpkg-dev) #for building debian packages
  packages << %w(mingw32-nsis) #for building windows exe packages
  packages << %w(wget curl) #for downloading stuff
  packages << %w(nodejs010-nodejs nodejs010-npm) #nodejs for compiling rails assets
  packages << %w(rh-ruby22 rh-ruby22-ruby-devel rh-ruby22-rubygem-bundler rh-ruby22-ruby-irb rh-ruby22-rubygem-rake rh-ruby22-rubygem-psych libffi-devel) #ruby needed for fpm

  config.vm.provision :shell, inline: <<-SHELL
  set -ex
  echo '
[nsis]
name=nsis
baseurl=https://gocd.github.io/nsis-rpm/
gpgcheck=0
' > /etc/yum.repos.d/nsis.repo

  yum install -y -q epel-release centos-release-scl
  yum install -y -q #{packages.flatten.join(' ')}

  echo 'source /opt/rh/rh-ruby22/enable' > /etc/profile.d/ruby-22.sh
  echo 'export PATH=/opt/rh/rh-ruby22/root/usr/local/bin:$PATH' >> /etc/profile.d/ruby-22.sh
  echo 'source /opt/rh/nodejs010/enable' > /etc/profile.d/node-0.10.sh

  (source /opt/rh/rh-ruby22/enable; gem install fpm --no-ri --no-rdoc)
  SHELL

  config.vm.provider :virtualbox do |vb, override|
    override.vm.box = "boxcutter/centos67"

    vb.gui          = ENV['GUI'] || false
    vb.memory       = ((ENV['MEMORY'] || 4).to_f * 1024).to_i
    vb.cpus         = 4
  end

  if Vagrant.has_plugin?('vagrant-cachier')
    config.cache.scope = :box
    config.cache.enable :apt
    config.cache.enable :apt_lists
    config.cache.enable :yum
  end

  if Vagrant.has_plugin?('vagrant-vbguest')
    config.vbguest.auto_update = false
  end

end
