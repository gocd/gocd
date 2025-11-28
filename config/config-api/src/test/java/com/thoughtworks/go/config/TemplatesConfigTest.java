/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.helper.PipelineTemplateConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplatesConfigTest {

    @Test
    public void shouldRemoveATemplateByName() {
        PipelineTemplateConfig template2 = template("template2");
        TemplatesConfig templates = new TemplatesConfig(template("template1"), template2);

        templates.removeTemplateNamed(new CaseInsensitiveString("template1"));

        assertThat(templates.size()).isEqualTo(1);
        assertThat(templates.get(0)).isEqualTo(template2);
    }

    @Test
    public void shouldIgnoreTryingToRemoveNonExistentTemplate() {
        TemplatesConfig templates = new TemplatesConfig(template("template1"), template("template2"));

        templates.removeTemplateNamed(new CaseInsensitiveString("sachin"));

        assertThat(templates.size()).isEqualTo(2);
    }

    @Test
    public void shouldReturnTemplateByName() {
        PipelineTemplateConfig template1 = template("template1");
        TemplatesConfig templates = new TemplatesConfig(template1, template("template2"));
        assertThat(templates.templateByName(new CaseInsensitiveString("template1"))).isEqualTo(template1);
    }

    @Test
    public void shouldReturnNullIfTemplateIsNotFound() {
        PipelineTemplateConfig template1 = template("template1");
        TemplatesConfig templates = new TemplatesConfig(template1);
        assertThat(templates.templateByName(new CaseInsensitiveString("some_invalid_template"))).isNull();
    }

    @Test
    public void shouldErrorOutIfTemplateNameIsAlreadyPresent() {
        PipelineTemplateConfig template = template("template1");
        TemplatesConfig templates = new TemplatesConfig(template);
        PipelineTemplateConfig duplicateTemplate = template("template1");
        templates.add(duplicateTemplate);

        templates.validate(null);

        assertThat(template.errors().isEmpty()).isFalse();
        assertThat(duplicateTemplate.errors().isEmpty()).isFalse();
        assertThat(template.errors().firstErrorOn(PipelineTemplateConfig.NAME)).isEqualTo(String.format("Template name '%s' is not unique", template.name()));
        assertThat(duplicateTemplate.errors().firstErrorOn(PipelineTemplateConfig.NAME)).isEqualTo(String.format("Template name '%s' is not unique", template.name()));
    }

    @Test
    public void shouldErrorOutIfTemplateNameIsAlreadyPresent_CaseInsensitiveMap() {
        PipelineTemplateConfig template = template("TEmplatE1");
        TemplatesConfig templates = new TemplatesConfig(template);
        PipelineTemplateConfig duplicateTemplate = template("template1");
        templates.add(duplicateTemplate);

        templates.validate(null);

        assertThat(template.errors().isEmpty()).isFalse();
        assertThat(duplicateTemplate.errors().isEmpty()).isFalse();
    }

    @Test
    public void shouldReturnTrueIfUserCanViewAndEditAtLeastOneTemplate() {
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("template-admin");
        TemplatesConfig templates = configForUserWhoCanViewATemplate();
        templates.add(PipelineTemplateConfigMother.createTemplate("template200", new Authorization(new AdminsConfig(new AdminUser(templateAdmin))), StageConfigMother.manualStage("stage-name")));

        assertThat(templates.canViewAndEditTemplate(templateAdmin, null)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfUserCannotViewAndEditAtLeastOneTemplate() {
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("template-admin");
        CaseInsensitiveString nonTemplateAdmin = new CaseInsensitiveString("some-random-user");
        TemplatesConfig templates = configForUserWhoCanViewATemplate();
        templates.add(PipelineTemplateConfigMother.createTemplate("template200", new Authorization(new AdminsConfig(new AdminUser(templateAdmin))), StageConfigMother.manualStage("stage-name")));

        assertThat(templates.canViewAndEditTemplate(nonTemplateAdmin, null)).isFalse();
    }

    @Test
    public void shouldReturnTrueIfUserCanViewAtLeastOneTemplate() {
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("template-view");
        TemplatesConfig templates = configForUserWhoCanViewATemplate();
        templates.add(PipelineTemplateConfigMother.createTemplate("template200", new Authorization(new ViewConfig(new AdminUser(templateViewUser))), StageConfigMother.manualStage("stage-name")));

        assertThat(templates.canUserViewTemplates(templateViewUser, null, false)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfUserCannotViewAtLeastOneTemplate() {
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("template-view");
        TemplatesConfig templates = configForUserWhoCanViewATemplate();

        assertThat(templates.canUserViewTemplates(templateViewUser, null, false)).isFalse();
    }

    @Test
    public void shouldReturnTrueIfUserWithinARoleCanViewAndEditTemplates() {
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("template-admin");
        Role securityConfigRole = getSecurityConfigRole(templateAdmin);
        List<Role> roles = setupRoles(securityConfigRole);

        List<PipelineTemplateConfig> templateList = new ArrayList<>();
        templateList.add(PipelineTemplateConfigMother.createTemplate("templateName", new Authorization(new AdminsConfig(new AdminRole(securityConfigRole))), StageConfigMother.manualStage("some-random-stage")));
        TemplatesConfig templates = new TemplatesConfig(templateList.toArray(new PipelineTemplateConfig[0]));

        assertThat(templates.canViewAndEditTemplate(templateAdmin, roles)).isTrue();
    }


    @Test
    public void shouldReturnFalseIfUserWithinARoleCannotViewAndEditTemplates() {
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("template-admin");
        Role securityConfigRole = getSecurityConfigRole(templateAdmin);
        List<Role> roles = setupRoles(securityConfigRole);

        List<PipelineTemplateConfig> templateList = new ArrayList<>();
        templateList.add(PipelineTemplateConfigMother.createTemplate("templateName", new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("random-user")))), StageConfigMother.manualStage("stage-name")));
        TemplatesConfig templates = new TemplatesConfig(templateList.toArray(new PipelineTemplateConfig[0]));

        assertThat(templates.canViewAndEditTemplate(templateAdmin, roles)).isFalse();
    }

    @Test
    public void shouldReturnTrueIfUserCanEditTemplate() {
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("template-admin");
        String templateName = "template1";
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate(templateName, new Authorization(new AdminsConfig(new AdminUser(templateAdmin))),
                StageConfigMother.manualStage("stage-name"));
        TemplatesConfig templates = new TemplatesConfig(template);
        assertThat(templates.canUserEditTemplate(template, templateAdmin, null)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfUserCannotEditTemplate() {
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("template-admin");
        CaseInsensitiveString templateAdminWhoDoesNotHavePermissionToThisTemplate = new CaseInsensitiveString("user");
        String templateName = "template1";
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate(templateName, new Authorization(new AdminsConfig(new AdminUser(templateAdmin))),
                StageConfigMother.manualStage("stage-name"));
        TemplatesConfig templates = new TemplatesConfig(template);
        assertThat(templates.canUserEditTemplate(template, templateAdminWhoDoesNotHavePermissionToThisTemplate, null)).isFalse();
    }

    @Test
    public void shouldReturnTrueIfUserWithinARoleCanEditTemplate() {
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("template-admin");
        Role securityConfigRole = getSecurityConfigRole(templateAdmin);
        List<Role> roles = setupRoles(securityConfigRole);

        String templateName = "template1";
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate(templateName, new Authorization(new AdminsConfig(new AdminRole(securityConfigRole))),
                StageConfigMother.manualStage("random-stage-name"));
        TemplatesConfig templates = new TemplatesConfig(template);

        assertThat(templates.canUserEditTemplate(template, templateAdmin, roles)).isTrue();
    }


    @Test
    public void shouldReturnFalseIfUserWithinARoleCannotEditTemplate() {
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("template-admin");
        Role securityConfigRole = getSecurityConfigRole(templateAdmin);
        List<Role> roles = setupRoles(securityConfigRole);

        String templateName = "template1";
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate(templateName, new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("another-role")))),
                StageConfigMother.manualStage("random-stage"));
        TemplatesConfig templates = new TemplatesConfig(template);

        assertThat(templates.canUserEditTemplate(template, templateAdmin, roles)).isFalse();
    }

    @Test
    public void shouldReturnTrueIfUserCanViewTemplate() {
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("view");
        String templateName = "template";
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate(templateName, StageConfigMother.manualStage("stage"));
        template.setAuthorization(new Authorization(new ViewConfig(new AdminUser(templateViewUser))));
        TemplatesConfig templates = new TemplatesConfig(template);

        assertThat(templates.hasViewAccessToTemplate(template, templateViewUser, null, false)).isTrue();
    }

    @Test
    public void shouldReturnTrueIfGroupAdminCanViewTemplate() {
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("view");
        String templateName = "template";
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate(templateName, StageConfigMother.manualStage("stage"));
        TemplatesConfig templates = new TemplatesConfig(template);

        assertThat(templates.hasViewAccessToTemplate(template, templateViewUser, null, true)).isTrue();
    }

    @Test
    public void shouldReturnTrueIfUserWithinARoleCanViewTemplate() {
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("template-admin");
        Role securityConfigRole = getSecurityConfigRole(templateViewUser);
        List<Role> roles = setupRoles(securityConfigRole);
        String templateName = "template1";

        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate(templateName, StageConfigMother.manualStage("stage"));
        template.setAuthorization(new Authorization(new ViewConfig(new AdminRole(securityConfigRole))));
        TemplatesConfig templates = new TemplatesConfig(template);

        assertThat(templates.hasViewAccessToTemplate(template, templateViewUser, roles, false)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfUserCannotViewTemplate() {
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("view");
        String templateName = "template";
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate(templateName, StageConfigMother.manualStage("stage"));
        TemplatesConfig templates = new TemplatesConfig(template);

        assertThat(templates.hasViewAccessToTemplate(template, templateViewUser, null, false)).isFalse();
    }

    @Test
    public void shouldReturnFalseIfGroupAdminCanViewTemplate() {
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("view");
        String templateName = "template";
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate(templateName, StageConfigMother.manualStage("stage"));
        template.getAuthorization().setAllowGroupAdmins(false);
        TemplatesConfig templates = new TemplatesConfig(template);

        assertThat(templates.hasViewAccessToTemplate(template, templateViewUser, null, true)).isFalse();
    }

    @Test
    public void shouldReturnFalseIfUserWithinARoleCannotViewTemplate() {
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("template-admin");
        Role securityConfigRole = getSecurityConfigRole(templateViewUser);
        List<Role> roles = setupRoles(securityConfigRole);
        String templateName = "template1";

        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate(templateName, StageConfigMother.manualStage("stage"));
        template.setAuthorization(new Authorization(new ViewConfig(new AdminRole(new CaseInsensitiveString("another-role")))));
        TemplatesConfig templates = new TemplatesConfig(template);

        assertThat(templates.hasViewAccessToTemplate(template, templateViewUser, roles, false)).isFalse();
    }

    private PipelineTemplateConfig template(final String name) {
        return new PipelineTemplateConfig(new CaseInsensitiveString(name), StageConfigMother.stageConfig("stage1"));
    }

    private Role getSecurityConfigRole(CaseInsensitiveString templateAdmin) {
        return new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(templateAdmin));
    }

    private List<Role> setupRoles(Role securityConfigRole) {
        List<Role> roles = new ArrayList<>();
        roles.add(securityConfigRole);
        return roles;
    }

    private TemplatesConfig configForUserWhoCanViewATemplate() {
        List<PipelineTemplateConfig> templateList = new ArrayList<>();
        templateList.add(PipelineTemplateConfigMother.createTemplate("template100", StageConfigMother.manualStage("stage-name")));
        return new TemplatesConfig(templateList.toArray(new PipelineTemplateConfig[0]));
    }
}
