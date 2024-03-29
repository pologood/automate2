package com.automate.service;

import com.alibaba.fastjson.JSON;
import com.automate.common.utils.SpringContextUtil;
import com.automate.entity.SourceCodeBranchEntity;
import com.automate.entity.SourceCodeEntity;
import com.automate.repository.SourceCodeBranchRepository;
import com.automate.repository.SourceCodeRepository;
import com.automate.vcs.IVCSHelper;
import com.automate.vcs.VCSHelper;
import com.automate.vcs.git.GitHelper;
import com.automate.vcs.vo.CommitLog;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author: genx
 * @date: 2019/1/24 23:26
 */
@Service
public class SourceCodeService {
    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * initialCapacity 设置cache的初始大小，要合理设置该值
     */
    private static Cache<Integer, SourceCodeEntity> sourceCodeCache = Caffeine.newBuilder()
            .initialCapacity(256)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();


    @Autowired
    private SourceCodeRepository sourceCodeRepository;

    @Autowired
    private SourceCodeBranchRepository sourceCodeBranchRepository;

    /**
     * 初始化本地代码库
     */
    @Deprecated
    public void init(SourceCodeEntity sourceCodeEntity) throws Exception {
        IVCSHelper cvsHelper = new GitHelper(sourceCodeEntity);
        Set<String> branchList = cvsHelper.init();
        sync(sourceCodeEntity, cvsHelper, branchList);
    }

    /**
     * 同步代码库
     * TODO 需要后台执行
     */
    public int sync(@NonNull SourceCodeEntity sourceCodeEntity) throws Exception {
        logger.info("开始同步代码仓库:{}", sourceCodeEntity.toJson());

        IVCSHelper cvsHelper =  VCSHelper.create(sourceCodeEntity);
        //检查一下
        Set<String> updateBranchList;
        int total = 0;
        if (!cvsHelper.isLocalRepositoryExist()) {
            updateBranchList = cvsHelper.init();
            total += sync(sourceCodeEntity, cvsHelper, updateBranchList);
        }
        return total;
    }

    public int sync(SourceCodeEntity sourceCodeEntity, IVCSHelper cvsHelper, Collection<String> branchList) throws Exception {
        for (String branchName : branchList) {
            List<CommitLog> commitLogs = cvsHelper.commitLogs(branchName);
            this.updateBranch(sourceCodeEntity.getId(), branchName, commitLogs);
        }
        return branchList.size();
    }

    public void updateBranch(int sourceCodeId, String branchName, List<CommitLog> commitLogs) {
        if (sourceCodeId <= 0 || StringUtils.isEmpty(branchName) || commitLogs.size() == 0) {
            return;
        }
        SourceCodeBranchEntity sourceCodeBranchEntity = sourceCodeBranchRepository.findFirstBySourceCodeIdAndBranchName(sourceCodeId, branchName);

        if (sourceCodeBranchEntity == null) {
            sourceCodeBranchEntity = new SourceCodeBranchEntity();
            sourceCodeBranchEntity.setSourceCodeId(sourceCodeId);
            sourceCodeBranchEntity.setBranchName(branchName);
            sourceCodeBranchEntity.setAutoType(SourceCodeBranchEntity.AutoType.NONE);
        }
        CommitLog lastCommit = commitLogs.get(0);

        if (lastCommit.getId().equals(sourceCodeBranchEntity.getLastCommitId())) {
            //无变化
            return;
        }
        sourceCodeBranchEntity.setLastCommitId(lastCommit.getId());
        sourceCodeBranchEntity.setLastCommitTime(new Timestamp(lastCommit.getCommitTime()));
        sourceCodeBranchEntity.setLastCommitUser(lastCommit.getCommitter().getName());
        sourceCodeBranchEntity.setCommitLog(JSON.toJSONString(commitLogs));
        sourceCodeBranchEntity.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        sourceCodeBranchRepository.save(sourceCodeBranchEntity);
    }

    public SourceCodeEntity getFirstByVcsUrl(String vcsUrl) {
        return sourceCodeRepository.getFirstByVcsUrl(vcsUrl);
    }

    public Iterable<SourceCodeEntity> findAll() {
        return sourceCodeRepository.findAll(Sort.by("id"));
    }

    public Map<Integer, SourceCodeEntity> findAllWidthMap() {
        Iterable<SourceCodeEntity> list = this.findAll();
        Map<Integer, SourceCodeEntity> map = new HashMap<>(64);
        for (SourceCodeEntity sourceCodeEntity : list) {
            map.put(sourceCodeEntity.getId(), sourceCodeEntity);
        }
        return map;
    }

    /**
     * 查询对象
     **/
    public Optional<SourceCodeEntity> getModel(int id) {
        return sourceCodeRepository.findById(id);
    }

    private static SourceCodeEntity getModelStatic(int id) {
        Optional<SourceCodeEntity> model = SpringContextUtil.getBean("sourceCodeService", SourceCodeService.class).getModel(id);
        return model.isPresent() ? model.get() : null;
    }

    public static SourceCodeEntity getModelByCache(int id) {
        return sourceCodeCache.get(id, SourceCodeService::getModelStatic);
    }

    /**
     * 添加对象
     **/
    public void save(SourceCodeEntity model) {
        if (model.getCreateTime() == null) {
            model.setCreateTime(new Timestamp(System.currentTimeMillis()));
        }
        sourceCodeRepository.save(model);

        sourceCodeCache.put(model.getId(), model);
    }

    /**
     * 删除对象
     **/
    public void deleteById(int id) {
        sourceCodeRepository.deleteById(id);

        sourceCodeCache.invalidate(id);
    }


}
