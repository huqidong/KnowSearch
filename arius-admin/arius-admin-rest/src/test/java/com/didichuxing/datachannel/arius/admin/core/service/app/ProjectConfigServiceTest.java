package com.didichuxing.datachannel.arius.admin.core.service.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.didichuxing.datachannel.arius.admin.AriusAdminApplicationTest;
import com.didichuxing.datachannel.arius.admin.common.Tuple;
import com.didichuxing.datachannel.arius.admin.common.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.common.bean.dto.app.ProjectConfigDTO;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.app.ProjectConfig;
import com.didichuxing.datachannel.arius.admin.common.bean.po.app.ProjectConfigPO;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.core.service.app.impl.ProjectConfigServiceImpl;
import com.didichuxing.datachannel.arius.admin.persistence.mysql.app.ProjectConfigDAO;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.ibatis.mapping.MappedStatement;
import org.elasticsearch.common.recycler.Recycler.C;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@Transactional(timeout = 1000)
@Rollback
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
class ProjectConfigServiceTest{
	
	@Mock
	private ProjectConfigDAO mockProjectConfigDAO;
	
	@InjectMocks
	private ProjectConfigServiceImpl projectConfigService;
	@BeforeEach
    void setUp() {
        initMocks(this);
    }
	

	@Test
	void testGetProjectConfig() {
		// Setup
		final ProjectConfig expectedResult = new ProjectConfig(0, 0, 0, 0, 0, 0, "memo");
		
		// Configure ProjectConfigDAO.getByProjectId(...).
		final ProjectConfigPO projectConfigPO = new ProjectConfigPO(0, 0, 0, 0, 0, 0, "memo");
		when(mockProjectConfigDAO.getByProjectId(0)).thenReturn(projectConfigPO);
		
		// Run the test
		final ProjectConfig result = projectConfigService.getProjectConfig(0);
		
		// Verify the results
		assertThat(result).isEqualTo(expectedResult);
	}
	
	@Test
	void testProjectId2ProjectConfigMap() {
		// Setup
		
		// Configure ProjectConfigDAO.listAll(...).
		final List<ProjectConfigPO> projectConfigPOS = Arrays.asList(new ProjectConfigPO(1, 0, 0, 0, 0, 0, "memo"));
		when(mockProjectConfigDAO.listAll()).thenReturn(projectConfigPOS);
				final Map<Integer, ProjectConfig> expectedResult = Maps.newHashMap();
		for (Entry<Integer, ProjectConfigPO> configPOEntry : ConvertUtil.list2Map(projectConfigPOS,
				ProjectConfigPO::getProjectId).entrySet()) {
			expectedResult.put(configPOEntry.getKey(),ConvertUtil.obj2Obj(configPOEntry.getValue(),
					ProjectConfig.class));
		}

		
		// Run the test
		final Map<Integer, ProjectConfig> result = projectConfigService.projectId2ProjectConfigMap();
		
		// Verify the results
		assertThat(result).isEqualTo(expectedResult);
	}
	
	
	
	@Test
	void testUpdateOrInitProjectConfig() {
		// Setup
		final ProjectConfigDTO configDTO = new ProjectConfigDTO(0, 0, 0, 0, 0, 0, "memo");
		when(mockProjectConfigDAO.checkProjectConfigByProjectId(0)).thenReturn(false);
		
		// Configure ProjectConfigDAO.getByProjectId(...).
		final ProjectConfigPO projectConfigPO = new ProjectConfigPO(0, 0, 0, 0, 0, 0, "memo");
		when(mockProjectConfigDAO.getByProjectId(0)).thenReturn(projectConfigPO);
		
		when(mockProjectConfigDAO.update(new ProjectConfigPO(0, 0, 0, 0, 0, 0, "memo"))).thenReturn(1);
		when(mockProjectConfigDAO.insert(new ProjectConfigPO(0, 0, 0, 0, 0, 0, "memo"))).thenReturn(1);
		
		// Run the test
		final Tuple<Result<Void>, ProjectConfigPO> result = projectConfigService.updateOrInitProjectConfig(
				configDTO, "operator");
		
		// Verify the results
	}
	
	@Test
	void testDeleteByProjectId() {
		// Setup
		when(mockProjectConfigDAO.checkProjectConfigByProjectId(anyInt())).thenReturn(true);
		when(mockProjectConfigDAO.deleteByProjectId(anyInt())).thenReturn(1);
		
		// Run the test
		projectConfigService.deleteByProjectId(0);
		
		// Verify the results
		verify(mockProjectConfigDAO).deleteByProjectId(0);
	}
}