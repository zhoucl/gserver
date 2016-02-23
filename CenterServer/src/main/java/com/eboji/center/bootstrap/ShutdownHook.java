package com.eboji.center.bootstrap;

import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

public class ShutdownHook {
	private static final Logger logger = LoggerFactory.getLogger(ShutdownHook.class);
	
	public static void doShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				logger.info("CenterServer is stopping...");
				
				dofinish();
				
				logger.info("CenterServer has been stopped.");
			}
		});
	}
	
	private static void dofinish() {
		logger.info("finish closing all work!");
	}
}