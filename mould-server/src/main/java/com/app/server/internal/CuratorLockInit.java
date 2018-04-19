package com.app.server.internal;

import lombok.extern.log4j.Log4j2;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 初始化 Curator 客户端及获取分布式锁
 */
@Log4j2
@Component
public class CuratorLockInit {
    // 重试之间等待的初始时间量，单位：毫秒
    @Value("${curator.connect.sleeptime}")
    private int baseSleepTimeMs;

    // 最大重试次数
    @Value("${curator.connect.retries}")
    private int maxRetries;

    // Zookeeper 服务器地址
    @Value("${curator.connect.address}")
    private String connectString;

    // 分布式锁路径
    @Value("${curator.lock.path}")
    private String lockPath;

    // Curator 客户端
    private CuratorFramework client = null;
    // 共享重入锁
    private InterProcessMutex interProcessMutex = null;

    @PostConstruct
    void init() {
        // 设置 Curator 连接 ZK 服务器的重试策略
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries);
        // 使用默认会话超时和默认连接超时创建新客户端
        client = CuratorFrameworkFactory.newClient(connectString, retryPolicy);
        // 启动客户端
        client.start();

        if (log.isInfoEnabled())
            log.info("Curator 客户端启动完成，状态：{}", client.getState());
    }

    @PreDestroy
    void destroy() {
        CloseableUtils.closeQuietly(client);

        if (log.isInfoEnabled())
            log.info("Curator 客户端关闭完成，状态：{}", client.getState());
    }

    /**
     * 获取共享重入锁
     *
     * @return InterProcessMutex 共享重入锁
     */
    synchronized InterProcessMutex getInterProcessMutex() {
        if (interProcessMutex == null) {
            interProcessMutex = new InterProcessMutex(client, lockPath);

            if (log.isInfoEnabled())
                log.info("初始化共享重入锁成功，锁路径：{}", lockPath);
        }
        return interProcessMutex;
    }
}
