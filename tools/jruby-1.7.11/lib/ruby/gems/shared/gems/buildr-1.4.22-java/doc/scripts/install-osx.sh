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
version=$(ruby --version)
if [ ${version:5:5} \< '1.8.6' ] ; then
  echo "You do not have Ruby 1.8.6 or later, attempting to install a newer version."
  if [ `which port` ] ; then
    echo "Installing Ruby using MacPorts"
    sudo port install ruby rb-rubygems
  elif [ `which fink` ] ; then
    echo "Installing Ruby using Fink"
    sudo fink install ruby ruby18-dev rubygems-rb18
  else
    echo "Can only upgrade to Ruby 1.8.6 using either MacPorts or Fink, and can't find either one"
    exit 1
  fi
  echo
fi

if [ -z $JAVA_HOME ] ; then
  echo "Setting JAVA_HOME"
  export JAVA_HOME=/Library/Java/Home
fi

if [ $(gem --version) \< '1.3.1' ] ; then
  echo "Upgrading to latest version of RubyGems"
  sudo gem update --system
  echo
fi

if [ `which buildr` ] ; then
  echo "Updating to the latest version of Buildr"
  sudo env JAVA_HOME=$JAVA_HOME gem update buildr
else
  echo "Installing the latest version of Buildr"
  sudo env JAVA_HOME=$JAVA_HOME gem install buildr
fi
echo

buildr --version
