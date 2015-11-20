#!/bin/sh
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
if [ -z `which jruby` ] ; then
  version=1.6.2
  target=/opt/jruby
  echo "Installing JRuby ${version} in ${target}"
  sudo mkdir -p $(dirname ${target})
  wget http://jruby.org.s3.amazonaws.com/downloads/${version}/jruby-bin-${version}.tar.gz
  tar -xz < jruby-bin-${version}.tar.gz
  sudo mv jruby-${version} ${target}
  rm jruby-bin-${version}.tar.gz
  export PATH=$PATH:${target}
  if [ -e ~/.bash_profile ] ; then
    echo "export PATH=${target}/bin:\$PATH" >> ~/.bash_profile
  elif [ -e ~/.profile ] ; then
    echo "export PATH=${target}/bin:\$PATH" >> ~/.profile
  else
    echo "You need to add ${target}/bin to the PATH"
  fi
fi

if [ `which buildr` ] ; then
  echo "Updating to the latest version of Buildr"
  sudo jruby -S gem update buildr
else
  echo "Installing the latest version of Buildr"
  sudo jruby -S gem install buildr
fi
echo

jruby -S buildr --version
