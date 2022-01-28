package net.huansi.job.admin.controller;

import net.huansi.job.admin.core.model.HsJobInfo;
import net.huansi.job.admin.core.model.HsJobLogGlue;
import net.huansi.job.admin.core.util.I18nUtil;
import net.huansi.job.admin.dao.HsJobInfoDao;
import net.huansi.job.admin.dao.HsJobLogGlueDao;
import net.huansi.job.core.biz.model.ReturnT;
import net.huansi.job.core.glue.GlueTypeEnum;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * job code controller
 * @author falcon 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/jobcode")
public class JobCodeController {
	
	@Resource
	private HsJobInfoDao xxlJobInfoDao;
	@Resource
	private HsJobLogGlueDao hsJobLogGlueDao;

	@RequestMapping
	public String index(HttpServletRequest request, Model model, int jobId) {
		HsJobInfo jobInfo = xxlJobInfoDao.loadById(jobId);
		List<HsJobLogGlue> jobLogGlues = hsJobLogGlueDao.findByJobId(jobId);

		if (jobInfo == null) {
			throw new RuntimeException(I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
		}
		if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType())) {
			throw new RuntimeException(I18nUtil.getString("jobinfo_glue_gluetype_unvalid"));
		}

		// valid permission
		JobInfoController.validPermission(request, jobInfo.getJobGroup());

		// Glue类型-字典
		model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());

		model.addAttribute("jobInfo", jobInfo);
		model.addAttribute("jobLogGlues", jobLogGlues);
		return "jobcode/jobcode.index";
	}
	
	@RequestMapping("/save")
	@ResponseBody
	public ReturnT<String> save(Model model, int id, String glueSource, String glueRemark) {
		// valid
		if (glueRemark==null) {
			return new ReturnT<String>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_glue_remark")) );
		}
		if (glueRemark.length()<4 || glueRemark.length()>100) {
			return new ReturnT<String>(500, I18nUtil.getString("jobinfo_glue_remark_limit"));
		}
		HsJobInfo exists_jobInfo = xxlJobInfoDao.loadById(id);
		if (exists_jobInfo == null) {
			return new ReturnT<String>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
		}
		
		// update new code
		exists_jobInfo.setGlueSource(glueSource);
		exists_jobInfo.setGlueRemark(glueRemark);
		exists_jobInfo.setGlueUpdatetime(new Date());

		exists_jobInfo.setUpdateTime(new Date());
		xxlJobInfoDao.update(exists_jobInfo);

		// log old code
		HsJobLogGlue hsJobLogGlue = new HsJobLogGlue();
		hsJobLogGlue.setJobId(exists_jobInfo.getId());
		hsJobLogGlue.setGlueType(exists_jobInfo.getGlueType());
		hsJobLogGlue.setGlueSource(glueSource);
		hsJobLogGlue.setGlueRemark(glueRemark);

		hsJobLogGlue.setAddTime(new Date());
		hsJobLogGlue.setUpdateTime(new Date());
		hsJobLogGlueDao.save(hsJobLogGlue);

		// remove code backup more than 30
		hsJobLogGlueDao.removeOld(exists_jobInfo.getId(), 30);

		return ReturnT.SUCCESS;
	}
	
}
