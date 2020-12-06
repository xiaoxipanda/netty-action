package io.netty.example.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.example.common.Operation;
import io.netty.example.common.OperationResult;
import io.netty.example.common.RequestMessage;
import io.netty.example.common.ResponseMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 业务Handler
 * @author markingWang
 * @date 2020/12/4 8:38 下午
 */
@Slf4j
public class OrderServerProcessHandler extends SimpleChannelInboundHandler<RequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestMessage requestMessage) throws Exception {
        Operation operation = requestMessage.getMessageBody();
        OperationResult operationResult = operation.execute();

        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setMessageHeader(requestMessage.getMessageHeader());
        responseMessage.setMessageBody(operationResult);

        if (ctx.channel().isActive() && ctx.channel().isWritable()) {
            ctx.writeAndFlush(responseMessage);
            //ctx.channel().writeAndFlush(responseMessage);
            //区别:ctx.channel().writeAndFlush从尾部往前找,ctx.writeAndFlush 是从当前handler往面找
        } else {
            log.error("not writable now, message dropped");
        }
    }
}
