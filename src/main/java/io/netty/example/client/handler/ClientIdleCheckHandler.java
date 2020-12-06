package io.netty.example.client.handler;

import io.netty.handler.timeout.IdleStateHandler;

/**
 * client idle check
 * @author markingWang
 * @date 2020/12/6 3:47 下午
 */
public class ClientIdleCheckHandler extends IdleStateHandler {
    public ClientIdleCheckHandler() {
        super(0, 5, 0);
    }
}
