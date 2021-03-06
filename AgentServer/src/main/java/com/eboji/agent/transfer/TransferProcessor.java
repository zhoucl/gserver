package com.eboji.agent.transfer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.eboji.agent.server.AgentServerClientMap;
import com.eboji.agent.util.ConfigUtil;
import com.eboji.commons.Constant;
import com.eboji.commons.hook.ConnectionBuilder;
import com.eboji.commons.msg.BaseMsg;
import com.eboji.commons.msg.JoinRoomMsg;
import com.eboji.commons.msg.JoinRoomNoMemMsg;
import com.eboji.commons.msg.JoinRoomResMsg;
import com.eboji.commons.msg.ReJoinRoomMsg;
import com.eboji.commons.util.CommonUtil;
import com.eboji.commons.util.memcached.MemCacheClient;

import io.netty.channel.socket.SocketChannel;

public class TransferProcessor {
	private static final Logger logger = LoggerFactory.getLogger(TransferProcessor.class);
	
	private static Map<String, SocketChannel> socketChannelMap = new ConcurrentHashMap<String, SocketChannel>();
	
	private static Map<String, Set<String>> serviceMap = new ConcurrentHashMap<String, Set<String>>();
	
	public static Map<String, Set<String>> gameMap = new ConcurrentHashMap<String, Set<String>>();
	
	public static Map<Integer, String> gameRouteMap = new ConcurrentHashMap<Integer, String>();
	
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
	
	public static Map<Integer, String> getGameRouteMap() {
		return gameRouteMap;
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
		
		Iterator<String> iter1 = gameMap.keySet().iterator();
		while(iter1.hasNext()) {
			String key = iter1.next();
			if(gameMap.get(key).contains(remoteAddress)) {
				gameMap.get(key).remove(remoteAddress);
			}
		}
		
		Iterator<Integer> iter2 = gameRouteMap.keySet().iterator();
		while(iter1.hasNext()) {
			Integer key = iter2.next();
			if(gameRouteMap.get(key).contains(remoteAddress)) {
				gameMap.get(key).remove(remoteAddress);
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
						Integer.parseInt(addresses[1]), addresses[0], new TransferHandler());
				socketChannelMap.put(serviceaddress, client.getSocketChannel());
				Set<String> sets = serviceMap.get(entry.getKey());
				if(sets == null) {
					sets = new HashSet<String>();
				}
				
				sets.add(serviceaddress);
				serviceMap.put(entry.getKey(), sets);
			}
		}
		
