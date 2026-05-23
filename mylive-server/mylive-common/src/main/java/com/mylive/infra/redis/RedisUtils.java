package com.mylive.infra.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisUtils {

    private final StringRedisTemplate strRedis;
    private final ObjectMapper objectMapper;

    /* ------------------------------------------------------------------------
     * Common
     * ------------------------------------------------------------------------ */

    public void delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        try {
            strRedis.delete(Arrays.asList(keys));
        } catch (Exception e) {
            log.error("redis delete failed, keys={}", Arrays.toString(keys), e);
        }
    }

    public boolean expire(String key, Duration duration) {
        if (!StringUtils.hasText(key) || duration == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(strRedis.expire(key, duration));
        } catch (Exception e) {
            log.error("redis expire failed, key={}", key, e);
            return false;
        }
    }

    public Long ttl(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        try {
            return strRedis.getExpire(key);
        } catch (Exception e) {
            log.error("redis ttl failed, key={}", key, e);
            return null;
        }
    }

    /* ------------------------------------------------------------------------
     * String / Object
     * 约定：
     * 1. Redis 中不保存 Java null 值
     * 2. set(key, null) 等价于 delete(key)
     * ------------------------------------------------------------------------ */

    public String get(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        try {
            return strRedis.opsForValue().get(key);
        } catch (Exception e) {
            log.error("redis get string failed, key={}", key, e);
            return null;
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        if (!StringUtils.hasText(key) || clazz == null) {
            return null;
        }
        try {
            String json = strRedis.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return readObject(json, clazz);
        } catch (Exception e) {
            log.error("redis get(Class) failed, key={}, clazz={}", key, clazz.getName(), e);
            return null;
        }
    }

    public <T> T get(String key, TypeReference<T> typeRef) {
        if (!StringUtils.hasText(key) || typeRef == null) {
            return null;
        }
        try {
            String json = strRedis.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return readObject(json, typeRef);
        } catch (Exception e) {
            log.error("redis get(TypeReference) failed, key={}", key, e);
            return null;
        }
    }

    public boolean set(String key, String value) {
        return setEx(key, value, null);
    }

    public boolean setEx(String key, String value, Duration duration) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        try {
            if (value == null) {
                strRedis.delete(key);
                return true;
            }

            if (duration == null) {
                strRedis.opsForValue().set(key, value);
            } else {
                strRedis.opsForValue().set(key, value, duration);
            }
            return true;
        } catch (Exception e) {
            log.error("redis set string failed, key={}", key, e);
            return false;
        }
    }

    public boolean set(String key, Object value) {
        return setEx(key, value, null);
    }

    public boolean setEx(String key, Object value, Duration duration) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        try {
            if (value == null) {
                strRedis.delete(key);
                return true;
            }

            String json = writeObject(value);
            if (duration == null) {
                strRedis.opsForValue().set(key, json);
            } else {
                strRedis.opsForValue().set(key, json, duration);
            }
            return true;
        } catch (Exception e) {
            log.error("redis set object failed, key={}", key, e);
            return false;
        }
    }

    public boolean lpushStringAll(String key, List<String> values, Duration duration) {
        if (!StringUtils.hasText(key) || CollectionUtils.isEmpty(values)) {
            return false;
        }

        List<String> list = values.stream()
                .filter(StringUtils::hasText)
                .toList();

        if (list.isEmpty()) {
            return false;
        }

        strRedis.opsForList().leftPushAll(key, list);

        if (duration != null) {
            expire(key, duration);
        }

        return true;
    }

    public boolean lpushAll(String key, List<?> values, Duration duration) {
        if (!StringUtils.hasText(key) || CollectionUtils.isEmpty(values)) {
            return false;
        }

        try {
            List<String> jsonList = new ArrayList<>(values.size());
            for (Object value : values) {
                if (value == null) {
                    log.warn("redis lpushAll skipped null element, key={}", key);
                    continue;
                }
                jsonList.add(writeObject(value));
            }

            if (jsonList.isEmpty()) {
                return false;
            }

            strRedis.opsForList().leftPushAll(key, jsonList);
            if (duration != null) {
                expire(key, duration);
            }
            return true;
        } catch (Exception e) {
            log.error("redis lpushAll failed, key={}, size={}", key, values.size(), e);
            return false;
        }
    }

    public long remove(String key, Object value) {
        if (!StringUtils.hasText(key) || value == null) {
            return 0L;
        }
        try {
            String json = writeObject(value);
            Long removed = strRedis.opsForList().remove(key, 1, json);
            return removed == null ? 0L : removed;
        } catch (Exception e) {
            log.error("redis list remove failed, key={}", key, e);
            return 0L;
        }
    }

    public <T> T rpop(String key, Class<T> clazz) {
        if (!StringUtils.hasText(key) || clazz == null) {
            return null;
        }
        try {
            String json = strRedis.opsForList().rightPop(key);
            if (json == null) {
                return null;
            }
            return readObject(json, clazz);
        } catch (Exception e) {
            log.error("redis rpop(Class) failed, key={}, clazz={}", key, clazz.getName(), e);
            return null;
        }
    }

    public String brpop(String key, Duration duration) {
        if (!StringUtils.hasText(key) || duration == null) {
            return null;
        }
        try {
            return strRedis.opsForList().rightPop(key, duration);
        } catch (Exception e) {
            log.error("redis brpop(String) failed, key={}", key, e);
            return null;
        }
    }

    public <T> T brpop(String key, Class<T> clazz, Duration duration) {
        if (!StringUtils.hasText(key) || clazz == null || duration == null) {
            return null;
        }
        try {
            String json = strRedis.opsForList().rightPop(key, duration);
            if (json == null) {
                return null;
            }
            return readObject(json, clazz);
        } catch (Exception e) {
            log.error("redis brpop(Class) failed, key={}, clazz={}", key, clazz.getName(), e);
            return null;
        }
    }

    /* ------------------------------------------------------------------------
     * Counter
     * ------------------------------------------------------------------------ */

    public Long increment(String key, int delta) {
        if (!StringUtils.hasText(key) || delta <= 0) {
            return null;
        }
        try {
            return strRedis.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("redis increment failed, key={}", key, e);
            return null;
        }
    }

    public Long increment(String key) {
        return increment(key, 1);
    }

    /**
     * 首次 incr 时设置过期时间。
     * 注意：这里不是严格原子操作。
     * 如果你用于限流/强一致计数，建议改 Lua。
     */
    public Long incrementAndExpireIfAbsent(String key, Duration duration, long delta) {
        if (!StringUtils.hasText(key)) {
            return null;
        }

        try {
            Long count = strRedis.opsForValue().increment(key, delta);
            Long ttl = strRedis.getExpire(key);
            if (ttl < 0 && duration != null) {
                expire(key, duration);
            }
            return count;
        } catch (Exception e) {
            log.error(
                    "redis incrementAndExpireIfAbsent failed, key={}, delta={}",
                    key, delta, e
            );
            return null;
        }
    }

    /* ------------------------------------------------------------------------
     * Set
     * ------------------------------------------------------------------------ */

    public Long sAdd(String key, String... values) {
        if (!StringUtils.hasText(key) || values == null || values.length == 0) {
            return null;
        }

        try {
            return strRedis.opsForSet().add(key, values);
        } catch (Exception e) {
            log.error("redis sAdd failed, key={}", key, e);
            return null;
        }
    }

    public Set<String> sMembers(String key) {
        if (!StringUtils.hasText(key)) {
            return Collections.emptySet();
        }

        try {
            Set<String> members = strRedis.opsForSet().members(key);
            return members != null ? members : Collections.emptySet();
        } catch (Exception e) {
            log.error("redis sMembers failed, key={}", key, e);
            return Collections.emptySet();
        }
    }

    /* ------------------------------------------------------------------------
     * ZSet
     * 约定：
     * 1. member 不允许为 null
     * 2. member 统一转 JSON 字符串存储
     * ------------------------------------------------------------------------ */

    public boolean zadd(String key, String member, double score) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(member)) {
            return false;
        }
        try {
            Boolean result = strRedis.opsForZSet().add(key, member, score);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("redis zadd failed, key={}", key, e);
            return false;
        }
    }

    public Long zremRangeByScore(String key, double min, double max) {
        if (!StringUtils.hasText(key)) {
            return 0L;
        }
        try {
            return strRedis.opsForZSet().removeRangeByScore(key, min, max);
        } catch (Exception e) {
            log.error("redis zremRangeByScore failed, key={}", key, e);
            return 0L;
        }
    }

    public Long zcard(String key) {
        if (!StringUtils.hasText(key)) {
            return 0L;
        }
        try {
            return strRedis.opsForZSet().zCard(key);
        } catch (Exception e) {
            log.error("redis zcard failed, key={}", key, e);
            return 0L;
        }
    }

    public void zaddCount(String key, String member) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(member)) {
            return;
        }
        try {
            strRedis.opsForZSet().incrementScore(key, member, 1D);
        } catch (Exception e) {
            log.error("redis zaddCount failed, key={}", key, e);
        }
    }

    public void zaddCount(String key, Object value) {
        if (!StringUtils.hasText(key) || value == null) {
            return;
        }
        try {
            String json = writeObject(value);
            strRedis.opsForZSet().incrementScore(key, json, 1D);
        } catch (Exception e) {
            log.error("redis zaddCountObject failed, key={}", key, e);
        }
    }

    public List<String> getZSetList(String key, Integer count) {
        if (!StringUtils.hasText(key) || count == null || count <= 0) {
            return Collections.emptyList();
        }
        try {
            Set<String> topElements = strRedis.opsForZSet()
                    .reverseRange(key, 0, count - 1L);

            if (CollectionUtils.isEmpty(topElements)) {
                return Collections.emptyList();
            }

            return new ArrayList<>(topElements);
        } catch (Exception e) {
            log.error("redis getZSetList failed, key={}, count={}", key, count, e);
            return Collections.emptyList();
        }
    }

    public <T> List<T> getZSetList(String key, Integer count, Class<T> clazz) {
        if (!StringUtils.hasText(key) || count == null || count <= 0 || clazz == null) {
            return Collections.emptyList();
        }
        try {
            Set<String> topElements = strRedis.opsForZSet().reverseRange(key, 0, count - 1L);
            if (CollectionUtils.isEmpty(topElements)) {
                return Collections.emptyList();
            }

            List<T> list = new ArrayList<>(topElements.size());
            for (String json : topElements) {
                list.add(readObject(json, clazz));
            }
            return list;
        } catch (Exception e) {
            log.error("redis getZSetList(Class) failed, key={}, count={}, clazz={}", key, count, clazz.getName(), e);
            return Collections.emptyList();
        }
    }

    /* ------------------------------------------------------------------------
     * Hash
     * ------------------------------------------------------------------------ */

    public Long hIncrement(String key, String field, long delta) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(field)) {
            return null;
        }
        try {
            return strRedis.opsForHash().increment(key, field, delta);
        } catch (Exception e) {
            log.error("redis hIncrement failed, key={}, field={}, delta={}", key, field, delta, e);
            return null;
        }
    }

    public Map<String, String> hGetAll(String key) {
        if (!StringUtils.hasText(key)) {
            return Collections.emptyMap();
        }
        try {
            return (Map<String, String>) (Map<?, ?>) strRedis.opsForHash().entries(key);
        } catch (Exception e) {
            log.error("redis hGetAll failed, key={}", key, e);
            return Collections.emptyMap();
        }
    }

    public <T> Map<String, T> hGetAll(String key, Class<T> clazz) {
        if (!StringUtils.hasText(key) || clazz == null) {
            return Collections.emptyMap();
        }

        try {
            Map<Object, Object> rawMap = strRedis.opsForHash().entries(key);

            if (CollectionUtils.isEmpty(rawMap)) {
                return Collections.emptyMap();
            }

            Map<String, T> result = new HashMap<>(rawMap.size());

            for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {

                Object fieldObj = entry.getKey();
                Object valueObj = entry.getValue();

                if (fieldObj == null || valueObj == null) {
                    continue;
                }

                try {
                    String field = (String) fieldObj;
                    String json = (String) valueObj;

                    result.put(field, readObject(json, clazz));

                } catch (Exception ex) {
                    log.error("redis hGetAll parse item failed, key={}, field={}",
                            key,
                            fieldObj,
                            ex);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("redis hGetAll object failed, key={}, clazz={}",
                    key,
                    clazz.getName(),
                    e);

            return Collections.emptyMap();
        }
    }

    public String hGet(String key, String field) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(field)) {
            return null;
        }

        try {
            return (String) strRedis.opsForHash().get(key, field);
        } catch (Exception e) {
            log.error("redis hGet failed, key={}, field={}", key, field, e);
            return null;
        }
    }

    public <T> T hGet(String key, String field, Class<T> clazz) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(field) || clazz == null) {
            return null;
        }

        try {
            String json = (String) strRedis.opsForHash().get(key, field);
            if (json == null) {
                return null;
            }

            return readObject(json, clazz);
        } catch (Exception e) {
            log.error("redis hGet object failed, key={}, field={}, clazz={}",
                    key, field, clazz.getName(), e);
            return null;
        }
    }

    public Boolean hSet(String key, String field, String value) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(field)) {
            return false;
        }
        try {
            strRedis.opsForHash().put(key, field, value);
            return true;
        } catch (Exception e) {
            log.error("redis hSet failed, key={}, field={}", key, field, e);
            return false;
        }
    }

    public Boolean hSet(String key, String field, Object value) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(field)) {
            return false;
        }

        try {
            if (value == null) {
                strRedis.opsForHash().delete(key, field);
                return true;
            }

            String json = writeObject(value);
            strRedis.opsForHash().put(key, field, json);
            return true;
        } catch (Exception e) {
            log.error("redis hSet object failed, key={}, field={}", key, field, e);
            return false;
        }
    }

    public Long hDelete(String key, String... fields) {
        if (!StringUtils.hasText(key) || fields == null || fields.length == 0) {
            return 0L;
        }
        try {
            return strRedis.opsForHash().delete(key, (Object[]) fields);
        } catch (Exception e) {
            log.error("redis hDelete failed, key={}, fields={}", key, Arrays.toString(fields), e);
            return 0L;
        }
    }

    public Long hIncrementAndExpireField(String key, String field, long delta, Duration duration) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(field)) {
            return null;
        }
        try {
            Long result = strRedis.opsForHash().increment(key, field, delta);
            strRedis.opsForHash().expire(key, duration, List.of(field));
            return result;
        } catch (Exception e) {
            log.error("redis hIncrementAndExpireField failed, key={}, field={}", key, field, e);
            return null;
        }
    }

    public List<Object> hValues(String key) {
        if (!StringUtils.hasText(key)) {
            return Collections.emptyList();
        }

        try {
            return strRedis.opsForHash().values(key);
        } catch (Exception e) {
            log.error("redis hValues failed, key={}", key, e);
            return Collections.emptyList();
        }
    }

    public Boolean hDeleteIfEquals(String key, String field, String expectedValue) {
        if (!StringUtils.hasText(key)
                || !StringUtils.hasText(field)
                || expectedValue == null) {
            return false;
        }

        String script = """
            local current = redis.call('HGET', KEYS[1], ARGV[1])
            if current == ARGV[2] then
                redis.call('HDEL', KEYS[1], ARGV[1])
                return 1
            end
            return 0
            """;

        try {
            Long result = strRedis.execute(
                    new DefaultRedisScript<>(script, Long.class),
                    Collections.singletonList(key),
                    field,
                    expectedValue
            );

            return Objects.equals(result, 1L);
        } catch (Exception e) {
            log.error("redis hDeleteIfEquals failed, key={}, field={}", key, field, e);
            return false;
        }
    }

    /* ------------------------------------------------------------------------
     * Lua Script helpers (ARGV 全是纯字符串)
     * ------------------------------------------------------------------------ */

    public Long execLongWithStrArgs(DefaultRedisScript<Long> script, List<String> keys, String... args) {
        if (script == null) {
            return null;
        }
        try {
            return strRedis.execute(script, keys == null ? Collections.emptyList() : keys, (Object[]) args);
        } catch (Exception e) {
            log.error("redis execLongWithStrArgs failed, keys={}, args={}", keys, Arrays.toString(args), e);
            return null;
        }
    }

    /* ------------------------------------------------------------------------
     * Private helpers
     * ------------------------------------------------------------------------ */

    private String writeObject(Object value) throws IOException {
        return objectMapper.writeValueAsString(value);
    }

    private <T> T readObject(String json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, clazz);
    }

    private <T> T readObject(String json, TypeReference<T> typeRef) throws IOException {
        return objectMapper.readValue(json, typeRef);
    }
}