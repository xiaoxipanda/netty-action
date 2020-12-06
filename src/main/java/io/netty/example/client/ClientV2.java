package io.netty.example.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.client.codec.*;
import io.netty.example.client.handler.dispatcher.OperationResultFuture;
import io.netty.example.client.handler.dispatcher.RequestPendingCenter;
import io.netty.example.client.handler.dispatcher.ResponseDispatcherHandler;
import io.netty.example.common.OperationResult;
import io.netty.example.common.RequestMessage;
import io.netty.example.common.order.OrderOperation;
import io.netty.example.util.IdUtil;
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
 * @date 2020/12/6 4:03 下午
 */
@UnstableApi
public class ClientV2 {

    @SneakyThrows
    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);

        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            bootstrap.group(group);

            RequestPendingCenter requestPendingCenter = new RequestPendingCenter();

            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
            sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            SslContext sslContext = sslContextBuilder.build();

            bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();

                    pipeline.addLast("sslHandler", sslContext.newHandler(ch.alloc()));

                    pipeline.addLast("OrderFrameDecoder", new OrderFrameDecoder());
                    pipeline.addLast("orderFrameEncoder", new OrderFrameEncoder());
                    pipeline.addLast("OrderProtocolEncoder", new OrderProtocolEncoder());
                    pipeline.addLast("OrderProtocolDecoder", new OrderProtocolDecoder());

                    pipeline.addLast("responseDispatcherHandler", new ResponseDispatcherHandler(requestPendingCenter));

                    pipeline.addLast("operationToRequestMessageEncoder", new OperationToRequestMessageEncoder());

                    pipeline.addLast("loggingHandler", new LoggingHandler(LogLevel.INFO));
                }
            });


            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8090);

            channelFuture.sync();

            long streamId = IdUtil.nextId();

            RequestMessage requestMessage = new RequestMessage(
                    streamId, new OrderOperation(1001, "tudou"));

            OperationResultFuture operationResultFuture = new OperationResultFuture();

            requestPendingCenter.add(streamId, operationResultFuture);

            channelFuture.channel().writeAndFlush(requestMessage);

            OperationResult operationResult = operationResultFuture.get();

            System.out.println(operationResult);

            channelFuture.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
