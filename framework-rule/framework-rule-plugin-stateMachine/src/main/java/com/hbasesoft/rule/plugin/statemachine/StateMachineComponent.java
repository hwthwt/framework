/**************************************************************************************** 
 Copyright © 2003-2012 hbasesoft Corporation. All rights reserved. Reproduction or       <br>
 transmission in whole or in part, in any form or by any means, electronic, mechanical <br>
 or otherwise, is prohibited without the prior written consent of the copyright owner. <br>
 ****************************************************************************************/
package com.hbasesoft.rule.plugin.statemachine;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hbasesoft.framework.common.ErrorCodeDef;
import com.hbasesoft.framework.common.FrameworkException;
import com.hbasesoft.framework.common.GlobalConstants;
import com.hbasesoft.framework.common.utils.Assert;
import com.hbasesoft.framework.common.utils.CommonUtil;
import com.hbasesoft.framework.common.utils.logger.LoggerUtil;
import com.hbasesoft.framework.rule.core.FlowComponent;
import com.hbasesoft.framework.rule.core.FlowContext;
import com.hbasesoft.framework.rule.core.FlowHelper;
import com.hbasesoft.framework.rule.core.config.FlowConfig;
import com.hbasesoft.framework.rule.core.config.JsonConfigUtil;

/**
 * <Description> <br>
 * 
 * @author 王伟<br>
 * @version 1.0<br>
 * @taskId <br>
 * @CreateDate 2018年8月24日 <br>
 * @since V1.0<br>
 * @see com.hbasesoft.rule.plugin.statemachine <br>
 */
@Component("StateMachineComponent")
public class StateMachineComponent<T extends StateMachineFlowBean> implements FlowComponent<T> {

    /**
     * Description: <br>
     * 
     * @author 王伟<br>
     * @taskId <br>
     * @param flowBean
     * @param flowContext
     * @return
     * @throws Exception <br>
     */
    @Override
    public boolean process(T flowBean, FlowContext flowContext) throws Exception {

        String currentEvent = flowBean.getEvent();
        Assert.notEmpty(currentEvent, ErrorCodeDef.EVENT_NOT_EMPTY);

        FlowConfig config = flowContext.getFlowConfig();
        Map<String, Object> attrMap = config.getConfigAttrMap();

        String state = (String) attrMap.get("begin");
        Assert.notEmpty(state, ErrorCodeDef.BEGIN_STATE_NOT_EMPTY);
        String end = (String) attrMap.get("end");
        Assert.notEmpty(end, ErrorCodeDef.END_STATE_NOT_EMPTY);
        JSONObject control = (JSONObject) attrMap.get("control");
        Assert.notEmpty(control, ErrorCodeDef.CONTROL_NOT_NULL);

        String currentState = flowBean.getState();
        if (StringUtils.isEmpty(currentState)) {
            currentState = state;
            flowBean.setState(currentState);
        }

        // 如果当前流程已经结束，则不往下继续走了
        if (!CommonUtil.match(end, currentState)) {

            JSONArray matchEvents = control.getJSONArray(currentState);
            Assert.notEmpty(matchEvents, ErrorCodeDef.STATE_NOT_MATCH, currentState);

            for (int i = 0, size = matchEvents.size(); i < size; i++) {
                JSONObject eventObj = matchEvents.getJSONObject(i);
                String event = eventObj.getString("event");
                Assert.notEmpty(event, ErrorCodeDef.EVENT_NOT_EMPTY);
                String endState = eventObj.getString("end");
                Assert.notEmpty(event, ErrorCodeDef.END_STATE_NOT_EMPTY);

                String errorState = eventObj.getString("error");
                Assert.notEmpty(errorState, ErrorCodeDef.ERROR_STATE_NOT_FOUND);

                if (CommonUtil.match(event, currentEvent)) {
                    FlowConfig flowConfig = JsonConfigUtil.getFlowConfig(eventObj);
                    FlowContext newFlowContext = new FlowContext(flowConfig, flowContext.getExtendUtils(),
                        flowContext.getParamMap());
                    try {
                        FlowHelper.execute(flowBean, newFlowContext);
                        flowBean.setState(endState);
                    }
                    catch (Exception e) {
                        LoggerUtil.error("flow process error.", e);

                        String code = GlobalConstants.BLANK
                            + (e instanceof FrameworkException ? ((FrameworkException) e).getCode()
                                : ErrorCodeDef.SYSTEM_ERROR_10001);

                        if (errorState.indexOf(GlobalConstants.EQUAL_SPLITER) == -1) {
                            flowBean.setState(errorState);
                        }
                        else {
                            String[] errCodes = StringUtils.split(errorState, GlobalConstants.SPLITOR);
                            String es = null;
                            for (String errCode : errCodes) {
                                String[] codeAndState = StringUtils.split(errCode, GlobalConstants.EQUAL_SPLITER);
                                if (codeAndState.length == 2 && CommonUtil.match(codeAndState[0], code)) {
                                    es = codeAndState[1];
                                    break;
                                }
                            }

                            Assert.notEmpty(es, ErrorCodeDef.ERROR_STATE_NOT_FOUND);
                            flowBean.setState(es);
                        }
                    }
                    break;
                }
            }
        }
        return false;
    }

}