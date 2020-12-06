package io.netty.example.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.client.codec.*;
import io.netty.example.common.order.OrderOperation;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.internal.UnstableApi;
import lombok.SneakyThrows;

/**
 * This class hadn't add auth or do other improvements. so need to refer {@link ClientV0}
 *
 * @author markingWang
 * @date 2020/12/6 3:55 下午
 */
@UnstableApi
public class ClientV1 {

    @SneakyThrows
    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);

        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            bootstrap.group(group);

            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
            sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            SslContext sslContext = sslContextBuilder.build();

            bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();

                    pipeline.addLast("sslHandler", sslContext.newHandler(ch.alloc()));

                    pipeline.addLast("orderFrameDecoder", new OrderFrameDecoder());
                    pipeline.addLast("orderFrameEncoder", new OrderFrameEncoder());

                    pipeline.addLast("orderProtocolEncoder", new OrderProtocolEncoder());
                    pipeline.addLast("orderProtocolDecoder", new OrderProtocolDecoder());

                    pipeline.addLast("operationToRequestMessageEncoder", new OperationToRequestMessageEncoder());

                    pipeline.addLast("loggingHandler", new LoggingHandler(LogLevel.INFO));
                }
            });

            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8090);

            channelFuture.sync();

            OrderOperation orderOperation = new OrderOperation(1001, "tudou");

            channelFuture.channel().writeAndFlush(orderOperation);

            channelFuture.channel().closeFuture().sync();

        } finally {
            group.shutdownGracefully();
        }

    }
}
