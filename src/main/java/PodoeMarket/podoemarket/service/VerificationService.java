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
public class VerificationService {
    private final StringRedisTemplate redisTemplate;

    //지정된 키(key)에 해당하는 데이터를 Redis에서 가져오는 메서드
    public String getData(String key){
        try{
            ValueOperations<String,String> valueOperations=redisTemplate.opsForValue();
            return valueOperations.get(key);
        } catch(Exception e){
            return "redis get 문제 발생";
        }
    }

    //지정된 키(key)에 값을 저장하고, 지정된 시간(duration) 후에 데이터가 만료되도록 설정하는 메서드
    public void setDataExpire(String key,String value,long duration){
        try{
            ValueOperations<String,String> valueOperations=redisTemplate.opsForValue();
            Duration expireDuration=Duration.ofSeconds(duration);
            valueOperations.set(key,value,expireDuration);
        } catch(Exception e) {
            log.warn("redis set 문제 발생:{}", e);
        }
    }

    //지정된 키(key)에 해당하는 데이터를 Redis에서 삭제하는 메서드
    public void deleteData(String key){
        redisTemplate.delete(key);
    }
}
