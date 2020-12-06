package io.netty.example.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 空闲检测Handler
 * @author markingWang
 * @date 2020/12/4 8:33 下午
 */
@Slf4j
public class ServerIdleCheckHandler extends IdleStateHandler {
    public ServerIdleCheckHandler() {
        // 每10s检查是否有读数据传输，没有就断开客户端连接
        super(10, 0, 0);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (evt == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT) {
            log.info("idle check happen, so close the connection");
            ctx.close();
            return;
        }

        super.channelIdle(ctx, evt);
    }
}
