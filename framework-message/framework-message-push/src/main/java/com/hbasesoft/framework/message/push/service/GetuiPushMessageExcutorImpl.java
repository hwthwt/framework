/**
 * 
 */
package com.hbasesoft.framework.message.push.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gexin.rp.sdk.base.IPushResult;
import com.gexin.rp.sdk.base.ITemplate;
import com.gexin.rp.sdk.base.impl.ListMessage;
import com.gexin.rp.sdk.base.impl.SingleMessage;
import com.gexin.rp.sdk.base.impl.Target;
import com.gexin.rp.sdk.http.IGtPush;
import com.gexin.rp.sdk.template.TransmissionTemplate;
import com.hbasesoft.framework.common.ErrorCodeDef;
import com.hbasesoft.framework.common.ServiceException;
import com.hbasesoft.framework.common.utils.PropertyHolder;
import com.hbasesoft.framework.common.utils.bean.JsonUtil;
import com.hbasesoft.framework.message.api.Attachment;
import com.hbasesoft.framework.message.core.service.MessageExcutor;

/**
 * <Description> <br>
 * 
 * @author 伟<br>
 * @version 1.0<br>
 * @CreateDate 2015-1-17 <br>
 * @see com.hbasesoft.framework.message.service.getui <br>
 */
@Service
public class GetuiPushMessageExcutorImpl implements MessageExcutor {

    private static final String CHANNEL_ID = "GETUI_PUSH";

    /*
     * (non-Javadoc)
     * @see com.hbasesoft.framework.message.service.MessageExcutor#sendMessage(java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String[], java.util.List)
     */
    public String sendMessage(String title, String content, String sender, String[] receivers,
        List<Attachment> attachments) throws ServiceException {
        try {
            IPushResult result = null;
            if (receivers.length == 1) {
                result = sendSingleMessage(content, receivers[0]);
            }
            else {
                result = sendListMessage(content, receivers);
            }
            return JsonUtil.writeObj2JSON(result);
        }
        catch (Exception e) {
            throw new ServiceException(ErrorCodeDef.GE_TUI_ERROR_10038, e);
        }
    }

    /**
     * Description: <br>
     * 
     * @author yang.zhipeng <br>
     * @taskId <br>
     * @param content <br>
     * @return <br>
     */
    private ITemplate getTemplate(String content) {
        TransmissionTemplate template = new TransmissionTemplate();
        template.setAppId(PropertyHolder.getProperty("GETUI.APP_ID"));
        template.setAppkey(PropertyHolder.getProperty("GETUI.APP_KEY"));
        // 透传消息设置，1为强制启动应用，客户端接收到消息后就会立即启动应用；2为等待应用启动
        template.setTransmissionType(2);
        template.setTransmissionContent(content);
        return template;
    }

    /**
     * Description: <br>
     * 
     * @author yang.zhipeng <br>
     * @taskId <br>
     * @return <br>
     * @throws IOException <br>
     */
    private IGtPush getGetPush() throws IOException {
        String host = PropertyHolder.getProperty("GETUI.HOST");
        String appKey = PropertyHolder.getProperty("GETUI.APP_KEY");
        String masterSecret = PropertyHolder.getProperty("GETUI.MASTER_SECRET");
        IGtPush push = new IGtPush(host, appKey, masterSecret);
        push.connect();
        return push;
    }

    /**
     * Description: <br>
     * 
     * @author yang.zhipeng <br>
     * @taskId <br>
     * @param content <br>
     * @param receivers <br>
     * @return <br>
     * @throws IOException <br>
     */
    private IPushResult sendListMessage(String content, String[] receivers) throws IOException {
        ListMessage message = new ListMessage();
        message.setData(getTemplate(content));

        // 设置消息离线，并设置离线时间
        message.setOffline(true);
        // 离线有效时间，单位为毫秒，可选
        message.setOfflineExpireTime(24 * 1000 * 3600);

        // 配置推送目标
        List<Target> targets = new ArrayList<Target>();
        Target target = null;
        String appid = PropertyHolder.getProperty("GETUI.APP_ID");
        for (String receiver : receivers) {
            target = new Target();
            target.setAppId(appid);
            target.setClientId(receiver);
            targets.add(target);
        }

        IGtPush push = getGetPush();

        // 获取taskID
        String taskId = push.getContentId(message);
        // 使用taskID对目标进行推送
        return push.pushMessageToList(taskId, targets);
    }

    /**
     * Description: <br>
     * 
     * @author yang.zhipeng <br>
     * @taskId <br>
     * @param content <br>
     * @param receiver <br>
     * @return <br>
     * @throws IOException <br>
     */
    private IPushResult sendSingleMessage(String content, String receiver) throws IOException {
        SingleMessage message = new SingleMessage();
        message.setOffline(true);
        // 离线有效时间，单位为毫秒，可选
        message.setOfflineExpireTime(24 * 3600 * 1000);
        message.setData(getTemplate(content));
        message.setPushNetWorkType(0); // 判断是否客户端是否wifi环境下推送，1为在WIFI环境下，0为不限制网络环境。

        Target target = new Target();
        target.setAppId(PropertyHolder.getProperty("GETUI.APP_ID"));
        target.setClientId(receiver);
        // 用户别名推送，cid和用户别名只能2者选其一
        // String alias = "个";
        // target.setAlias(alias);
        return getGetPush().pushMessageToSingle(message, target);
    }

    /**
     * Description: <br>
     * 
     * @author 王伟<br>
     * @taskId <br>
     * @return <br>
     */
    @Override
    public String getChannelId() {
        return CHANNEL_ID;
    }

}