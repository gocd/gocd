package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class MaterialConfigServiceTest {
	@Mock
	GoConfigService goConfigService;

	@Mock
	SecurityService securityService;

	String user;
	MaterialConfigService materialConfigService;

	@Before
	public void setup() throws Exception {
		initMocks(this);

		user = "looser";
		when(securityService.hasViewPermissionForGroup(user, "group1")).thenReturn(true);
		when(securityService.hasViewPermissionForGroup(user, "group2")).thenReturn(false);
		when(securityService.hasViewPermissionForGroup(user, "group3")).thenReturn(true);

		PipelineConfigs pipelineGroup1 = new PipelineConfigs();
		pipelineGroup1.setGroup("group1");
		PipelineConfig pipelineConfig1 = new PipelineConfig();
		GitMaterialConfig gitMaterialConfig1 = new GitMaterialConfig("http://test.com");
		pipelineConfig1.addMaterialConfig(gitMaterialConfig1);
		GitMaterialConfig getMaterialConfig2 = new GitMaterialConfig("http://crap.com");
		pipelineConfig1.addMaterialConfig(getMaterialConfig2);
		pipelineGroup1.add(pipelineConfig1);

		PipelineConfigs pipelineGroup2 = new PipelineConfigs();
		pipelineGroup2.setGroup("group2");
		PipelineConfig pipelineConfig2 = new PipelineConfig();
		GitMaterialConfig gitMaterialConfig3 = new GitMaterialConfig("http://another.com");
		pipelineConfig2.addMaterialConfig(gitMaterialConfig3);
		pipelineGroup2.add(pipelineConfig2);

		PipelineConfigs pipelineGroup3 = new PipelineConfigs();
		pipelineGroup3.setGroup("group3");
		PipelineConfig pipelineConfig3 = new PipelineConfig();
		GitMaterialConfig gitMaterialConfig4 = new GitMaterialConfig("http://test.com");
		pipelineConfig1.addMaterialConfig(gitMaterialConfig4);
		pipelineGroup3.add(pipelineConfig3);

		PipelineGroups pipelineGroups = new PipelineGroups(pipelineGroup1, pipelineGroup2, pipelineGroup3);
		when(goConfigService.groups()).thenReturn(pipelineGroups);

		materialConfigService = new MaterialConfigService(goConfigService, securityService);
	}

	@Test
	public void shouldGetUniqueMaterialConfigsToWhichUserHasViewPermission() {
		MaterialConfigs materialConfigs = materialConfigService.getMaterialConfigs(user);

		assertThat(materialConfigs.size(), is(2));
		assertThat(materialConfigs.get(0), is((MaterialConfig) new GitMaterialConfig("http://test.com")));
		assertThat(materialConfigs.get(1), is((MaterialConfig) new GitMaterialConfig("http://crap.com")));
	}

	@Test
	public void shouldGetMaterialConfigByFingerprint() {
		HttpOperationResult result = new HttpOperationResult();
		GitMaterialConfig gitMaterialConfig = new GitMaterialConfig("http://crap.com");
		MaterialConfig materialConfig = materialConfigService.getMaterialConfig(user, gitMaterialConfig.getFingerprint(), result);

		assertThat(materialConfig, is((MaterialConfig) gitMaterialConfig));
		assertThat(result.canContinue(), is(true));
	}

	@Test
	public void shouldPopulateErrorCorrectlyWhenMaterialNotFound_getMaterialConfigByFingerprint() {
		HttpOperationResult result = new HttpOperationResult();
		MaterialConfig materialConfig = materialConfigService.getMaterialConfig(user, "unknown-fingerprint", result);

		assertThat(materialConfig, is(nullValue()));
		assertThat(result.httpCode(), is(404));
	}
}