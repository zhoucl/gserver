package com.eboji.data.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eboji.commons.msg.BaseMsg;
import com.eboji.commons.type.MsgType;
import com.eboji.data.service.DataService;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;

public class DataServerHandler extends SimpleChannelInboundHandler<BaseMsg> {
	private static final Logger logger = LoggerFactory.getLogger(DataServerHandler.class);
	
	protected DataServerProcessor dataProcessor = null;
	
	public DataServerHandler(int poolSize, DataService dataService) {
		this.dataProcessor = new DataServerProcessor(dataService, poolSize);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		DataServerClientMap.remove((SocketChannel)ctx.channel());
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		String clientId = ctx.channel().remoteAddress().toString();
		DataServerClientMap.put(clientId, (SocketChannel)ctx.channel());
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, BaseMsg msg)
			throws Exception {
		try {
			MsgType type = msg.getT();
			
			switch (type) {
			case CONN:
			case PING:
				break;

			case LOGIN:
			case CREATEROOM:
			case JOINROOM:
			case JOINROOMNOMEM:
				dataProcessor.process(msg, ctx.channel().remoteAddress().toString());
				break;
				
			default:
				break;
			}
			
		} catch (Exception e) {
			logger.error("request param is not json object, request msg is:\n" + msg, e);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		logger.info("remote address: " + ctx.channel().remoteAddress() + ", " + cause.getMessage()); 
	}
}
