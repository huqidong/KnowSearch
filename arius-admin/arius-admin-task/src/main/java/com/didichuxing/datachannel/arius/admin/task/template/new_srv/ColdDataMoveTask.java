package com.didichuxing.datachannel.arius.admin.task.template.new_srv;

import com.didichuxing.datachannel.arius.admin.biz.template.srv.cold.ColdManager;
import com.didichuxing.datachannel.arius.admin.common.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.task.BaseConcurrentTemplateTask;
import com.didiglobal.logi.job.annotation.Task;
import com.didiglobal.logi.job.common.TaskResult;
import com.didiglobal.logi.job.core.job.Job;
import com.didiglobal.logi.job.core.job.JobContext;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author chengxiang
 * @date 2022/06/07
 */
@Task(name = "ColdDataMoveTask", description = "admin冷数据搬迁服务", cron = "0 30 22 * * ?", autoRegister = true)
public class ColdDataMoveTask extends BaseConcurrentTemplateTask implements Job {

    private static final ILog LOGGER = LogFactory.getLog(ColdDataMoveTask.class);

    @Autowired
    private ColdManager       coldManager;

    @Override
    public TaskResult execute(JobContext jobContext) throws Exception {
        LOGGER.info("class=ColdDataMoveTask||method=execute||msg=ColdDataMoveTask start");
        if (execute()) {
            return TaskResult.SUCCESS;
        }
        return TaskResult.FAIL;
    }

    @Override
    public String getTaskName() {
        return "ColdDataMoveTask";
    }

    @Override
    public int poolSize() {
        return 10;
    }

    @Override
    public int current() {
        return 5;
    }
    
    @Override
    protected boolean executeByLogicTemplate(Integer logicId) {
        try {
            final Result<Boolean> result = coldManager.move2ColdNode(logicId);
            if (Boolean.FALSE.equals(result.getData())) {
                LOGGER.warn("class=ColdDataMoveTask||method=executeByLogicTemplate||logicId={}||msg={}", logicId,
                        result.getMessage());
            }
            return result.getData();
        } catch (Exception e) {
            LOGGER.error("class=ColdDataMoveTask||method=executeByLogicTemplate||logicId={}||msg=admin冷数据搬迁服务",
                    logicId, e);
        }
        
        return Boolean.TRUE;
    }
}