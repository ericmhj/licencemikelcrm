package com.mikelcrm.licenseservice.service.cache;

import com.mikelcrm.licenseservice.domain.entity.Plan;
import com.mikelcrm.licenseservice.domain.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Syncs plan data to Redis for fast access by APISIX and SGR.
 * Runs at startup and can be triggered manually when plans change.
 *
 * Redis keys:
 *   plan:{codigo}:roles  → SET of role strings
 *   plan:{codigo}:limits → HASH with creditos, downloads, usuarios
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanCacheWarmupService implements ApplicationRunner {

    private final PlanRepository planRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        warmup();
    }

    /**
     * Load all active plans into Redis. Can be called after plan CRUD operations.
     */
    public void warmup() {
        List<Plan> plans = planRepository.findByActivoTrue();
        int count = 0;

        for (Plan plan : plans) {
            syncPlanToRedis(plan);
            count++;
        }

        log.info("[PlanCacheWarmup] {} planes cargados en Redis", count);
    }

    /**
     * Sync a single plan to Redis (used after create/update).
     */
    public void syncPlanToRedis(Plan plan) {
        String rolesKey = "plan:" + plan.getCodigo() + ":roles";
        String limitsKey = "plan:" + plan.getCodigo() + ":limits";

        // Delete existing set and recreate
        redisTemplate.delete(rolesKey);
        String rolesJson = plan.getRolesAutorizados();
        if (rolesJson != null && !rolesJson.isBlank()) {
            String[] roles = rolesJson.replace("[", "").replace("]", "").replace("\"", "").split(",\\s*");
            redisTemplate.opsForSet().add(rolesKey, roles);
        }

        // Write limits as hash
        redisTemplate.opsForHash().putAll(limitsKey, Map.of(
                "creditos", String.valueOf(plan.getCreditosMensuales()),
                "downloads", String.valueOf(plan.getMaxFreeDownloads()),
                "usuarios", String.valueOf(plan.getMaxUsuarios())
        ));
    }

    /**
     * Remove a plan from Redis (used after deactivation).
     */
    public void removePlanFromRedis(String codigo) {
        redisTemplate.delete("plan:" + codigo + ":roles");
        redisTemplate.delete("plan:" + codigo + ":limits");
    }
}
