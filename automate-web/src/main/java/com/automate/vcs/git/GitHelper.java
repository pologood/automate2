package com.automate.vcs.git;

import com.automate.event.EventCenter;
import com.automate.event.po.SourceCodePullEvent;
import com.automate.vcs.AbstractVCSHelper;
import com.automate.vcs.IVCSRepository;
import com.automate.vcs.vo.CommitLog;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author: genx
 * @date: 2019/1/26 22:23
 */
public class GitHelper extends AbstractVCSHelper {

    private static final Logger logger = LoggerFactory.getLogger(GitHelper.class);

    private CredentialsProvider credentialsProvider = null;

    public GitHelper(IVCSRepository repository) {
        super(repository);
        if (StringUtils.isNotEmpty(this.userName) && StringUtils.isNotEmpty(this.passWord)) {
            credentialsProvider = new UsernamePasswordCredentialsProvider(this.userName, this.passWord);
        }
    }

    /**
     * 初始化 clone项目
     * @return 分支列表
     */
    @Override
    public Set<String> init() throws Exception {
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setCredentialsProvider(this.credentialsProvider);
        cloneCommand.setURI(this.remoteUrl).setDirectory(new File(this.localDir));

        //TODO 为啥没用呢
        cloneCommand.setCloneAllBranches(true);

        cloneCommand.setProgressMonitor(new JgitProgressMonitor());
        logger.debug("clone {}", this.remoteUrl);
        Git git = cloneCommand.call();
        git.close();
        Set<String> branchList = new HashSet(8);
        List<Ref> list = git.branchList().call();
        for (Ref ref : list) {
            branchList.add(ref.getName().substring(GitContants.BRANCH_NAME_PREFIX_LEN));
        }
        //TODO 第一次同步只返回了 master 分支

        branchList.addAll(this.update());
        return branchList;
    }

    /**
     * 同步项目
     * @throws Exception
     * @return 有变化的分支列表
     */
    @Override
    public Set<String> update() throws Exception {
        return update(null);
    }

    /**
     * 同步项目的一个分支
     * @param branchName 分支名称
     * @throws Exception
     * @return 有变化的分支列表
     */
    @Override
    public Set<String> update(String branchName) throws Exception {
        FileRepository db = openFileRepository();
        Git git = null;
        try {
            git = Git.wrap(db);
            Map<String, String> localBranchMap = new HashMap(8);
            //查看本地分支
            List<Ref> list = git.branchList().call();
            for (Ref ref : list) {
                localBranchMap.put(ref.getName().substring(GitContants.BRANCH_NAME_PREFIX_LEN), ref.getObjectId().toObjectId().getName());
            }

            //查询远程分支
            Collection<Ref> refs = git.lsRemote().setTags(false).setHeads(false).setCredentialsProvider(this.credentialsProvider).call();
            int update = 0;
            Set<String> updateBranchList = new HashSet(8);
            for (Ref ref : refs) {
                if (ref.getName().startsWith(GitContants.BRANCH_NAME_PREFIX)) {
                    String remoteBranchName = ref.getName().substring(GitContants.BRANCH_NAME_PREFIX_LEN);
                    if(StringUtils.isEmpty(branchName) || branchName.equals(remoteBranchName)) {
                        logger.debug("check out {} | {}", remoteBranchName, this.remoteUrl);
                        CheckoutCommand checkoutCommand = git.checkout().setName(remoteBranchName);
                        String id = localBranchMap.get(remoteBranchName);
                        if (id == null) {
                            //新建分支
                            logger.info("{},新建分支:{}", this.localDir, remoteBranchName);
                            checkoutCommand.setCreateBranch(true);
                        }
                        checkoutCommand.call();

                        //reset hard
                        git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/" + remoteBranchName).call();

                        //从远程库 pull 代码
                        PullResult pullResult = git.pull().setRemoteBranchName(remoteBranchName).setCredentialsProvider(this.credentialsProvider).call();
                        if(!pullResult.isSuccessful()){
                            logger.warn("push 失败");
                            continue;
                        }
                        if(id == null || !id.equals(pullResult.getMergeResult().getNewHead().getName())){
                            logger.error("pushed, before:{}, after:{}", id, pullResult.getMergeResult().getNewHead().getName());
                            //TODO pushed
                            EventCenter.post(new SourceCodePullEvent(super.sourceCodeId, remoteBranchName, pullResult.getMergeResult().getNewHead().getName()));

                            updateBranchList.add(remoteBranchName);
                        }

                        update++;
                    }
                }
            }
            if(update == 0) {
                throw new IllegalArgumentException("the branchName is not correct?");
            }

            return updateBranchList;
        } finally {
            if(git != null){
                git.close();
            }
            db.close();
        }
    }

    /**
     * 查询单个分支的所有提交历史(本地)
     * @param  branchName
     * @return
     * @throws Exception
     */
    @Override
    public List<CommitLog> commitLogs(String branchName) throws Exception {
        FileRepository db = openFileRepository();
        Git git = null;
        try {
            git = Git.wrap(db);
            git.checkout().setName(branchName).call();
            //.all()       标识查看所有分支的日志
            Iterator<RevCommit> commits = git.log().call().iterator();
            List<CommitLog> list = new ArrayList<>(256);
            for (Iterator<RevCommit> it = commits; it.hasNext(); ) {
                RevCommit commit = it.next();
                list.add(JgitFormat.parse(commit));
            }
            return list;
        } finally {
            if(git != null){
                git.close();
            }
            db.close();
        }
    }

    @Override
    public String checkOut(String branchName, String commitId) throws Exception {
        FileRepository db = openFileRepository();
        Git git = null;
        try {
            git = Git.wrap(db);

            git.checkout().setName(branchName).call();
            if(StringUtils.isNotBlank(commitId)) {
                Ref resetCommand = git.reset().setMode(ResetCommand.ResetType.HARD).setRef(commitId).call();
                return resetCommand.getObjectId().toObjectId().name();
            } else {
                //reset hard
                Ref ref = git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/" + branchName).call();
                return ref.getObjectId().toObjectId().name();
            }
        } finally {
            if(git != null){
                git.close();
            }
            db.close();
        }
    }

    @Override
    public boolean isLocalRepositoryExist(){
        File file = new File(this.localDir);
        if (!file.exists()) {
           return false;
        }
        if (!this.localDir.endsWith(GitContants.DOT_GIT)) {
            file = new File(this.localDir + File.separator + GitContants.DOT_GIT);
        }
        if (!file.exists()) {
            return false;
        }
        return true;
    }

    @Override
    public void testConnetction() throws Exception {
        LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository();
        lsRemoteCommand.setCredentialsProvider(this.credentialsProvider);
        lsRemoteCommand.setRemote(this.remoteUrl);

        lsRemoteCommand.setTimeout(5);
        lsRemoteCommand.call();
    }

    private FileRepository openFileRepository() throws IOException {
        File file = new File(this.localDir);
        if (!file.exists()) {
            //TODO clone 远程库
            throw new RuntimeException("文件路径不存在：" + this.localDir);
        }
        if (!this.localDir.endsWith(GitContants.DOT_GIT)) {
            file = new File(this.localDir + File.separator + GitContants.DOT_GIT);
        }
        if (!file.exists()) {
            throw new RuntimeException("未处于版本控制下：" + this.localDir);
        }
        return new FileRepository(file);
    }
}
