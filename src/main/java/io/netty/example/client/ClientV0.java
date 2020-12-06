package io.netty.example.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.client.codec.OrderFrameDecoder;
import io.netty.example.client.codec.OrderFrameEncoder;
import io.netty.example.client.codec.OrderProtocolDecoder;
import io.netty.example.client.codec.OrderProtocolEncoder;
import io.netty.example.client.handler.ClientIdleCheckHandler;
import io.netty.example.client.handler.KeepaliveHandler;
import io.netty.example.common.RequestMessage;
import io.netty.example.common.auth.AuthOperation;
import io.netty.example.common.order.OrderOperation;
import io.netty.example.util.IdUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.SneakyThrows;

/**
 * ClientV0版本
 *
 * @author markingWang
 * @date 2020/12/6 3:34 下午
 */
public class ClientV0 {

    @SneakyThrows
    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);

        // 设置连接超时事件
        bootstrap.option(NioChannelOption.CONNECT_TIMEOUT_MILLIS, 10 * 1000);

        NioEventLoopGroup group = new NioEventLoopGroup();

        bootstrap.group(group);

        try {
            KeepaliveHandler keepaliveHandler = new KeepaliveHandler();
            LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);

            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();

            // 先直接信任自签证书
            // 安装证书，安装方法参考课程，执行命令参考resources/ssl.txt里面
            sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);

            SslContext sslContext = sslContextBuilder.build();

            bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();

                    pipeline.addLast("clientIdleCheckHandler", new ClientIdleCheckHandler());

                    pipeline.addLast("sslHandler", sslContext.newHandler(ch.alloc()));

                    pipeline.addLast("orderFrameDecoder", new OrderFrameDecoder());
                    pipeline.addLast("orderFrameEncoder", new OrderFrameEncoder());

                    pipeline.addLast("orderProtocolEncoder", new OrderProtocolEncoder());
                    pipeline.addLast("orderProtocolDecoder", new OrderProtocolDecoder());

                    pipeline.addLast("loggingHandler", loggingHandler);

                    pipeline.addLast("keepalive", keepaliveHandler);

                }
            });


            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8090);
            channelFuture.sync();

            AuthOperation authOperation = new AuthOperation("admin", "password");

            channelFuture.channel().writeAndFlush(new RequestMessage(IdUtil.nextId(), authOperation));

            RequestMessage requestMessage = new RequestMessage(IdUtil.nextId(), new OrderOperation(1001, "tudou"));

            channelFuture.channel().writeAndFlush(requestMessage);

            channelFuture.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
