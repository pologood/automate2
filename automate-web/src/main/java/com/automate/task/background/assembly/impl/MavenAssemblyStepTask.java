package com.automate.task.background.assembly.impl;

import com.automate.exec.ExecCommand;
import com.automate.exec.IExecStreamMonitor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author: genx
 * @date: 2019/2/3 9:59
 */
public class MavenAssemblyStepTask extends AbstractExecAssemblyStepTask {


    public void setSourceCodeId(int sourceCodeId) {

    }

    @Override
    public String[] getLocks() {
        return new String[0];
    }

    @Override
    public void valid() throws Exception {

    }

    /*
    使用-B参数：该参数表示让Maven使用批处理模式构建项目，能够避免一些需要人工参与交互而造成的挂起状态
    使用-e参数：如果构建出现异常，该参数能让Maven打印完整的stack trace，以方便分析错误原因。

    使用-U参数： 该参数能强制让Maven检查所有SNAPSHOT依赖更新，确保集成基于最新的状态，如果没有该参数，Maven默认以天为单位检查更新，而持续集成的频率应该比这高很多。

     */

    public enum Lifecycle{
        clean,
        validate,
        compile,
        test,
        /**
         * 打包   package 是关键字
         */
        pack,
        verify,
        install,
        site,
        deploy
    }



    @Override
    public ExecCommand buildExecCommand() throws Exception {
        StringBuilder cmd = new StringBuilder("mvn");

        String shortcut = StringUtils.trimToEmpty(this.shortcut);

        //3个命令需要特殊处理
        if(this.clean || Lifecycle.clean.name().equals(shortcut)){
            cmd.append(" clean");
        }

        if(Lifecycle.pack.name().equals(shortcut) || "package".equals(shortcut)){
            cmd.append(" package");
        } else {
            if(Lifecycle.test.name().equals(shortcut)){
                this.testSkip = false;
            }

            for (Lifecycle l : Lifecycle.values()) {
                if(l.name().equals(shortcut)){
                    cmd.append(" ").append(shortcut);
                }
            }
        }

        if(this.testSkip){
            cmd.append(" -DskipTests=true");
        }

        /*
            使用-B参数：该参数表示让Maven使用批处理模式构建项目，能够避免一些需要人工参与交互而造成的挂起状态
            使用-e参数：如果构建出现异常，该参数能让Maven打印完整的stack trace，以方便分析错误原因。
         */


        cmd.append(" -B -e");

        if(StringUtils.isNotEmpty(this.custom)){
            cmd.append(" ").append(this.custom);
        }

        ExecCommand execCommand = new ExecCommand(Arrays.asList(cmd.toString()), null, null, execStreamMonitor);
        return execCommand;
    }

    private boolean clean = true;
    private boolean testSkip = true;

    /**
     * 快捷方式
     * 与 Lifecycle对应
     * @see Lifecycle
     */
    private String shortcut;

    /**
     * 自定义的内容
     * 比如   sonar代码审查
     * sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=00996135a7d5148b7fab335d4dba233c3dcb7afc
     */
    private String custom;


    private IExecStreamMonitor execStreamMonitor = null;


    public boolean isClean() {
        return clean;
    }

    public void setClean(boolean clean) {
        this.clean = clean;
    }

    public boolean isTestSkip() {
        return testSkip;
    }

    public void setTestSkip(boolean testSkip) {
        this.testSkip = testSkip;
    }

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(String shortcut) {
        this.shortcut = shortcut;
    }

    public String getCustom() {
        return custom;
    }

    public void setCustom(String custom) {
        this.custom = custom;
    }

    public IExecStreamMonitor getExecStreamMonitor() {
        return execStreamMonitor;
    }

    public void setExecStreamMonitor(IExecStreamMonitor execStreamMonitor) {
        this.execStreamMonitor = execStreamMonitor;
    }
}
