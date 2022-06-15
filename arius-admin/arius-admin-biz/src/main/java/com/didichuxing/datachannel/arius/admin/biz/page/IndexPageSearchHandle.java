package com.didichuxing.datachannel.arius.admin.biz.page;

import static com.didichuxing.datachannel.arius.admin.persistence.constant.ESOperateContant.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.didichuxing.datachannel.arius.admin.biz.cluster.ClusterPhyManager;
import com.didichuxing.datachannel.arius.admin.common.Tuple;
import com.didichuxing.datachannel.arius.admin.common.bean.common.PaginationResult;
import com.didichuxing.datachannel.arius.admin.common.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.common.bean.dto.indices.IndexQueryDTO;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.index.IndexCatCell;
import com.didichuxing.datachannel.arius.admin.common.bean.vo.indices.IndexCatCellVO;
import com.didichuxing.datachannel.arius.admin.common.constant.index.IndexBlockEnum;
import com.didichuxing.datachannel.arius.admin.common.constant.index.IndexStatusEnum;
import com.didichuxing.datachannel.arius.admin.common.util.AriusObjUtils;
import com.didichuxing.datachannel.arius.admin.common.util.BatchProcessor;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.common.util.ListUtils;
import com.didichuxing.datachannel.arius.admin.core.service.app.AppService;
import com.didichuxing.datachannel.arius.admin.core.service.es.ESIndexCatService;
import com.didichuxing.datachannel.arius.admin.core.service.es.ESIndexService;
import com.didiglobal.logi.elasticsearch.client.response.setting.index.IndexConfig;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;
import com.google.common.collect.Lists;

public class IndexPageSearchHandle extends AbstractPageSearchHandle<IndexQueryDTO, IndexCatCellVO> {
    private static final String DEFAULT_SORT_TERM = "timestamp";

    @Autowired
    private AppService          appService;

    @Autowired
    private ESIndexCatService   esIndexCatService;

    @Autowired
    private ESIndexService      esIndexService;

    @Override
    protected Result<Boolean> checkCondition(IndexQueryDTO condition, Integer appId) {
        if (!appService.isAppExists(appId)) {
            return Result.buildParamIllegal("项目不存在");
        }
        if (StringUtils.isNotBlank(condition.getHealth()) && !IndexStatusEnum.isStatusExit(condition.getHealth())) {
            return Result.buildParamIllegal(String.format("健康状态%s非法", condition.getHealth()));
        }

        String indexName = condition.getIndex();
        if (!AriusObjUtils.isBlack(indexName) && (indexName.startsWith("*") || indexName.startsWith("?"))) {
            return Result.buildParamIllegal("索引名称不允许带类似*, ?等通配符查询");
        }

        return Result.buildSucc(true);
    }

    @Override
    protected void initCondition(IndexQueryDTO condition, Integer appId) {
        if (null == condition.getPage()) {
            condition.setPage(1L);
        }

        if (null == condition.getSize() || 0 == condition.getSize()) {
            condition.setSize(10L);
        }

        if (AriusObjUtils.isBlack(condition.getSortTerm())) {
            condition.setSortTerm(DEFAULT_SORT_TERM);
        }
    }

    @Override
    protected PaginationResult<IndexCatCellVO> buildPageData(IndexQueryDTO condition, Integer appId) {
        return getIndexCatCellsFromES(condition, appId);
    }

    /**
     * 获取索引Cat/index信息
     *
     * 业务上限制ES深分页(不考虑10000条之后的数据), 由前端限制
     */
    private PaginationResult<IndexCatCellVO> getIndexCatCellsFromES(IndexQueryDTO condition, Integer appId) {
        try {
            Tuple<Long, List<IndexCatCell>> totalHitAndIndexCatCellListTuple;
            if (appService.isSuperApp(appId)) {
                totalHitAndIndexCatCellListTuple = esIndexCatService.syncGetCatIndexInfo(condition.getCluster(),
                    condition.getIndex(), condition.getHealth(), appId, null,
                    (condition.getPage() - 1) * condition.getSize(), condition.getSize(), condition.getSortTerm(),
                    condition.getOrderByDesc());
            } else {
                totalHitAndIndexCatCellListTuple = esIndexCatService.syncGetCatIndexInfo(null, condition.getIndex(),
                    condition.getHealth(), appId, condition.getCluster(),
                    (condition.getPage() - 1) * condition.getSize(), condition.getSize(), condition.getSortTerm(),
                    condition.getOrderByDesc());
            }

            if (null == totalHitAndIndexCatCellListTuple) {
                LOGGER.warn("class=IndicesPageSearchHandle||method=getIndexCatCellsFromES||clusters={}||index={}||"
                            + "errMsg=get empty index cat info from es",
                    condition.getCluster(), condition.getIndex());
                return null;
            }

            //设置索引阻塞信息
            List<IndexCatCell> finalIndexCatCellList = batchFetchIndexBlockInfo(
                totalHitAndIndexCatCellListTuple.getV2());
            List<IndexCatCellVO> indexCatCellVOList = ConvertUtil.list2List(finalIndexCatCellList,
                IndexCatCellVO.class);

            return PaginationResult.buildSucc(indexCatCellVOList, totalHitAndIndexCatCellListTuple.getV1(),
                condition.getPage(), condition.getSize());
        } catch (Exception e) {
            LOGGER.error(
                "class=IndicesPageSearchHandle||method=getIndexCatCellsFromES||clusters={}||index={}||errMsg={}",
                condition.getCluster(), condition.getIndex(), e.getMessage(), e);
            return PaginationResult.buildFail("获取分页索引列表失败");
        }
    }

