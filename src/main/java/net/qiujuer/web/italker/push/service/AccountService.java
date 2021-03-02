package net.qiujuer.web.italker.push.service;

import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.api.account.LoginModel;
import net.qiujuer.web.italker.push.bean.api.base.ResponseModel;
import net.qiujuer.web.italker.push.bean.api.account.AccountRspModel;
import net.qiujuer.web.italker.push.bean.api.account.RegisterModel;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.factory.UserFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Description: 后端入口
 * Crete by Anding on 2019-12-01
 */
//实际访问路径127.0.0.1/api/account/。。。
@Path("/account")
public class AccountService extends BaseService {
    // 登录接口
    @POST
    @Path("/login")
    // 指定请求与返回的相应体为JSON Jersey中使用@Path注解来设置资源
    //注解Consumes告诉传入的为json，回去的时候也使用json，必须要指定返回格式，json不止可以返回json也可以返回xml格式
    //jersey只是为了restful框架的一个轻量级框架，基本概念是可以识别get和post。即时是同一个地址，使用不同的get和post则执行不同的逻辑
    //传入是json
    @Consumes(MediaType.APPLICATION_JSON)
    //返回时json
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<AccountRspModel> login(LoginModel model) {
        if (!LoginModel.check(model)) {
            // 返回参数异常
            return ResponseModel.buildParameterError();
        }
        // 登录逻辑操作 数据库查询
        User user = UserFactory.login(model.getAccount(), model.getPassword());
        if (user != null) {
            //如果有携带PushId 需要将id绑定到用户信息中 为后续推送添加标识
            if (!Strings.isNullOrEmpty(model.getPushId())) {
                // 绑定PushId
                return bind(user, model.getPushId());
            }
            //返回当前的账户
            AccountRspModel rspModel = new AccountRspModel(user);
            return ResponseModel.buildOk(rspModel);
        } else {
            // 登录失败
            return ResponseModel.buildLoginError();
        }
    }

    // 注册接口
    @POST
    @Path("/register")
    // 指定请求与返回的相应体为JSON
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<AccountRspModel> register(RegisterModel model) {

        //检查参数
        if (!RegisterModel.check(model)) {
            //返回参数异常
            return ResponseModel.buildParameterError();
        }

        //检测账户已存在
        User user = UserFactory.findByPhone(model.getAccount().trim());
        if (user != null) {
            return ResponseModel.buildHaveAccountError();
        }

        //检测用户名是否已注册过
        user = UserFactory.findByName(model.getName().trim());
        if (user != null) {
            return ResponseModel.buildHaveNameError();
        }

        //开始逻辑注册 数据库
        user = UserFactory.register(model.getAccount(),
                model.getPassword(),
                model.getName());

        if (user != null) {

            // 如果携带了PushId 需要绑定进账户信息
            if (!Strings.isNullOrEmpty(model.getPushId())) {
                return bind(user, model.getPushId());
            }

            //返回当前的账户信息
            AccountRspModel rspModel = new AccountRspModel(user);
            return ResponseModel.buildOk(rspModel);

        } else {
            //注册异常
            return ResponseModel.buildRegisterError();
        }
    }

    // 绑定设备PushId 接口
    @POST
    @Path("/bind/{pushId}")
    // 指定请求与返回的相应体为JSON
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // 从请求头中获取token字段
    // pushId 从url地址中获取
    public ResponseModel<AccountRspModel> bind(@HeaderParam("token") String token,
                                               @PathParam("pushId") String pushId) {
        if (Strings.isNullOrEmpty(token) ||
                Strings.isNullOrEmpty(pushId))
            // 返回参数异常
            return ResponseModel.buildParameterError();

        // 拿到自己的个人信息
//        User user = UserFactory.findByToken(token);
        User user = getSelf();
        return bind(user, pushId);
    }

    /**
     * 绑定的操作
     *
     * @param self   自己
     * @param pushId PushId
     * @return User
     */
    private ResponseModel<AccountRspModel> bind(User self, String pushId) {
        // 进行设备Id绑定操作
        User user = UserFactory.bindPushId(self, pushId);

        if (user == null) {
            // 绑定失败 服务器异常
            return ResponseModel.buildServiceError();
        }

        // 返回当前的账户，并且已经绑定
        AccountRspModel rspModel = new AccountRspModel(user, true);
        return ResponseModel.buildOk(rspModel);
    }

}
