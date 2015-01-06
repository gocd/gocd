#!/bin/bash
#*************************GO-LICENSE-START********************************
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#*************************GO-LICENSE-END**********************************

set -e

# Remove license from ruby files.
find ../.. -name "*.rb" | grep -v "../../tools" | grep -v "../../server/webapp/WEB-INF/rails.new/vendor*"  | while read filename
do
   sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license from rake files.
find ../../tasks -name "*.rake" | while read filename
do
  sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' ../../server/prepare_go_server_webapp.rake

# Remove license from property files.
find ../.. -name "*.properties" | grep -v "../../server/webapp/WEB-INF/rails.new/vendor" | grep -v "../../tools" | while read filename
do
 sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license from wrapper files.
find ../.. -name "*.conf" | grep -v "../../tools" | while read filename
do
  sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license from shell scripts.
find ../.. -name "*.sh"  | grep -v "../../scripts/license/" | grep -v "../../tools" | while read filename
do
  sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license from javascript files.
cat javascript-files.txt | while read filename
do
 sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license from XML files.
find ../.. -name "*.xml" | grep -v "../../localivy/ant/ant-dependencies.xml" |grep -v "../../tools" | grep -v "../../.idea" | grep -v "../../go-command-repo" | grep -v "../../local-maven-repo" | while read filename
do
 sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license from HTML files.
find ../.. -name "*.html" | grep -v "../../tools" | grep -v "../../server/webapp/WEB-INF/rails.new/vendor" | grep -v "../../server/jsunit/app" | while read filename
do
 sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license from xsd files.
find ../.. -name "*.xsd" | grep -v "../../tools" | grep -v "../../server/webapp/WEB-INF/rails.new/vendor" | grep -v "../../server/jsunit/app" |  while read filename
do
 sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license from xsl files.
find ../.. -name "*.xsl" | grep -v "../../tools" | grep -v "../../server/webapp/WEB-INF/rails.new/vendor" | grep -v "../../server/jsunit/app" |  while read filename
do
 sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license for CSS files.
find ../.. -name "*.css" | grep -v "../../tools" | grep -v "../../server/webapp/WEB-INF/rails.new/vendor"  | grep -v "../../server/jsunit" | grep -v "*/reset-fonts-grids.css" | while read filename
do
 sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license from additional files.
cat additional-files.txt | while read filename
do
   sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license from vm files.
find ../.. -name "*.vm" | while read filename
do
   sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license from sql files.
find ../.. -name "*.sql" | grep -v "../../server/webapp/WEB-INF/rails.new/vendor" | while read filename
do
   sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done

# Remove license for scss files.
find ../../server/webapp/WEB-INF/rails.new/app/assets/stylesheets/css_sass -name "*.scss"| while read filename
do
   sed -i -E '/.*GO-LICENSE-START/,/.*GO-LICENSE-END/{N;d}' $filename
done