    /**
     * 批量构建索引阻塞信息
     * @param catCellList    索引cat/index基本信息
     * @return               List<IndexCatCell>
     */
    private List<IndexCatCell> batchFetchIndexBlockInfo(List<IndexCatCell> catCellList) {
        List<IndexCatCell> finalIndexCatCellList = Lists.newArrayList();
        Map<String, List<IndexCatCell>> cluster2IndexCatCellListMap = ConvertUtil.list2MapOfList(catCellList,
            IndexCatCell::getClusterPhy, indexCatCell -> indexCatCell);
        if (MapUtils.isEmpty(cluster2IndexCatCellListMap)) {
            return finalIndexCatCellList;
        }

        for (Map.Entry<String, List<IndexCatCell>> entry : cluster2IndexCatCellListMap.entrySet()) {
            String cluster = entry.getKey();
            List<IndexCatCell> indexCatCellList = entry.getValue();

            //批量操作
            BatchProcessor.BatchProcessResult<IndexCatCell, List<IndexCatCell>> batchResult = new BatchProcessor<IndexCatCell, List<IndexCatCell>>()
                .batchList(indexCatCellList).batchSize(50).processor(items -> buildBlockInfo(cluster, items)).process();

            if (!batchResult.isSucc() && (batchResult.getErrorMap().size() > 0)) {
                LOGGER.warn(
                    "class=IndicesPageSearchHandle||method=batchFetchIndexBlockInfo||cluster={}||errMsg=batch result error:{}",
                    cluster, batchResult.getErrorMap());
            }

            List<List<IndexCatCell>> resultList = batchResult.getResultList();
            for (List<IndexCatCell> indexCatCells : resultList) {
                finalIndexCatCellList.addAll(indexCatCells);
            }
        }

        return finalIndexCatCellList;
    }

    /**
     * 批量构建索引阻塞信息
     * @param cluster              集群名称
     * @param indexCatCellList     批量索引列表
     * @return
     */
    private List<IndexCatCell> buildBlockInfo(String cluster, List<IndexCatCell> indexCatCellList) {
        List<IndexCatCell> indexCatCellWithBlockInfo = Lists.newArrayList();
        List<String> indexNameList = indexCatCellList.stream().map(IndexCatCell::getIndex).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(indexNameList)) {
            LOGGER.warn(
                "class=IndicesPageSearchHandle||method=buildBlockInfo||cluster={}||index={}||errMsg=index is empty",
                cluster, ListUtils.strList2String(indexNameList));
            return indexCatCellWithBlockInfo;
        }

        Map<String, IndexConfig> name2IndexConfigMap = esIndexService.syncGetIndexSetting(cluster, indexNameList, 3);
        for (IndexCatCell indexCatCell : indexCatCellList) {
            IndexConfig indexConfig = name2IndexConfigMap.get(indexCatCell.getIndex());
            if (null == indexConfig) {
                LOGGER.warn(
                    "class=IndicesPageSearchHandle||method=batchFetchIndexBlockInfo||cluster={}||index={}||errMsg=index config is empty",
                    indexCatCell.getClusterPhy(), indexCatCell.getIndex());
                continue;
            }

            Tuple<Boolean, Boolean> writeAndReadBlockFromMerge = new Tuple<>();
            try {
                //build from es setUp settings
                Tuple<Boolean, Boolean> writeAndReadBlockFromSetUpSettingTuple = getBlockInfoFromSetUpSettings(
                    indexConfig);
                //build from es default settings
                Tuple<Boolean, Boolean> writeAndReadBlockFromDefaultSettingTuple = getBlockInfoFromDefaultSettings(
                    indexConfig);
                writeAndReadBlockFromMerge = mergeBlockInfo(writeAndReadBlockFromSetUpSettingTuple,
                    writeAndReadBlockFromDefaultSettingTuple);
            } catch (Exception e) {
                writeAndReadBlockFromMerge.setV1(null);
                writeAndReadBlockFromMerge.setV2(null);
                LOGGER.error(
                    "class=IndicesPageSearchHandle||method=batchFetchIndexBlockInfo||cluster={}||index={}||errMsg={}",
                    indexCatCell.getClusterPhy(), indexCatCell.getIndex(), e.getMessage(), e);
            }

            indexCatCell.setReadFlag(writeAndReadBlockFromMerge.getV1() != null && writeAndReadBlockFromMerge.getV1());
            indexCatCell.setWriteFlag(writeAndReadBlockFromMerge.getV2() != null && writeAndReadBlockFromMerge.getV2());
        }

