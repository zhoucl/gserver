package com.eboji.game.logic.mj.zzmj;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.eboji.commons.msg.BaseMsg;
import com.eboji.game.logic.mj.GameBaseLogic;

public class GameZZMJLogic extends GameBaseLogic {
	//麻将的张数
	public static final Integer ORG_MJ_MAX = 108;
	
	//完整的麻将静态数据
	public static final Byte[] MJDATA = {
		0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,		//万(1-9)
		0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19,		//筒(17-25)
		0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29,		//条(33-41)
		
		0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,		//万
		0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19,		//筒
		0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29,		//条
		
		0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,		//万
		0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19,		//筒
		0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29,		//条
		
		0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,		//万
		0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19,		//筒
		0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29		//条
	};

	protected void randMj(Byte[] mj, Integer mjMaxCount, Integer randCount) {
		Integer index = 0;
		Byte temp;
		Random rand = new Random(System.currentTimeMillis());
		
		for(int r = 0; r < randCount; r++) {
			for(int i = 0; i < mjMaxCount; i++) {
				index = rand.nextInt(mjMaxCount);
				if(i != index) {
					temp = mj[i];
					mj[i] = mj[index];
					mj[index] = temp;
				}
			}
		}
		
		for(int i = 1; i < mjMaxCount+1; i++) {
			if(i % 13 == 0 && i > 0) {
				System.out.println(mj[i-1]);
			} else {
				System.out.print(mj[i-1] + "\t");
			}
		}
		
		System.out.println();
	}

	public void sortMj(Byte[] mjHand, int mjHandCount) {
		List<Byte> mjList  = Arrays.asList(mjHand);
		mjList.sort(new ByteComparator());
		mjList.toArray(mjHand);
	}

	@Override
	public void start() {
		//复制麻将数据
		Byte[] mj = MJDATA.clone();
		
		//随机洗牌次数
		Random random = new Random(System.currentTimeMillis());
		int r = random.nextInt(3);
		
		//按次数随机洗牌
		randMj(mj, ORG_MJ_MAX, r);
		
		//给各个玩家发牌
		
	}
	
	@Override
	public void process(BaseMsg msg) {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		//复制新牌
		Byte[] mj = MJDATA.clone();
		
		GameZZMJLogic logic = new GameZZMJLogic();
		
		//重新洗牌
		Random rand = new Random(System.currentTimeMillis());
		int r = rand.nextInt(3);
		logic.randMj(mj, ORG_MJ_MAX, r);
		
		//各玩家获取牌，并且判断是否胡牌，杠牌等，信息分别发送
		for(int i = 0; i < 4; i++) {
			Byte[] mjHand = null;
			if(i == 0) {
				mjHand = new Byte[14];
				for(int j = 0; j < 14; j++) {
					mjHand[j] = mj[j];
				}
			} else {
				mjHand = new Byte[13];
				for(int j = 0; j < 13; j++) {
					mjHand[j] = mj[14 + (i - 1) * 13 + j];
				}
			}
			logic.sortMj(mjHand, 13);
			
			ZZMjData mjData = new ZZMjData();
			mjData.setMjHand(mjHand);
			
			System.out.println("Player " + i + ":\n" + Arrays.toString(mjData.getMjHand()));
		}
	}
}

class ByteComparator implements Comparator<Byte> {
	@Override
	public int compare(Byte o1, Byte o2) {
		if(o1 > o2) {
			return 1;
		} else if(o1 == o2) {
			return 0;
		} else {
			return -1;
		}
	}
}
