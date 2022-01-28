package net.huansi.job.core.handler.impl;

import net.huansi.job.core.context.HsJobContext;
import net.huansi.job.core.context.HsJobHelper;
import net.huansi.job.core.glue.GlueTypeEnum;
import net.huansi.job.core.handler.IJobHandler;
import net.huansi.job.core.log.HsJobFileAppender;
import net.huansi.job.core.util.ScriptUtil;

import java.io.File;

/**
 * Created by falcon on 17/4/27.
 */
public class ScriptJobHandler extends IJobHandler {

    private int jobId;
    private long glueUpdatetime;
    private String gluesource;
    private GlueTypeEnum glueType;

    public ScriptJobHandler(int jobId, long glueUpdatetime, String gluesource, GlueTypeEnum glueType){
        this.jobId = jobId;
        this.glueUpdatetime = glueUpdatetime;
        this.gluesource = gluesource;
        this.glueType = glueType;

        // clean old script file
        File glueSrcPath = new File(HsJobFileAppender.getGlueSrcPath());
        if (glueSrcPath.exists()) {
            File[] glueSrcFileList = glueSrcPath.listFiles();
            if (glueSrcFileList!=null && glueSrcFileList.length>0) {
                for (File glueSrcFileItem : glueSrcFileList) {
                    if (glueSrcFileItem.getName().startsWith(String.valueOf(jobId)+"_")) {
                        glueSrcFileItem.delete();
                    }
                }
            }
        }

    }

    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }

    @Override
    public void execute() throws Exception {

        if (!glueType.isScript()) {
            HsJobHelper.handleFail("glueType["+ glueType +"] invalid.");
            return;
        }

        // cmd
        String cmd = glueType.getCmd();

        // make script file
        String scriptFileName = HsJobFileAppender.getGlueSrcPath()
                .concat(File.separator)
                .concat(String.valueOf(jobId))
                .concat("_")
                .concat(String.valueOf(glueUpdatetime))
                .concat(glueType.getSuffix());
        File scriptFile = new File(scriptFileName);
        if (!scriptFile.exists()) {
            ScriptUtil.markScriptFile(scriptFileName, gluesource);
        }

        // log file
        String logFileName = HsJobContext.getXxlJobContext().getJobLogFileName();

        // script params：0=param、1=分片序号、2=分片总数
        String[] scriptParams = new String[3];
        scriptParams[0] = HsJobHelper.getJobParam();
        scriptParams[1] = String.valueOf(HsJobContext.getXxlJobContext().getShardIndex());
        scriptParams[2] = String.valueOf(HsJobContext.getXxlJobContext().getShardTotal());

        // invoke
        HsJobHelper.log("----------- script file:"+ scriptFileName +" -----------");
        int exitValue = ScriptUtil.execToFile(cmd, scriptFileName, logFileName, scriptParams);

        if (exitValue == 0) {
            HsJobHelper.handleSuccess();
            return;
        } else {
            HsJobHelper.handleFail("script exit value("+exitValue+") is failed");
            return ;
        }

    }

}
