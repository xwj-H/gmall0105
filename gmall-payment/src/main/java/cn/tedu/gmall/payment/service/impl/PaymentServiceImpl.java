package cn.tedu.gmall.payment.service.impl;

import cn.tedu.gmall.bean.PaymentInfo;
import cn.tedu.gmall.mq.ActiveMQUtil;
import cn.tedu.gmall.payment.mapper.PaymentInfoMapper;
import cn.tedu.gmall.service.PaymentService;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    @Transactional
    public void updatePayment(PaymentInfo paymentInfo) {
        // 幂等性检查
        PaymentInfo paymentInfoParam = new PaymentInfo();
        paymentInfoParam.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo paymentInfoResult = paymentInfoMapper.selectOne(paymentInfoParam);

        if (StringUtils.isNotBlank(paymentInfoResult.getPaymentStatus()) && paymentInfoResult.getPaymentStatus().equals("已支付")) {
            return;
        } else {
            String orderSn = paymentInfo.getOrderSn();
            Example example = new Example(PaymentInfo.class);
            example.createCriteria().andEqualTo("orderSn", orderSn);

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
                Queue payhment_success_queue = session.createQueue("PAYHMENT_SUCCESS_QUEUE");
                MessageProducer producer = session.createProducer(payhment_success_queue);

                //ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();//字符串文本
                ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();//hash结构
                mapMessage.setString("out_trade_no", paymentInfo.getOrderSn());

                paymentInfoMapper.updateByExampleSelective(paymentInfo, example);

                producer.send(mapMessage);

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

    @Override
    public void sendDelayPaymentResultCheckQueue(String outTradeNo, int count) {

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
            Queue payhment_success_queue = session.createQueue("PAYHMENT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(payhment_success_queue);

            //ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();//字符串文本
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();//hash结构
            mapMessage.setString("out_trade_no", outTradeNo);
            mapMessage.setInt("count", count);
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, 1000 * 30);

            producer.send(mapMessage);

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

    @Override
    public Map<String, Object> checkAlipayPayment(String out_trade_no) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("out_trade_no", out_trade_no);
        request.setBizContent(JSON.toJSONString(resultMap));
        AlipayTradeQueryResponse response = null;

        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            resultMap.put("out_trade_no", response.getOutTradeNo());
            resultMap.put("trade_no", response.getTradeNo());
            resultMap.put("trade_status", response.getTradeStatus());
        } else {
            System.out.println("调用失败");
        }

        return resultMap;
    }
}
