package com.eboji.game.server.transfer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.eboji.commons.Constant;
import com.eboji.commons.hook.ConnectionBuilder;
import com.eboji.commons.util.CommonUtil;

import io.netty.channel.socket.SocketChannel;

public class TransferProcessor {
	private static Map<String, SocketChannel> socketChannelMap = new ConcurrentHashMap<String, SocketChannel>();
	private static Map<String, Set<String>> serviceMap = new ConcurrentHashMap<String, Set<String>>();
	
	public static Map<String, SocketChannel> getSocketChannelMap() {
		return socketChannelMap;
	}

	public static void setSocketChannelMap(
			Map<String, SocketChannel> socketChannelMap) {
		TransferProcessor.socketChannelMap = socketChannelMap;
	}

	public static Map<String, Set<String>> getServiceMap() {
		return serviceMap;
	}

	public static void setServiceMap(Map<String, Set<String>> serviceMap) {
		TransferProcessor.serviceMap = serviceMap;
	}
	
	public static void remove(String remoteAddress) {
		socketChannelMap.remove(remoteAddress);
		Iterator<String> iter = serviceMap.keySet().iterator();
		while(iter.hasNext()) {
			String key = iter.next();
			if(serviceMap.get(key).contains(remoteAddress)) {
				serviceMap.get(key).remove(remoteAddress);
				break;
			}
		}
	}
	
	public static void parse(Map<String, Set<String>> _serviceMap) {
		Map<String, Set<String>> needSets = findNeedInitial(_serviceMap);
		
		//初始化
		for(Map.Entry<String, Set<String>> entry : needSets.entrySet()) {
			Set<String> entrysets = entry.getValue();
			for(String serviceaddress : entrysets) {
				String[] addresses = serviceaddress.split(Constant.STR_COLON);
				ConnectionBuilder client = new ConnectionBuilder(
						Integer.parseInt(addresses[1]), addresses[0],
						new TransferHandler());
				TransferProcessor.getSocketChannelMap().put(serviceaddress, client.getSocketChannel());
				Set<String> sets = TransferProcessor.getServiceMap().get(entry.getKey());
				if(sets == null) {
					sets = new HashSet<String>();
				}
				
				sets.add(serviceaddress);
				TransferProcessor.getServiceMap().put(entry.getKey(), sets);
			}
		}
		CommonUtil.printConnections(getServiceMap());
	}
	
	protected static Map<String, Set<String>> findNeedInitial(Map<String, Set<String>> _serviceMap) {
		Map<String, Set<String>> needSets = new ConcurrentHashMap<String, Set<String>>();
		
		boolean isIncludeKey = false;
		for(Map.Entry<String, Set<String>> entry : _serviceMap.entrySet()) {
			if(entry.getKey().contains(Constant.SRV_DATA) 
					|| entry.getKey().contains(Constant.SRV_IM)) {
				Set<String> centerService = entry.getValue();
				for(Map.Entry<String, Set<String>> innerEntry : serviceMap.entrySet()) {
					if(entry.getKey().equals(innerEntry.getKey())) {
						isIncludeKey = true;
						Set<String> services = innerEntry.getValue();
						boolean flag = false;
						for(String cService : centerService) {
							for(String service : services) {
								if(cService.equals(service)) {
									flag = true;
									break;
								}
							}
							
							if(!flag) {
								if(needSets.get(entry.getKey()) == null) {
									Set<String> sets = new HashSet<String>();
									sets.add(cService);
									needSets.put(entry.getKey(), sets);
								} else {
									needSets.get(entry.getKey()).add(cService);
								}
							}
						}
					}
				}
				
				if(!isIncludeKey) {
					for(String cService : centerService) {
						if(needSets.get(entry.getKey()) == null) {
							Set<String> sets = new HashSet<String>();
							sets.add(cService);
							needSets.put(entry.getKey(), sets);
						} else {
							needSets.get(entry.getKey()).add(cService);
						}
					}
				}
			}
		}
		
		return needSets;
	}

	public static void write(Object obj) {
		for(Map.Entry<String, SocketChannel> entry : socketChannelMap.entrySet()) {
			entry.getValue().writeAndFlush(obj);
		}
	}
	
	public static void persist(Object obj) {
		Set<String> serviceSet = serviceMap.get(Constant.SRV_DATA);
		if(serviceSet != null && serviceSet.size() > 0) {
			int index = 0;
			if(serviceSet.size() > 1) {
				index = new Random().nextInt(serviceSet.size());
			}
			socketChannelMap.get(serviceSet.toArray()[index]).writeAndFlush(obj);
		}
	}
}
