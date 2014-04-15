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

# Change license for Java files.
find ../.. -path "*com/thoughtworks*.java" | grep -v "../../tools" | while read filename
do
 sed -i -E '1i\
/*************************GO-LICENSE-START*********************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END***********************************/\
' $filename
done

find ../.. -path "*com/tw*.java" | grep -v "../../tools" | while read filename
do
 sed -i -E '1i\
/*************************GO-LICENSE-START*********************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END***********************************/\
' $filename
done

# Change license for ruby files.
find ../.. -name "*.rb" | grep -v "../../tools*" | grep -v "../../server/webapp/WEB-INF/rails/vendor*" | while read filename
do
  sed -i -E '1i\
##########################GO-LICENSE-START################################\
# Copyright 2014 ThoughtWorks, Inc.\
#\
# Licensed under the Apache License, Version 2.0 (the "License");\
# you may not use this file except in compliance with the License.\
# You may obtain a copy of the License at\
#\
#     http://www.apache.org/licenses/LICENSE-2.0\
#\
# Unless required by applicable law or agreed to in writing, software\
# distributed under the License is distributed on an "AS IS" BASIS,\
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
# See the License for the specific language governing permissions and\
# limitations under the License.\
##########################GO-LICENSE-END##################################\
' $filename
done

# Change license for rake files.
find ../../tasks -name "*.rake" | while read filename
do
  sed -i -E '1i\
##########################GO-LICENSE-START################################\
# Copyright 2014 ThoughtWorks, Inc.\
#\
# Licensed under the Apache License, Version 2.0 (the "License");\
# you may not use this file except in compliance with the License.\
# You may obtain a copy of the License at\
#\
#     http://www.apache.org/licenses/LICENSE-2.0\
#\
# Unless required by applicable law or agreed to in writing, software\
# distributed under the License is distributed on an "AS IS" BASIS,\
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
# See the License for the specific language governing permissions and\
# limitations under the License.\
##########################GO-LICENSE-END##################################\
' $filename
done

sed -i -E '1i\
##########################GO-LICENSE-START################################\
# Copyright 2014 ThoughtWorks, Inc.\
#\
# Licensed under the Apache License, Version 2.0 (the "License");\
# you may not use this file except in compliance with the License.\
# You may obtain a copy of the License at\
#\
#     http://www.apache.org/licenses/LICENSE-2.0\
#\
# Unless required by applicable law or agreed to in writing, software\
# distributed under the License is distributed on an "AS IS" BASIS,\
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
# See the License for the specific language governing permissions and\
# limitations under the License.\
##########################GO-LICENSE-END##################################\
' ../../server/prepare_go_server_webapp.rake

# Change license for property files.
find ../.. -name "*.properties" | grep -v "../../server/webapp/WEB-INF/rails/vendor" | grep -v "../../tools" | while read filename
do
  sed -i -E '1i\
##########################GO-LICENSE-START################################\
# Copyright 2014 ThoughtWorks, Inc.\
#\
# Licensed under the Apache License, Version 2.0 (the "License");\
# you may not use this file except in compliance with the License.\
# You may obtain a copy of the License at\
#\
#     http://www.apache.org/licenses/LICENSE-2.0\
#\
# Unless required by applicable law or agreed to in writing, software\
# distributed under the License is distributed on an "AS IS" BASIS,\
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
# See the License for the specific language governing permissions and\
# limitations under the License.\
##########################GO-LICENSE-END##################################\
' $filename
done

# Change license for wrapper files.
find ../.. -name "*.conf" | | grep -v "../../tools" | grep -v "../../server/webapp/WEB-INF/rails/vendor/" | while read filename
do
  sed -i -E '1i\
#*************************GO-LICENSE-START********************************\
# Copyright 2014 ThoughtWorks, Inc.\
#\
# Licensed under the Apache License, Version 2.0 (the "License");\
# you may not use this file except in compliance with the License.\
# You may obtain a copy of the License at\
#\
#     http://www.apache.org/licenses/LICENSE-2.0\
#\
# Unless required by applicable law or agreed to in writing, software\
# distributed under the License is distributed on an "AS IS" BASIS,\
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
# See the License for the specific language governing permissions and\
# limitations under the License.\
#*************************GO-LICENSE-END**********************************\
' $filename
done

# Change license for shell scripts.
find ../.. -name "*.sh" | grep -v "../../tools" | while read filename
do
	head -1 $filename | grep -q "^\#\!\/bin\/bash" $filename
if [ $? -eq 0 ]; then 
  sed -i -E '2i\
#*************************GO-LICENSE-START********************************\
# Copyright 2014 ThoughtWorks, Inc.\
#\
# Licensed under the Apache License, Version 2.0 (the "License");\
# you may not use this file except in compliance with the License.\
# You may obtain a copy of the License at\
#\
#     http://www.apache.org/licenses/LICENSE-2.0\
#\
# Unless required by applicable law or agreed to in writing, software\
# distributed under the License is distributed on an "AS IS" BASIS,\
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
# See the License for the specific language governing permissions and\
# limitations under the License.\
#*************************GO-LICENSE-END**********************************\
' $filename
else 
	sed -i -E '1i\
#*************************GO-LICENSE-START********************************\
# Copyright 2014 ThoughtWorks, Inc.\
#\
# Licensed under the Apache License, Version 2.0 (the "License");\
# you may not use this file except in compliance with the License.\
# You may obtain a copy of the License at\
#\
#     http://www.apache.org/licenses/LICENSE-2.0\
#\
# Unless required by applicable law or a-qgreed to in writing, software\
# distributed under the License is distributed on an "AS IS" BASIS,\
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
# See the License for the specific language governing permissions and\
# limitations under the License.\
#*************************GO-LICENSE-END**********************************\
' $filename
fi	

