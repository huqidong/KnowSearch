package com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author chengxiang
 * @date 2022/06/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClusterNodeInfo {

    private String nodeSet;

    private String nodeName;
}
