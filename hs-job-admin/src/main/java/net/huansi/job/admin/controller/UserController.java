package net.huansi.job.admin.controller;

import net.huansi.job.admin.controller.annotation.PermissionLimit;
import net.huansi.job.admin.core.model.HsJobGroup;
import net.huansi.job.admin.core.model.HsJobUser;
import net.huansi.job.admin.core.util.I18nUtil;
import net.huansi.job.admin.dao.HsJobGroupDao;
import net.huansi.job.admin.dao.HsJobUserDao;
import net.huansi.job.admin.service.LoginService;
import net.huansi.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author falcon 2019-05-04 16:39:50
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Resource
    private HsJobUserDao hsJobUserDao;
    @Resource
    private HsJobGroupDao hsJobGroupDao;

    @RequestMapping
    @PermissionLimit(adminuser = true)
    public String index(Model model) {

        // 执行器列表
        List<HsJobGroup> groupList = hsJobGroupDao.findAll();
        model.addAttribute("groupList", groupList);

        return "user/user.index";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String username, int role) {

        // page list
        List<HsJobUser> list = hsJobUserDao.pageList(start, length, username, role);
        int list_count = hsJobUserDao.pageListCount(start, length, username, role);

        // filter
        if (list!=null && list.size()>0) {
            for (HsJobUser item: list) {
                item.setPassword(null);
            }
        }

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表
        return maps;
    }

    @RequestMapping("/add")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> add(HsJobUser hsJobUser) {

        // valid username
        if (!StringUtils.hasText(hsJobUser.getUsername())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input")+I18nUtil.getString("user_username") );
        }
        hsJobUser.setUsername(hsJobUser.getUsername().trim());
        if (!(hsJobUser.getUsername().length()>=4 && hsJobUser.getUsername().length()<=20)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }
        // valid password
        if (!StringUtils.hasText(hsJobUser.getPassword())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input")+I18nUtil.getString("user_password") );
        }
        hsJobUser.setPassword(hsJobUser.getPassword().trim());
        if (!(hsJobUser.getPassword().length()>=4 && hsJobUser.getPassword().length()<=20)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }
        // md5 password
        hsJobUser.setPassword(DigestUtils.md5DigestAsHex(hsJobUser.getPassword().getBytes()));

        // check repeat
        HsJobUser existUser = hsJobUserDao.loadByUserName(hsJobUser.getUsername());
        if (existUser != null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("user_username_repeat") );
        }

        // write
        hsJobUserDao.save(hsJobUser);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/update")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> update(HttpServletRequest request, HsJobUser hsJobUser) {

        // avoid opt login seft
        HsJobUser loginUser = (HsJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (loginUser.getUsername().equals(hsJobUser.getUsername())) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }

        // valid password
        if (StringUtils.hasText(hsJobUser.getPassword())) {
            hsJobUser.setPassword(hsJobUser.getPassword().trim());
            if (!(hsJobUser.getPassword().length()>=4 && hsJobUser.getPassword().length()<=20)) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
            }
            // md5 password
            hsJobUser.setPassword(DigestUtils.md5DigestAsHex(hsJobUser.getPassword().getBytes()));
        } else {
            hsJobUser.setPassword(null);
        }

        // write
        hsJobUserDao.update(hsJobUser);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/remove")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> remove(HttpServletRequest request, int id) {

        // avoid opt login seft
        HsJobUser loginUser = (HsJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (loginUser.getId() == id) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }

        hsJobUserDao.delete(id);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/updatePwd")
    @ResponseBody
    public ReturnT<String> updatePwd(HttpServletRequest request, String password){

        // valid password
        if (password==null || password.trim().length()==0){
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码不可为空");
        }
        password = password.trim();
        if (!(password.length()>=4 && password.length()<=20)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }

        // md5 password
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        // update pwd
        HsJobUser loginUser = (HsJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);

        // do write
        HsJobUser existUser = hsJobUserDao.loadByUserName(loginUser.getUsername());
        existUser.setPassword(md5Password);
        hsJobUserDao.update(existUser);

        return ReturnT.SUCCESS;
    }

}