		CommonUtil.printConnections(getServiceMap());
	}
	
	protected static Map<String, Set<String>> findNeedInitial(Map<String, Set<String>> _serviceMap) {
		Map<String, Set<String>> needSets = new ConcurrentHashMap<String, Set<String>>();
		
		boolean isIncludeKey = false;
		for(Map.Entry<String, Set<String>> entry : _serviceMap.entrySet()) {
			//AgentServer-->LoginServer|AgentServer-->GameServer|AgentServer-->IMServer
			if(entry.getKey().contains(Constant.SRV_LOGIN) 
					|| entry.getKey().contains(Constant.SRV_GAME) 
					|| entry.getKey().contains(Constant.SRV_IM)) {
				Set<String> centerService = entry.getValue();
				for(Map.Entry<String, Set<String>> innerEntry : serviceMap.entrySet()) {
					if(entry.getKey().equals(innerEntry.getKey())) {
						isIncludeKey = true;
						Set<String> services = innerEntry.getValue();
						boolean flag = false;
						for(String cService : centerService) {
							if(!cService.contains(Constant.STR_UNDERLINE)) {	//非游戏服务注册连接
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
						
						flag = false;
						for(String cService : centerService) {
							if(cService.contains(Constant.STR_UNDERLINE)) {	//游戏服务注册连接
								String gameId = cService.split(Constant.STR_UNDERLINE)[0];
								String realCService = cService.split(Constant.STR_UNDERLINE)[1];
								
								for(String service : services) {
									if(realCService.equals(service)) {
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
								
								if(gameMap.get(gameId) == null || gameMap.get(gameId).size() == 0) {
									Set<String> address = new HashSet<String>();
									address.add(realCService);
									gameMap.put(gameId, address);
								} else {
									gameMap.get(gameId).add(realCService);
								}
							}
						}
					}
				}
				
				if(!isIncludeKey) {
					for(String cService : centerService) {
						if(!cService.contains(Constant.STR_UNDERLINE)) {	//非游戏服务注册连接
							if(needSets.get(entry.getKey()) == null) {
								Set<String> sets = new HashSet<String>();
								sets.add(cService);
								needSets.put(entry.getKey(), sets);
							} else {
								needSets.get(entry.getKey()).add(cService);
							}
						} else if(cService.contains(Constant.STR_UNDERLINE)) {	//游戏服务注册连接
							String gameId = cService.split(Constant.STR_UNDERLINE)[0];
							String realCService = cService.split(Constant.STR_UNDERLINE)[1];
							
							if(needSets.get(entry.getKey()) == null) {
								Set<String> sets = new HashSet<String>();
								sets.add(realCService);
								needSets.put(entry.getKey(), sets);
							} else {
								needSets.get(entry.getKey()).add(realCService);
							}
							
							if(gameMap.get(gameId) == null || gameMap.get(gameId).size() == 0) {
								Set<String> address = new HashSet<String>();
								address.add(realCService);
								gameMap.put(gameId, address);
							} else {
								gameMap.get(gameId).add(realCService);
							}
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
	
	public static void login(Object obj) {
		Set<String> serviceSet = serviceMap.get(Constant.SRV_LOGIN);
		if(serviceSet != null && serviceSet.size() > 0) {
			int index = 0;
			if(serviceSet.size() > 1) {
				Random rand = new Random(System.currentTimeMillis());
				index = rand.nextInt(serviceSet.size());
			}
				
			socketChannelMap.get(serviceSet.toArray()[index]).writeAndFlush(obj);
		}
	}
	
	public static void processMj(BaseMsg obj) {
		String gameHost = null;
		Integer roomNo = obj.getRoomNo();
		Set<String> serviceSet = gameMap.get(((JSONObject)JSONObject.toJSON(obj)).get("gid"));
		
		if(obj instanceof ReJoinRoomMsg) {
			ReJoinRoomMsg reJoinMsg = (ReJoinRoomMsg)obj;
			JoinRoomMsg msg = new JoinRoomMsg();
			msg.setCid(reJoinMsg.getCid());
			msg.setGid(reJoinMsg.getGid());
			msg.setRoomNo(reJoinMsg.getRoomNo());
			msg.setUid(reJoinMsg.getUid());
			socketChannelMap.get(reJoinMsg.getGameHost()).writeAndFlush(msg);
		} else {
			if(roomNo != null && roomNo != 0) {
				MemCacheClient client = ConfigUtil.getClient();
				if(client != null) {
					Object roomObj = null; 
					roomObj = client.get(Constant.MEM_ROOM_PREFIX + obj.getRoomNo());
					if(roomObj != null) {
						gameHost = String.valueOf(roomObj);
						if(serviceSet.contains(gameHost)) {
							socketChannelMap.get(gameHost).writeAndFlush(obj);
						} else {
							//加入房间异常失败
							JoinRoomResMsg res = obtainJRMsg(obj, -2);
							AgentServerClientMap.get(obj.getUid()).writeAndFlush(res);
						}
					} else {
						//房间号不存在，加入房间失败
						//JoinRoomResMsg res = obtainJRMsg(obj, 0);
						//AgentServerClientMap.get(obj.getUid()).writeAndFlush(res);
						logger.error("Memcache Get [" + Constant.MEM_ROOM_PREFIX + obj.getRoomNo() + "]Exception!!!");
						//通过数据库查找创建房间的游戏服务
						if(serviceSet != null && serviceSet.size() > 0) {
							int index = getRandom(serviceSet.size());
							JoinRoomNoMemMsg msg = obtainJRNMsg(obj);
							socketChannelMap.get(serviceSet.toArray()[index]).writeAndFlush(msg);
						} else {
							//加入房间异常失败
							JoinRoomResMsg res = obtainJRMsg(obj, -2);
							AgentServerClientMap.get(obj.getUid()).writeAndFlush(res);
						}
					}
				} else {
					logger.error("Memcache was not exist!!!");
					//通过数据库查找创建房间的游戏服务
					if(serviceSet != null && serviceSet.size() > 0) {
						int index = getRandom(serviceSet.size());
						JoinRoomNoMemMsg msg = obtainJRNMsg(obj);
						socketChannelMap.get(serviceSet.toArray()[index]).writeAndFlush(msg);
					} else {
						//加入房间异常失败
						JoinRoomResMsg res = obtainJRMsg(obj, -2);
						AgentServerClientMap.get(obj.getUid()).writeAndFlush(res);
					}
				}
			} else {
				//创建房间请求
				if(serviceSet != null && serviceSet.size() > 0) {
					int index = getRandom(serviceSet.size());
					socketChannelMap.get(serviceSet.toArray()[index]).writeAndFlush(obj);
				}
			}
		}
	}
	
	protected static int getRandom(int size) {
		int ret = 0;
		if(size > 1) {
			ret = new Random().nextInt(size - 1);
		}
		
		return ret;
	}
	
	protected static JoinRoomResMsg obtainJRMsg(BaseMsg obj, int status) {
		JoinRoomResMsg res = new JoinRoomResMsg();
		res.setCid(obj.getCid());
		res.setGid(obj.getGid());
		res.setRoomNo(obj.getRoomNo());
		res.setUid(obj.getUid());
		res.setStatus(status);	//无法到达请求服务失败
		return res;
	}
	
	protected static JoinRoomNoMemMsg obtainJRNMsg(BaseMsg obj) {
		JoinRoomNoMemMsg res = new JoinRoomNoMemMsg();
		res.setCid(obj.getCid());
		res.setGid(obj.getGid());
		res.setRas(obj.getRas());
		res.setRoomNo(obj.getRoomNo());
		res.setUid(obj.getUid());
		return res;
	}
}
