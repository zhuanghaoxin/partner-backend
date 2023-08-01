package cn.ichensw.partner.job;

import cn.ichensw.partner.model.domain.User;
import cn.ichensw.partner.service.UserService;
import cn.ichensw.partner.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热任务
 * @author zhx
 */
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 重点用户
     */
    private List<Long> mainUserList = Collections.singletonList(28L);

    /**
     * 每天执行，预热推荐用户
     */
    @Scheduled(cron = "0 58 23 * * *")
    public void doCacheRecommendUser() {
        RLock lock = redissonClient.getLock("partner:precachejob:docache:lock");
        try {
            // 尝试获取锁，只有一个线程可以获取到锁
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                System.out.println("getLock: " + Thread.currentThread().getId());
                for (Long userId : mainUserList) {
                    // 查数据库
                    Page<User> userPage = userService.page(new Page<>(1, 20), new QueryWrapper<>());
                    // 获取当前用户
                    String redisKey = String.format("zhx:user:recommend:%s", userId);
                    // 写缓存
                    try {
                        redisUtils.set(redisKey, userPage, 30);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}
