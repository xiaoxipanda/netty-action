package io.netty.example.server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.example.common.Operation;
import io.netty.example.common.RequestMessage;
import io.netty.example.common.auth.AuthOperation;
import io.netty.example.common.auth.AuthOperationResult;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证授权Handler
 *
 * @author markingWang
 * @date 2020/12/4 8:19 下午
 */
@Slf4j
@ChannelHandler.Sharable
public class AuthHandler extends SimpleChannelInboundHandler<RequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestMessage msg) throws Exception {
        Operation messageBody = msg.getMessageBody();
        try {
            if (messageBody instanceof AuthOperation) {
                AuthOperation authOperation = (AuthOperation) messageBody;
                AuthOperationResult authOperationResult = authOperation.execute();
                if (authOperationResult.isPassAuth()) {
                    log.info("授权通过");
                } else {
                    log.error("授权失败");
                    ctx.close();
                }
            } else {
                log.error("首次请求非授权消息");
                ctx.close();
            }
        } catch (Exception e) {
            log.error("授权异常", e);
            ctx.close();
        } finally {
            ctx.pipeline().remove(this);
        }
    }
}
