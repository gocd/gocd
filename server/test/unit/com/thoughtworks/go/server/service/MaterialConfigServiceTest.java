package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class MaterialConfigServiceTest {
	@Mock
	GoConfigService goConfigService;

	@Mock
	SecurityService securityService;

	@Before
	public void setup() throws Exception {
		initMocks(this);
	}

	@Test
	public void shouldGetMaterialConfigs() {
		when(goConfigService.allGroups()).thenReturn(Arrays.asList("group1", "group2"));
		String user = "looser";
		when(securityService.hasViewPermissionForGroup(user, "group1")).thenReturn(false);
		when(securityService.hasViewPermissionForGroup(user, "group2")).thenReturn(true);
		PipelineConfigs pipelineConfigs = new PipelineConfigs();
		PipelineConfig pipelineConfig = new PipelineConfig();
		GitMaterialConfig gitMaterialConfig1 = new GitMaterialConfig("http://test.com");
		GitMaterialConfig getMaterialConfig2 = new GitMaterialConfig("http://crap.com");
		pipelineConfig.addMaterialConfig(gitMaterialConfig1);
		pipelineConfig.addMaterialConfig(getMaterialConfig2);
		pipelineConfigs.add(pipelineConfig);
		when(goConfigService.getAllPipelinesInGroup("group2")).thenReturn(pipelineConfigs);

		MaterialConfigService materialConfigService = new MaterialConfigService(goConfigService, securityService);

		MaterialConfigs materialConfigs = materialConfigService.getMaterialConfigs(user);

		verify(goConfigService, never()).getAllPipelinesInGroup("group1");
		assertThat(materialConfigs.size(), is(2));
		assertThat(materialConfigs.get(0), is((MaterialConfig) gitMaterialConfig1));
		assertThat(materialConfigs.get(1), is((MaterialConfig) getMaterialConfig2));
	}
}