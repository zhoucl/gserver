package com.eboji.login.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eboji.commons.Constant;
import com.eboji.commons.codec.MsgDecoder;
import com.eboji.commons.codec.MsgEncoder;
import com.eboji.commons.hook.ConnectionFactory;
import com.eboji.commons.jetty.JettyServerFactory;
import com.eboji.login.bootstrap.Daemon;
import com.eboji.login.server.transfer.TransferHandler;
import com.eboji.login.server.transfer.TransferProcessor;
import com.eboji.login.servlet.ServiceServlet;
import com.eboji.login.util.ConfigUtil;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class LoginServerListener {
	private static final Logger logger = LoggerFactory.getLogger(LoginServerListener.class);
	
	private int port;
	
	public LoginServerListener(int port) throws Exception {
		this.port = port;
		
		bind();
	}
	
	protected void bind() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup);
			bootstrap.channel(NioServerSocketChannel.class);
			bootstrap.option(ChannelOption.SO_BACKLOG, 128);
			bootstrap.option(ChannelOption.TCP_NODELAY, true);
			bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
			
			bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipe = ch.pipeline();
					pipe.addLast(new MsgEncoder());
					pipe.addLast(new MsgDecoder());
					pipe.addLast(new LoginServerHandler());
				}
			});
			
			ChannelFuture f = bootstrap.bind(port).sync();
			if(f.isSuccess()) {
				logger.info("Login Server listened in port: " + this.port + " has been started.");
				
				ConnectionFactory.registerServiceToCenterServer(ConfigUtil.getProps("centerserver"), 
						new TransferHandler(), Daemon.getInstance().getPort(), 
						TransferProcessor.getSocketChannelMap(), 
						TransferProcessor.getServiceMap(), Constant.SRV_LOGIN);
				
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							JettyServerFactory.getInstance()
								.port(Daemon.getInstance().getPort() + 10000)
								.addServlet(new ServiceServlet())
								.build().start();
						} catch (Exception e) {
							logger.warn("jetty server start failed!");
						}
					}
				}).start();
			}
			
			f.channel().closeFuture().sync();
		} catch (Exception e) {
			throw e;
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
}
