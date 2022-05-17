package com.didichuxing.datachannel.arius.admin.biz.template.new_srv.cold.impl;

import com.didichuxing.datachannel.arius.admin.biz.template.new_srv.base.impl.BaseTemplateSrvImpl;
import com.didichuxing.datachannel.arius.admin.biz.template.new_srv.cold.ColdManager;
import com.didichuxing.datachannel.arius.admin.common.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.common.constant.template.TemplateServiceEnum;
import org.springframework.stereotype.Service;

/**
 * @author chengxiang, zqr
 * @date 2022/5/13
 */
@Service
public class ColdManagerImpl extends BaseTemplateSrvImpl implements ColdManager {


    @Override
    public TemplateServiceEnum templateSrv() {
        return TemplateServiceEnum.TEMPLATE_COLD;
    }

    @Override
    public Result<Void> isTemplateSrvAvailable(Integer logicTemplateId) {
        return Result.buildSucc();
    }

    @Override
    public Result<Void> move2ColdNode(Integer logicTemplateId) {
        if (!isTemplateSrvOpen(logicTemplateId)) {
            return Result.buildFail("没有开启冷热分离模板服务");
        }
        return Result.buildSucc();
    }

    @Override
    public int fetchClusterDefaultHotDay(String phyCluster) {
        return 0;
    }

    @Override
    public boolean updateHotIndexRack(Long physicalId, String tgtRack) {
        return true;
    }

}
