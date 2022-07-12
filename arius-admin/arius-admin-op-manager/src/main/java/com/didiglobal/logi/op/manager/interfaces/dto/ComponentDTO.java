package com.didiglobal.logi.op.manager.interfaces.dto;

import com.didiglobal.logi.op.manager.domain.component.entity.value.ComponentGroupConfig;
import com.didiglobal.logi.op.manager.domain.component.entity.value.ComponentHost;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author didi
 * @date 2022-07-12 2:16 下午
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComponentDTO {
    /**
     * 组件id
     */
    private String id;
    /**
     * 状态(0 green,1 yellow,2 red,3 unKnow)
     */
    private Integer status;
    /**
     * 包含的组件id列表
     */
    private String containComponentIds;
    /**
     * 组件名
     */
    private String name;
    /**
     * 关联的安装包id
     */
    private String packageId;
    /**
     * 创建时间
     */
    private Timestamp createTime;
    /**
     * 更新时间
     */
    private Timestamp updateTime;
    /**
     * 是否卸载
     */
    private Integer isDelete;

    /**
     * 值对象，关联group配置
     */
    private List<ComponentGroupConfigDTO> groupConfigList;


    /**
     * 值对象，关联host
     */
    private List<ComponentHostDTO> hostList;

}
