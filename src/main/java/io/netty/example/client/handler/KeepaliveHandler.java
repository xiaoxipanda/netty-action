package io.netty.example.client.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.example.common.RequestMessage;
import io.netty.example.common.keepalive.KeepaliveOperation;
import io.netty.example.util.IdUtil;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于事件Keepalive请求
 * 心跳有的时候是为了监测已断掉的连接，而有的时候为了不断开连接而做心跳用来证明自己还在
 * @author markingWang
 * @date 2020/12/4 9:34 下午
 */
@Slf4j
@ChannelHandler.Sharable
public class KeepaliveHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT){
            log.info("write idle happen. so need to send keepalive to keep connection not closed by server");
            KeepaliveOperation keepaliveOperation = new KeepaliveOperation();
            RequestMessage requestMessage = new RequestMessage(IdUtil.nextId(), keepaliveOperation);
            ctx.writeAndFlush(requestMessage);
        }
        super.userEventTriggered(ctx, evt);
    }
}
