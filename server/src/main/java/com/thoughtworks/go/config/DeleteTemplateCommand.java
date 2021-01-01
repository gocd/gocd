/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.config;

public class DeleteTemplateCommand implements NoOverwriteUpdateConfigCommand {
    private final String templateName;
    private final String md5;

    public DeleteTemplateCommand(String templateName, String md5) {
        this.templateName = templateName;
        this.md5 = md5;
    }

    @Override
    public CruiseConfig update(CruiseConfig cruiseConfig) {
        cruiseConfig.getTemplates().removeTemplateNamed(new CaseInsensitiveString(templateName));
        return cruiseConfig;
    }

    @Override
    public String unmodifiedMd5() {
        return md5;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeleteTemplateCommand)) {
            return false;
        }

        DeleteTemplateCommand command = (DeleteTemplateCommand) o;

        if (md5 != null ? !md5.equals(command.md5) : command.md5 != null) {
            return false;
        }
        return !(templateName != null ? !templateName.equals(command.templateName) : command.templateName != null);
    }

    @Override
    public int hashCode() {
        int result = templateName != null ? templateName.hashCode() : 0;
        result = 31 * result + (md5 != null ? md5.hashCode() : 0);
        return result;
    }
}
