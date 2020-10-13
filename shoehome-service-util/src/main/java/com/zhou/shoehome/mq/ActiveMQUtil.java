package com.zhou.shoehome.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

import javax.jms.ConnectionFactory;

/**
 * @author zhouzh6
 * @date 2020-10-12
 */
public class ActiveMQUtil {

    PooledConnectionFactory pooledConnectionFactory = null;

    public ConnectionFactory init(String brokerUrl) {

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        // 加入连接池
        pooledConnectionFactory = new PooledConnectionFactory(factory);
        // 出现异常时重新连接
        pooledConnectionFactory.setReconnectOnException(true);
        // 最大连接数
        pooledConnectionFactory.setMaxConnections(5);
        // 过期时间
        pooledConnectionFactory.setExpiryTimeout(10000);
        return pooledConnectionFactory;
    }

    public ConnectionFactory getConnectionFactory() {
        return pooledConnectionFactory;
    }
}