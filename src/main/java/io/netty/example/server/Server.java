package io.netty.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.server.codec.OrderFrameDecoder;
import io.netty.example.server.codec.OrderFrameEncoder;
import io.netty.example.server.codec.OrderProtocolDecoder;
import io.netty.example.server.codec.OrderProtocolEncoder;
import io.netty.example.server.handler.AuthHandler;
import io.netty.example.server.handler.MetricHandler;
import io.netty.example.server.handler.OrderServerProcessHandler;
import io.netty.example.server.handler.ServerIdleCheckHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import io.netty.handler.ipfilter.RuleBasedIpFilter;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty Server端
 *
 * @author markingWang
 * @date 2020/12/4 7:55 下午
 */
@Slf4j
public class Server {

    @SneakyThrows
    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.option(NioChannelOption.SO_BACKLOG, 1024);
        serverBootstrap.childOption(NioChannelOption.TCP_NODELAY, true);
        serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));

        // thread
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("boss"));
        NioEventLoopGroup workGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("work"));
        UnorderedThreadPoolEventExecutor businessGroup = new UnorderedThreadPoolEventExecutor(10, new DefaultThreadFactory("business"));
        NioEventLoopGroup eventLoopGroupForTrafficShaping = new NioEventLoopGroup(0, new DefaultThreadFactory("TS"));

        try {
            serverBootstrap.group(bossGroup, workGroup);

            // metrics
            MetricHandler metricHandler = new MetricHandler();

            // traffic shaping
            GlobalTrafficShapingHandler globalTrafficShapingHandler = new GlobalTrafficShapingHandler(eventLoopGroupForTrafficShaping, 10 * 1024 * 1024, 10 * 1024 * 1024);

            // ip filter
            IpFilterRule ipRule = new IpSubnetFilterRule("127.1.1.1", 16, IpFilterRuleType.REJECT);
            RuleBasedIpFilter ruleBasedIpFilter = new RuleBasedIpFilter(ipRule);

            // auth
            AuthHandler authHandler = new AuthHandler();

            // ssl
            SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
            log.info("certificate position:" + selfSignedCertificate.certificate().toString());
            SslContext sslContext = SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey()).build();

            // log
            LoggingHandler debugLoggingHandler = new LoggingHandler(LogLevel.DEBUG);
            LoggingHandler infoLoggingHandler = new LoggingHandler(LogLevel.INFO);

            serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();

                    pipeline.addLast("debugLoggingHandler", debugLoggingHandler);

                    // ip白名单规则
                    pipeline.addLast("ipFilter", ruleBasedIpFilter);

                    // 流量整形
                    pipeline.addLast("trafficShapingHandler", globalTrafficShapingHandler);

                    // 监控上报
                    pipeline.addLast("metricHandler", metricHandler);

                    // 空闲检测
                    pipeline.addLast("idleHandler", new ServerIdleCheckHandler());

                    // 安全连接
                    pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));

                    pipeline.addLast("frameDecoder", new OrderFrameDecoder());
                    pipeline.addLast("frameEncoder", new OrderFrameEncoder());

                    pipeline.addLast("protocolDecoder", new OrderProtocolDecoder());
                    pipeline.addLast("protocolEncoder", new OrderProtocolEncoder());

                    pipeline.addLast("infoLoggingHandler", infoLoggingHandler);

                    // 按照次数进行flush，增加吞吐量
                    pipeline.addLast("flushEnhanceHandler", new FlushConsolidationHandler(10, true));

                    // 认证授权
                    pipeline.addLast("authHandler", authHandler);

                    // 业务处理
                    pipeline.addLast(businessGroup, new OrderServerProcessHandler());
                }
            });

            ChannelFuture channelFuture = serverBootstrap.bind(8090).sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            businessGroup.shutdownGracefully();
            eventLoopGroupForTrafficShaping.shutdownGracefully();
        }
    }

}
