package com.ssafy.worldy.model.game.repo;

import com.ssafy.worldy.model.game.dto.GameRoom;
import com.ssafy.worldy.model.game.service.RedisPublisher;
import com.ssafy.worldy.model.game.service.RedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

 /**
 * 게임방 정보가 초기화 되지 않도록 생성 시 Redis Hash에 저장
 * 게임방 정보를 조회할 때는 Redis Hash에 저장된 데이터 사용
 * 게임방 입장 시에는 게임방 id로 Redis의 Topic을 조회하여 pub/sub 메시지 리스너와 연동
 **/

@RequiredArgsConstructor
@Repository
public class GameRoomRepo {

    // Topic에 발행되는 메시지를 처리할 Listner
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final RedisSubscriber redisSubscriber;
    private static final String GAME_ROOMS = "GAME_ROOM";

    private final RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, GameRoom> opsHashGameRoom;
    // 메시지를 발행하기 위한 redis topic 정보. 서버별로 채팅방에 매치되는 topic정보를 Map에 넣어 roomId로 찾을수 있도록 한다.
    private Map<String, ChannelTopic> topics;

    @PostConstruct
    private void init() {
        opsHashGameRoom = redisTemplate.opsForHash();
        topics = new HashMap<>();
    }

    /**
     * 모든 게임방 조회
     **/
    public List<GameRoom> findAllRoom() {
        return opsHashGameRoom.values(GAME_ROOMS);
    }

     /**
      * 채팅방 생성 : 서버간 채팅방 공유를 위해 redis hash에 저장
      */
    public GameRoom createGameRoom(String name) {
        GameRoom gameRoom = GameRoom.create(name);
        opsHashGameRoom.put(GAME_ROOMS, gameRoom.getRoomId(), gameRoom);
        return gameRoom;
    }

     /**
      * 채팅방 입장 : redis에 topic을 만들고 pub/sub 통신을 하기 위해 리스너를 설정
      */
     public void enterGameRoom(String roomId) {
         ChannelTopic topic = topics.get(roomId);

         if(topic==null) {
             topic = new ChannelTopic(roomId);
             redisMessageListenerContainer.addMessageListener(redisSubscriber, topic);
             topics.put(roomId, topic);
         }
     }
     public ChannelTopic getTopic(String roomId) {
         return topics.get(roomId);
     }
}
