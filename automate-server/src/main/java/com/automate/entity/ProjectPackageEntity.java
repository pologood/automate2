package com.automate.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * Description: 项目文件包  war jar apk 用于更新具体应用实例
 * @author genx
 * @date 2019/5/26 20:05
 */
@Entity
@Table(name = "CA2_PROJECT_PACKAGE")
public class ProjectPackageEntity {

    public enum Type{
        /**
         * 构建生成
         */
        BUILD,
        /**
         * 手动上传
         */
        UPLOAD
    }

    private int id;

    /**
     * 项目ID
     * @see ProjectEntity#getId()
     */
    private Integer projectId;

    /**
     * 类型
     * @See Type
     */
    private Byte type;

    /**
     * 分支
     */
    private String branch;

    /**
     * 最后提交ID
     */
    private String commitId;

    /**
     * 创建时间
     */
    private Timestamp createTime;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件后缀
     */
    private String suffix;

    /**
     * 文件列表
     */
    private String fileList;

    /**
     * 版本号
     * type == BUILD 时 取pom.xml中的版本号
     * type == UPLOAD 时 手工填写
     */
    private String version;

    /**
     * 备注
     */
    private String remark;

    /**
     * 提交人
     * type == BUILD 时 为0
     * type == UPLOAD 时 上传用户ID
     */
    private Integer userId;

    @Id
    @Column(name = "ID", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "PROJECT_ID", nullable = true)
    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    @Basic
    @Column(name = "TYPE", nullable = true)
    public Byte getType() {
        return type;
    }

    public void setType(Byte type) {
        this.type = type;
    }

    @Basic
    @Column(name = "BRANCH", nullable = true, length = 64)
    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    @Basic
    @Column(name = "COMMIT_ID", nullable = true, length = 64)
    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    @Basic
    @Column(name = "CREATE_TIME", nullable = true)
    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    @Basic
    @Column(name = "FILE_PATH", nullable = true, length = 128)
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Basic
    @Column(name = "SUFFIX", nullable = true, length = 16)
    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Basic
    @Column(name = "FILE_LIST", nullable = true, length = -1)
    public String getFileList() {
        return fileList;
    }

    public void setFileList(String fileList) {
        this.fileList = fileList;
    }

    @Basic
    @Column(name = "VERSION", nullable = true, length = 16)
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Basic
    @Column(name = "REMARK", nullable = true, length = 256)
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Basic
    @Column(name = "USER_ID", nullable = true)
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

}
