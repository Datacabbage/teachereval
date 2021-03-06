package com.example.teachereval.controller;

import com.example.teachereval.pojo.*;
import com.example.teachereval.service.GroupService;
import com.example.teachereval.service.MenuService;
import com.example.teachereval.service.ScoreService;
import com.example.teachereval.service.UserService;
import com.example.teachereval.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class HomeAction {
    @Autowired
    private UserService userService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ScoreService scoreService;
    /*主要目的是设置和获取教师的用户id*/
    private int userid;
    private int groupid;
    private int claid;

    /*获取添加班级的id*/
    @RequestMapping("getAddClaid")
    @ResponseBody
    public int getClaid() {
        return claid;
    }

    public void setClaid(int claid) {
        this.claid = claid;
    }

    /*获取添加课程的教师id*/
    @RequestMapping("getAddUserid")
    @ResponseBody
    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    /*获取添加课程的批次id*/
    @RequestMapping("getAddGroupid")
    @ResponseBody
    public int getGroupid() { return groupid; }

    public void setGroupid(int groupid) { this.groupid = groupid; }

    /*登录*/
    @RequestMapping("/doLogin")
    public String login(TblUser user, HttpSession session){
        user.setPassword(Encrypt.MD5(user.getPassword()));
        TblUser u=userService.login(user);
        if(u!=null){
            session.setAttribute("userinfo",u);
            return "index";
        }
        return "404";
    }

    @RequestMapping("/logOut")
    public String logOut(HttpServletRequest request){
        request.getSession().removeAttribute("userinfo");
        request.getSession().removeAttribute("rolename");
        return "login";
    }

    /*确认用户*/
    @RequestMapping("/getInfo")
    @ResponseBody
    public Map<String,String> getInfo(HttpSession session){
        TblUser u=(TblUser)session.getAttribute("userinfo");
        List<TblUserInfo> list=userService.getUserInfo(u.getUserid());
        String role="";
        if(null!=list&&list.size()>0){
            for(TblUserInfo a:list){
                role+=a.getRoleName()+" ";
            }
        }
        session.setAttribute("rolename",role);
        Map<String,String> map=new HashMap<>();
        map.put("username",u.getUsername());
        map.put("rolename",role);
        return map;
    }

    /*注册*/
    @RequestMapping("/doRegistered")
    public String registered(TblUser user){
        user.setPassword(Encrypt.MD5(user.getPassword()));
        if(userService.save(user)>0){
            return "login";
        }
        return "404";
    }

    /*跳转修改密码*/
    @RequestMapping("/editPassword")
    public String editPassword(){ return "changepassword";}

    /*修改密码*/
    @RequestMapping("/doEditPassword")
    @ResponseBody
    public Map<String,String> doEditPassword(String oldPassword,String newPassword,HttpSession session){
        TblUser u=(TblUser)session.getAttribute("userinfo");
        String pass=Encrypt.MD5(oldPassword);
        Map<String ,String> map=new HashMap<String,String>();
        if(u.getPassword().equals(Encrypt.MD5(oldPassword))){
            u.setPassword(Encrypt.MD5(newPassword));
            userService.update(u);
            map.put("statusCode","200");
            map.put("message","修改成功！！");
            return map;
        }
        return null;
    }

    /*注册用户名是否可用，false表示已经被注册*/
    @RequestMapping("/doCheck")
    @ResponseBody
    public boolean check(String username){
        TblUser user=userService.selectByUsername(username);
        if(null!=user&&user.getUsername()!=null){
            return false;
        }
        return true;
    }

    @RequestMapping("/getTreeMenu")
    @ResponseBody
    public List<MenuJson> getTreeMenu(HttpSession session){
        //获取原始数据
        TblUser user= (TblUser) session.getAttribute("userinfo");
        List<TblMenu> rootMenu = menuService.loadUserResources(user);

        // 最后的结果
        List<MenuJson> menuList = new ArrayList<MenuJson>();

        // 先找到所有的一级菜单
        for (int i = 0; i < rootMenu.size(); i++) {
            // 一级菜单没有parentId
            if (rootMenu.get(i).getParMen()==0) {
                MenuJson mj=new MenuJson(rootMenu.get(i).getMenid(),rootMenu.get(i).getParMen(),
                        rootMenu.get(i).getMenName(),rootMenu.get(i).getMenUrl());
                menuList.add(mj);
            }
        }

        // 为一级菜单设置子菜单，getChild是递归调用的
        for (MenuJson menu : menuList) {
            menu.setChildren(getChild(menu.getId(), rootMenu));
        }

        return menuList;
    }

    @RequestMapping("/getTotalMenu")
    @ResponseBody
    public List<TblMenu> getTotalMenu(HttpSession session){
        //获取原始数据
        TblUser user= (TblUser) session.getAttribute("userinfo");
        List<TblMenu> rootMenu = menuService.loadUserResources(user);
        List<TblMenu> getTotalMenu=new ArrayList<>();

        // 先找到所有的一级菜单
        for (int i = 0; i < rootMenu.size(); i++) {
            // 一级菜单没有parentId
            if (rootMenu.get(i).getParMen()==0) {
                getTotalMenu.add(rootMenu.get(i));
            }
        }
        return getTotalMenu;
    }

    @RequestMapping("/getOne")
    @ResponseBody
    public List<MenuJson> getOne(MenuJson json,HttpSession session){
        //获取原始数据
        TblUser user= (TblUser) session.getAttribute("userinfo");
        List<TblMenu> rootMenu = menuService.loadUserResources(user);

        // 子菜单
        List<MenuJson> childList = new ArrayList<>();
        for (TblMenu menu : rootMenu) {
            // 遍历所有节点，将父菜单id与传过来的id比较
            if (menu.getParMen()==Integer.valueOf(json.getId().split("m")[1])) {
                MenuJson mj=new MenuJson(menu.getMenid(),menu.getParMen(),
                        menu.getMenName(),menu.getMenUrl());
                childList.add(mj);
            }
        }
        return childList;
    }

    /**
     * 递归算法获取菜单
     * @param id
     * @param rootMenu
     * @return
     */
    private List<MenuJson> getChild(String id, List<TblMenu> rootMenu) {
        // 子菜单
        List<MenuJson> childList = new ArrayList<>();
        for (TblMenu menu : rootMenu) {
            // 遍历所有节点，将父菜单id与传过来的id比较
            if (menu.getParMen()==Integer.valueOf(id.split("m")[1])) {
                MenuJson mj=new MenuJson(menu.getMenid(),menu.getParMen(),
                        menu.getMenName(),menu.getMenUrl());
                childList.add(mj);
            }
        }
        // 递归退出条件
        if(childList.size()!=0){
            // 把子菜单的子菜单再循环一遍
            for (MenuJson jsonMenu : childList) {// 没有url子菜单还有子菜单
                // 递归
                jsonMenu.setChildren(getChild(jsonMenu.getId(), rootMenu));
            }
        }else{
            return null;
        }
        return childList;
    }

    /*根据用户名获取用户信息*/
    @RequestMapping("/getClassInfo")
    @ResponseBody
    public TblUserInfo getClassInfo(TblUserInfo info){
        return userService.getClassInfo(info);
    }
    //控制跳转
    @RequestMapping("/toMenu")
    public String toMenu(){ return "view/MenuTable";}
    @RequestMapping("/toUser")
    public String toUser(){ return "view/UserTable";}
    @RequestMapping("/toRole")
    public String toRole(){ return "view/RoleTable";}
    @RequestMapping("/toCourse")
    public String toCourse(){ return "view/CourseTable";}
    @RequestMapping("/toClass")
    public String toClass(){ return "view/ClassTable";}
    @RequestMapping("/toExercise")
    public String toExercise(){ return "view/ExerciseTable"; }
    @RequestMapping("/toGroup")
    public String toGroup(){ return "view/GroupTable"; }
    @RequestMapping("/toEval")
    public String toEval(){ return "view/EvalTable"; }
    @RequestMapping("/roleMenu")
    public String roleMenu(){ return "view/add/RoleMenu";}
    @RequestMapping("/toScore")
    public String toScore(){
        long l=0;           //计算当前时间是否到达批次规定的时间，大于0到达，否则还没到达
        String endTime=groupService.getTotal().getEndTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date d;
        Date now=new Date();
        try {
            d = sdf.parse(endTime);
            l =now.getTime() - d.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if(l>0){
            //判断是否提交到score分数表中，没提交则执行提交
            if(null!= Constant.Statistics&&Constant.Statistics.size()>0){
                Map<ScoreFlag,Double> target=new HashMap<>();
                List<ScoreStatistics> scoreList=new ArrayList<>();
                for (Map.Entry<Integer,ScoreStatistics> entry : Constant.Statistics.entrySet()) {
                    ScoreFlag sf=new ScoreFlag(entry.getValue().getTeacherid(),entry.getValue().getClaid());
                    target.put(sf, (double) 0);
                    scoreList.add(entry.getValue());
                }
                for(Map.Entry<ScoreFlag,Double> entry2 : target.entrySet()){
                    double time=0;              //次数
                    double total=0;             //总计
                    for(ScoreStatistics ss:scoreList){
                        ScoreFlag sf=new ScoreFlag(ss.getTeacherid(),ss.getClaid());
                        if(entry2.getKey().equals(sf)){
                            time++;
                            total+=ss.getScore();
                        }
                    }
                    TblScore gold=new TblScore();
                    gold.setScoid(entry2.getKey().getTeacherid());
                    gold.setClaid(entry2.getKey().getClaid());
                    gold.setScore(total/time);
                    scoreService.save(gold);
                }
            }
            return "view/ScoreTable";
        }
        return "view/TooEarly";
    }
    @RequestMapping("/AddCourseToTeacher")
    public String toAddCourse(int userid){
        this.setUserid(userid);
        return "view/add/addCourse";
    }
    @RequestMapping("/AddCourseToGroup")
    public String AddCourseToGroup(int groupid){
        this.setGroupid(groupid);
        return "view/add/addCourseToGroup";
    }
    @RequestMapping("/eval")
    public String eval(int userid,int claid){
        this.setUserid(userid);
        this.setClaid(claid);
        return "view/add/eval";
    }
}