        indexCatCellWithBlockInfo.addAll(indexCatCellList);
        return indexCatCellWithBlockInfo;
    }

    /**
     * 从默认 索引setting 中获取获取block信息
     * @param  indexConfig  indexConfig from es
     * @return read write
     */
    private Tuple<Boolean, Boolean> getBlockInfoFromDefaultSettings(IndexConfig indexConfig) {
        Tuple<Boolean, Boolean> writeAndReadBlockFromDefaultSettingTuple = new Tuple<>();
        writeAndReadBlockFromDefaultSettingTuple.setV1(null);
        writeAndReadBlockFromDefaultSettingTuple.setV2(null);

        if (AriusObjUtils.isNull(indexConfig.getOther(DEFAULTS))) {
            return writeAndReadBlockFromDefaultSettingTuple;
        }
        JSONObject defaultsSettingsObj = JSON.parseObject(indexConfig.getOther(DEFAULTS).toString());

        JSONObject indexSettingsObj = JSON.parseObject(defaultsSettingsObj.get(INDEX).toString());
        if (null == indexSettingsObj) {
            return writeAndReadBlockFromDefaultSettingTuple;
        }

        JSONObject blocksObj = JSON.parseObject(indexSettingsObj.get(BLOCKS).toString());
        if (null == blocksObj) {
            return writeAndReadBlockFromDefaultSettingTuple;
        }

        if (null != blocksObj.get(IndexBlockEnum.READ.getType())) {
            writeAndReadBlockFromDefaultSettingTuple
                .setV1(Boolean.parseBoolean(blocksObj.get(IndexBlockEnum.READ.getType()).toString()));
        }
        if (null != blocksObj.get(IndexBlockEnum.WRITE.getType())) {
            writeAndReadBlockFromDefaultSettingTuple
                .setV2(Boolean.parseBoolean(blocksObj.get(IndexBlockEnum.WRITE.getType()).toString()));
        }

        return writeAndReadBlockFromDefaultSettingTuple;
    }

    /**
     * 从手动配置的 索引setting 中获取获取block信息
     * @param  indexConfig  indexConfig from es
     * @return read write
     */
    private Tuple<Boolean, Boolean> getBlockInfoFromSetUpSettings(IndexConfig indexConfig) {
        //init
        Tuple<Boolean, Boolean> writeAndReadBlockFromSetUpSettingTuple = new Tuple<>();
        writeAndReadBlockFromSetUpSettingTuple.setV1(null);
        writeAndReadBlockFromSetUpSettingTuple.setV2(null);

        //get from indexConfig
        Map<String, String> settings = indexConfig.getSettings();
        if (MapUtils.isNotEmpty(settings)) {
            if (null != settings.get(READ)) {
                writeAndReadBlockFromSetUpSettingTuple.setV1(Boolean.parseBoolean(settings.get(READ)));
            }
            if (null != settings.get(WRITE)) {
                writeAndReadBlockFromSetUpSettingTuple.setV2(Boolean.parseBoolean(settings.get(WRITE)));
            }
        }

        return writeAndReadBlockFromSetUpSettingTuple;
    }

    /**
     * 合并block信息
     *
     * @param tupleFromSetUp   手动配置setting
     * @param tupleFromDefault 默认setting
     * @return {@link Tuple}<{@link Boolean}, {@link Boolean}> Tuple<read, write>
     */
    private Tuple<Boolean, Boolean> mergeBlockInfo(Tuple<Boolean, Boolean> tupleFromSetUp,
                                                                    Tuple<Boolean, Boolean> tupleFromDefault) {
        Tuple<Boolean, Boolean> mergeBlockTuple = new Tuple<>();

        //set read block info
        if (null != tupleFromSetUp.getV1()) {
            mergeBlockTuple.setV1(tupleFromSetUp.getV1());
        } else if (null != tupleFromDefault.getV1()) {
            mergeBlockTuple.setV1(tupleFromDefault.getV1());
        }

        //set write block info
        if (null != tupleFromSetUp.getV2()) {
            mergeBlockTuple.setV2(tupleFromSetUp.getV2());
        } else if (null != tupleFromDefault.getV2()) {
            mergeBlockTuple.setV2(tupleFromDefault.getV2());
        }

        return mergeBlockTuple;
    }
}
