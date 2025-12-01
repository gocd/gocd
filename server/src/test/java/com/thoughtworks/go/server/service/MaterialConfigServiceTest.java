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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialConfigServiceTest {
    @Mock
    private GoConfigService goConfigService;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private SecurityService securityService;

    private MaterialConfigService materialConfigService;

    private final String user = "looser";
    private final GitMaterialConfig GIT_REPO_1 = git("http://test.com");
    private final GitMaterialConfig GIT_REPO_1_DUPLICATE = git("http://test.com");
    private final GitMaterialConfig GIT_REPO_2 = git("http://crap.com");
    private final GitMaterialConfig GIT_REPO_3 = git("http://another.com");

    @BeforeEach
    void setup() {
        when(securityService.hasViewPermissionForGroup(user, "group1")).thenReturn(true);
        when(securityService.hasViewPermissionForGroup(user, "group2")).thenReturn(false);
        when(securityService.hasViewPermissionForGroup(user, "group3")).thenReturn(true);
        when(securityService.hasOperatePermissionForGroup(any(), eq("group3"))).thenReturn(true);

        GIT_REPO_1_DUPLICATE.setFolder("non-default-folder");
        assertThat(GIT_REPO_1)
            .as("GIT_REPO_1 has a duplicate with the same fingerprint, but not equal")
            .isNotEqualTo(GIT_REPO_1_DUPLICATE)
            .extracting(GitMaterialConfig::getFingerprint)
            .isEqualTo(GIT_REPO_1_DUPLICATE.getFingerprint());

        PipelineConfigs pipelineGroup1 = new BasicPipelineConfigs();
        pipelineGroup1.setGroup("group1");
        PipelineConfig pipelineConfig1 = new PipelineConfig();
        pipelineConfig1.setName("pipeline1");
        pipelineConfig1.addMaterialConfig(GIT_REPO_1);
        pipelineConfig1.addMaterialConfig(GIT_REPO_2);
        pipelineGroup1.add(pipelineConfig1);

        PipelineConfigs pipelineGroup2 = new BasicPipelineConfigs();
        pipelineGroup2.setGroup("group2");
        PipelineConfig pipelineConfig2 = new PipelineConfig();
        pipelineConfig2.setName("pipeline2");
        pipelineConfig2.addMaterialConfig(GIT_REPO_3);
        pipelineGroup2.add(pipelineConfig2);

        PipelineConfigs pipelineGroup3 = new BasicPipelineConfigs();
        pipelineGroup3.setGroup("group3");
        PipelineConfig pipelineConfig3 = new PipelineConfig();
        pipelineConfig3.setName("pipeline3");
        pipelineConfig3.addMaterialConfig(GIT_REPO_1_DUPLICATE);
        pipelineGroup3.add(pipelineConfig3);

        PipelineGroups pipelineGroups = new PipelineGroups(pipelineGroup1, pipelineGroup2, pipelineGroup3);
        when(goConfigService.groups()).thenReturn(pipelineGroups);

        materialConfigService = new MaterialConfigService(goConfigService, securityService);
    }

    @Test
    void shouldGetUniqueMaterialConfigsToWhichUserHasViewPermission() {
        MaterialConfigs materialConfigs = materialConfigService.getMaterialConfigs(user);

        assertThat(materialConfigs).containsExactly(GIT_REPO_1, GIT_REPO_2);
    }

    @Nested
    class GetMaterialConfig {

        @Test
        void shouldGetMaterialConfigByFingerprint() {
            HttpOperationResult result = new HttpOperationResult();
            MaterialConfig materialConfig = materialConfigService.getMaterialConfig(user, GIT_REPO_2.getFingerprint(), result);

            assertThat(materialConfig).isEqualTo(GIT_REPO_2);
            assertThat(result.canContinue()).isTrue();
        }

        @Test
        void shouldPopulateErrorCorrectlyWhenMaterialNotFound() {
            HttpOperationResult result = new HttpOperationResult();
            MaterialConfig materialConfig = materialConfigService.getMaterialConfig(user, "unknown-fingerprint", result);

            assertThat(materialConfig).isNull();
            assertThat(result.httpCode()).isEqualTo(404);
        }

        @Test
        void shouldPopulateErrorCorrectlyWhenUnauthorizedToViewMaterial() {
            HttpOperationResult result = new HttpOperationResult();
            MaterialConfig materialConfig = materialConfigService.getMaterialConfig(user, GIT_REPO_3.getFingerprint(), result);

            assertThat(materialConfig).isNull();
            assertThat(result.httpCode()).isEqualTo(403);
        }
    }

    @Nested
    class GetUsages {
        @Test
        void shouldReturnUsagesOfAGivenMaterial() {
            List<String> usages = materialConfigService.getUsagesForMaterial(user, GIT_REPO_1.getFingerprint());
            assertThat(usages).containsExactly("pipeline1", "pipeline3");
        }

        @Test
        void shouldReturnEmptyMapIfNotUsagesFound() {
            List<String> usages = materialConfigService.getUsagesForMaterial(user, "unknown-fingerprint");
            assertThat(usages).isEmpty();
        }
    }

    @Nested
    class GetMaterialConfigsWithPermissions {
        @Test
        void shouldGetUniqueMaterialConfigsWithPermissionsToWhichUserHasViewPermission() {
            Map<MaterialConfig, Boolean> materialConfigs = materialConfigService.getMaterialConfigsToOperatePermissions(user);

            assertThat(materialConfigs).containsExactlyInAnyOrderEntriesOf(Map.of(
                GIT_REPO_1, true, // Can operate via group3 permission to duplicate material
                GIT_REPO_2, false)
            );
        }

        @Test
        void shouldGetUniqueMaterialConfigsWithPermissionsToWhichUserHasViewPermissionAgain() {
            when(securityService.hasOperatePermissionForGroup(any(), eq("group1"))).thenReturn(true);
            when(securityService.hasOperatePermissionForGroup(any(), eq("group3"))).thenReturn(true);

            Map<MaterialConfig, Boolean> materialConfigs = materialConfigService.getMaterialConfigsToOperatePermissions(user);

            assertThat(materialConfigs).containsExactlyInAnyOrderEntriesOf(Map.of(
                GIT_REPO_1, true, // Can operate via either group 1 or group3 permission
                GIT_REPO_2, true) // Can operate via either group 1 permission
            );
        }

        @Test
        void shouldReturnEmptyMapIfUserDoesNotHavePermissionToViewAnyGroup() {
            Map<MaterialConfig, Boolean> materialConfigs = materialConfigService.getMaterialConfigsToOperatePermissions("dummy_user");
            assertThat(materialConfigs).isEmpty();
        }
    }

    @Nested
    class GetMaterialConfigWithOperate {
        @Test
        void shouldGetMaterialConfigByFingerprint() {
            MaterialConfig materialConfig = materialConfigService.getMaterialConfig(user, GIT_REPO_1.getFingerprint());
            assertThat(materialConfig).isEqualTo(GIT_REPO_1_DUPLICATE);
        }

        @Test
        void shouldThrowRecordNotFoundExceptionIfMaterialNotFound() {
            assertThatCode(() -> materialConfigService.getMaterialConfig(user, git("http://dummy.com").getFingerprint()))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Material not found");
        }

        @Test
        void shouldThrowUnAuthorizedExceptionIfViewPermissionIsFalse() {
            assertThatCode(() -> materialConfigService.getMaterialConfig(user, GIT_REPO_3.getFingerprint()))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessage("Do not have view permission to this material");
        }

        @Test
        void shouldThrowUnAuthorizedExceptionIfOperatePermissionIsFalse() {
            assertThatCode(() -> materialConfigService.getMaterialConfig(user, GIT_REPO_2.getFingerprint()))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessage("Do not have permission to trigger this material");
        }
    }
}
