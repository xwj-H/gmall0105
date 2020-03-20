package cn.tedu.gmall.order.service.impl;

import cn.tedu.gmall.bean.OmsOrder;
import cn.tedu.gmall.bean.OmsOrderItem;
import cn.tedu.gmall.mq.ActiveMQUtil;
import cn.tedu.gmall.order.mapper.OmsOrderItemMapper;
import cn.tedu.gmall.order.mapper.OmsOrderMapper;
import cn.tedu.gmall.service.CartService;
import cn.tedu.gmall.service.OrderService;
import cn.tedu.gmall.util.RedisUtil;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper omsOrderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Reference
    CartService cartService;

    @Override
    public String checkTradeCode(String memberId, String tradeCode) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:" + memberId + ":tradeCode";
            String tradeCodeFromCache = jedis.get(tradeKey);
            //对比防重删令牌
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else " +
                    "return 0 end";
            Long eval = (Long) jedis.eval(script, Collections.singletonList(tradeKey),
                    Collections.singletonList(tradeCode));

            if (eval != null && eval != 0) {
                jedis.del(tradeKey); // 使用lua脚本在发现key的同时将key删除，防止并发订单攻击
                return "success";
            } else {
                return "fail";
            }
        } finally {
            jedis.close();
        }

    }

    @Override
    public String genTradeCode(String memberId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeKey = "user:" + memberId + ":tradeCode";
        String tradeCode = UUID.randomUUID().toString();

        jedis.setex(tradeKey, 60 * 15, tradeCode);

        jedis.close();
        return tradeCode;
    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {
        omsOrderMapper.insertSelective(omsOrder);
        String orderId = omsOrder.getId();
        //保存订单详情
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
            //删除购物车数据
        }
    }

    @Override
    public OmsOrder getOrderByOutTradeNo(String outTradeNo) {
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(outTradeNo);
        OmsOrder omsOrder1 = omsOrderMapper.selectOne(omsOrder);
        return omsOrder1;
    }

    @Override
    @Transactional
    public void updateOrder(OmsOrder omsOrder) {
        omsOrder.setStatus(1);
        Example example = new Example(OmsOrder.class);
        example.createCriteria().andEqualTo("orderSn", omsOrder.getOrderSn());
        OmsOrder omsOrderUpdate = new OmsOrder();
        omsOrderUpdate.setStatus(1);

        //发送一个订单已支付的队列，提供给库存消费
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch
        (JMSException e) {
            e.printStackTrace();
        }

        try {
            //支付成功后，引起的系统服务->订单服务的更新->库存服务->物流服务
            //调用mq发送支付成功的消息
            Queue payhment_success_queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(payhment_success_queue);

            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();//字符串文本
//            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();//hash结构
            omsOrderMapper.updateByExampleSelective(omsOrderUpdate, example);

            OmsOrder omsOrder1 = new OmsOrder();
            omsOrder1.setOrderSn(omsOrder.getOrderSn());
            OmsOrder omsOrder2 = omsOrderMapper.selectOne(omsOrder1);

            OmsOrderItem omsOrderItem = new OmsOrderItem();
            omsOrderItem.setOrderSn(omsOrder1.getOrderSn());
            List<OmsOrderItem> omsOrderItems = omsOrderItemMapper.select(omsOrderItem);
            omsOrder2.setOmsOrderItems(omsOrderItems);

            textMessage.setText(JSON.toJSONString(omsOrder2));


            producer.send(textMessage);

            session.commit();
        } catch (Exception e) {
            //消息回滚
            try {
                session.rollback();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }

        } finally {
            try {
                session.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }

        }
    }


}
