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


function has_group {
    getent group $1 >/dev/null
}

function has_user {
    getent passwd $1 >/dev/null
}

function does_not_have_user {
    ! has_user $1 
}

function does_not_have_group {
    ! has_group $1
}

function warn_about_go_existing_as {
    echo "$APP_NAME installation will now use $1 'go'"
}

function warn_about_failure_to_rename {
    echo "Rename of $1 'cruise' to 'go' failed, please rename manually."
}

function rename_group_cruise_to_go {
    groupmod -n go cruise || warn_about_failure_to_rename group
}

function rename_user_cruise_to_go {
    usermod -l go -c 'Go User' cruise || warn_about_failure_to_rename user
}

function user_go_belongs_to_group_go {
    groups go | grep -q ':.*\<go\>'
}

function add_user_go_to_group_go {
    usermod -G go -a go
}

function warn_about_existing_user_or_group_go {
    (has_group go && warn_about_go_existing_as group) || true
    (has_user go && warn_about_go_existing_as user) || true
}

function rename_cruise_user_and_group_to_go {
    (has_group cruise && does_not_have_group go && rename_group_cruise_to_go) || true
    (has_user cruise && does_not_have_user go && rename_user_cruise_to_go) || true
}

function create_user_go_if_none {
    (does_not_have_user go && create_user_go_with_group) || true
}

function create_group_go_if_none {
    (does_not_have_group go && create_group go) || true
}

function ensure_go_user_belongs_to_go_group {
    user_go_belongs_to_group_go || add_user_go_to_group_go
}
