package com.sulin.code.pose.doubbo;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * Filter dubbo的拦截器 可以做AOP日志打印
 */
@Slf4j
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER})
public class DubboLogFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        //可以在RPC传递USER信息或者TID
        RpcContext.getServerContext().setObjectAttachment("USER_ID", 1000L);
        RpcContext.getServerContext().getAttachment("USER_ID");

        //可以获取日志需要的参数
        String serviceName = invocation.getServiceName();
        String methodName = invocation.getMethodName();
        Object[] arguments = invocation.getArguments();

        //do process
        Result invoke = invoker.invoke(invocation);
        if (invoke.hasException()) {
            log.info("rpc hasException", invoke.getException());
        }
        //后置处理
        log.info("after rpc process");
        return invoke;
    }
}
