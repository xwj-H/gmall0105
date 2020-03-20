package cn.tedu.gmall.seckill.controller;

import cn.tedu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import redis.clients.jedis.Jedis;

@Controller
public class SeckillController {
    @Autowired
    RedisUtil redisUtil;
    @RequestMapping("kill")
    public String kill() {
        Jedis jedis = redisUtil.getJedis();
        jedis.incrBy()
        return "1";
    }
}