sed -i -E '2i\
#*************************GO-LICENSE-START********************************\
# Copyright 2014 ThoughtWorks, Inc.\
#\
# Licensed under the Apache License, Version 2.0 (the "License");\
# you may not use this file except in compliance with the License.\
# You may obtain a copy of the License at\
#\
#     http://www.apache.org/licenses/LICENSE-2.0\
#\
# Unless required by applicable law or agreed to in writing, software\
# distributed under the License is distributed on an "AS IS" BASIS,\
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
# See the License for the specific language governing permissions and\
# limitations under the License.\
#*************************GO-LICENSE-END**********************************\
' ../../tfs-tool/tf
done

# Change license for JavaScript files.
cat javascriptFiles.txt | while read filename
do
 sed -i -E '1i\
/*************************GO-LICENSE-START*********************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END**********************************/\
' $filename
done

# Change license for XML files.
find ../.. -name "*.xml" | grep -v "../../localivy/ant/ant-dependencies.xml" | grep -v "../../tools" | grep -v "../../.idea" | grep -v "../../go-command-repo" | grep -v "../../local-maven-repo" | while read filename
do

head -1 $filename | grep -q "^<?xml" $filename
if [ $? -eq 0 ]; then 
sed -i -E '2i\
<!-- *************************GO-LICENSE-START******************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END******************************* -->\
' $filename
else 
sed -i -E '1i\
<!-- *************************GO-LICENSE-START******************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END******************************* -->\
' $filename
fi
done

# Change license for HTML files.
find ../.. -name "*.html" | grep -v "../../tools" | grep -v "../../server/webapp/WEB-INF/rails/vendor" | grep -v "../../server/jsunit/app" | while read filename
do
head -1 $filename | grep -q "^<\!DOCTYPE" $filename
if [ $? -eq 0 ]; then 
sed -i -E '2i\
<!-- *************************GO-LICENSE-START******************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END******************************* -->\
' $filename
else 
sed -i -E '1i\
<!-- *************************GO-LICENSE-START******************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END******************************* -->\
' $filename
fi
done

# Change license for xsd files.
find ../.. -name "*.xsd" | grep -v "../../tools" | grep -v "../../server/webapp/WEB-INF/rails/vendor" | grep -v "../../server/jsunit/app" | while read filename
do
sed -i -E '2i\
<!-- *************************GO-LICENSE-START******************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END******************************* -->\
' $filename
done

# Change license for xsl files.
find ../.. -name "*.xsl" | grep -v "../../tools" | grep -v "../../server/webapp/WEB-INF/rails/vendor" | grep -v "../../server/jsunit/app" | while read filename
do
sed -i -E '2i\
<!-- *************************GO-LICENSE-START*****************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END******************************* -->\
' $filename
done

# Change license for CSS files.
find ../.. -name "*.css" | grep -v "../../tools" | grep -v "../../server/webapp/WEB-INF/rails/vendor"  | grep -v "../../server/jsunit" | grep -v "*/reset-fonts-grids.css" | while read filename
do
 sed -i -E '1i\
/*************************GO-LICENSE-START*********************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END***********************************/\
' $filename
done

# Change license for vm files.
find ../.. -name "*.vm" | while read filename
do
 sed -i -E '1i\
#*************************GO-LICENSE-START*********************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END***********************************#\
' $filename
done

# Change license for sql files.
find ../.. -name "*.sql" | grep -v "../../server/webapp/WEB-INF/rails/vendor" | while read filename
do
 sed -i -E '1i\
--*************************GO-LICENSE-START*********************************\
-- Copyright 2014 ThoughtWorks, Inc.\
--\
-- Licensed under the Apache License, Version 2.0 (the "License");\
-- you may not use this file except in compliance with the License.\
-- You may obtain a copy of the License at\
--\
--    http://www.apache.org/licenses/LICENSE-2.0\
--\
-- Unless required by applicable law or agreed to in writing, software\
-- distributed under the License is distributed on an "AS IS" BASIS,\
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
-- See the License for the specific language governing permissions and\
-- limitations under the License.\
--*************************GO-LICENSE-END***********************************\
' $filename
done

# Change license for vm files.
find ../.. -name "*.scss" |  | while read filename
do
 sed -i -E '1i\
/*************************GO-LICENSE-START*********************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END***********************************/\
' $filename
done

# Change license for scss files.
find ../../server/webapp/sass -name "*.scss"| while read filename
do
 sed -i -E '1i\
/*************************GO-LICENSE-START*********************************\
 * Copyright 2014 ThoughtWorks, Inc.\
 *\
 * Licensed under the Apache License, Version 2.0 (the "License");\
 * you may not use this file except in compliance with the License.\
 * You may obtain a copy of the License at\
 *\
 *    http://www.apache.org/licenses/LICENSE-2.0\
 *\
 * Unless required by applicable law or agreed to in writing, software\
 * distributed under the License is distributed on an "AS IS" BASIS,\
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\
 * See the License for the specific language governing permissions and\
 * limitations under the License.\
 *************************GO-LICENSE-END***********************************/\
' $filename
done

