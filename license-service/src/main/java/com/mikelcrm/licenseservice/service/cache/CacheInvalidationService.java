package com.mikelcrm.licenseservice.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service responsible for invalidating Redis cache entries when tenant state changes.
 * Should be called by all Command services after modifying tenant state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationService {

    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_KEY_PREFIX = "tenant:";
    private static final String CACHE_KEY_SUFFIX = ":access";

    /**
     * Invalidates the access cache for a given tenant.
     * This ensures the next access validation query will rebuild from the database.
     *
     * @param tenantId the tenant whose cache should be invalidated
     */
    public void invalidateAccessCache(UUID tenantId) {
        String key = CACHE_KEY_PREFIX + tenantId + CACHE_KEY_SUFFIX;
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Invalidated access cache for tenant {}", tenantId);
        } else {
            log.debug("No cache entry found to invalidate for tenant {}", tenantId);
        }
    }
}
