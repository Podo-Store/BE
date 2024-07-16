package PodoeMarket.podoemarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisUtil {
    private final StringRedisTemplate redisTemplate;//Redis에 접근하기 위한 Spring의 Redis 템플릿 클래스

    public String getData(String key){//지정된 키(key)에 해당하는 데이터를 Redis에서 가져오는 메서드
        try{
            ValueOperations<String,String> valueOperations=redisTemplate.opsForValue();
            return valueOperations.get(key);
        } catch(Exception e){
            return "redis get 문제 발생";
        }
    }

    public void setData(String key,String value){//지정된 키(key)에 값을 저장하는 메서드
        ValueOperations<String,String> valueOperations=redisTemplate.opsForValue();
        valueOperations.set(key,value);
    }

    public void setDataExpire(String key,String value,long duration){//지정된 키(key)에 값을 저장하고, 지정된 시간(duration) 후에 데이터가 만료되도록 설정하는 메서드
        try{
            ValueOperations<String,String> valueOperations=redisTemplate.opsForValue();
            Duration expireDuration=Duration.ofSeconds(duration);
            valueOperations.set(key,value,expireDuration);
        } catch(Exception e) {
            log.warn("redis set 문제 발생:{}", e);
        }
    }

    public void deleteData(String key){//지정된 키(key)에 해당하는 데이터를 Redis에서 삭제하는 메서드
        redisTemplate.delete(key);
    }
}
