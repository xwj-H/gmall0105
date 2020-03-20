package cn.tedu.gmall.order.controller;

import cn.tedu.gmall.annotations.LoginRequired;
import cn.tedu.gmall.bean.OmsCartItem;
import cn.tedu.gmall.bean.OmsOrder;
import cn.tedu.gmall.bean.OmsOrderItem;
import cn.tedu.gmall.bean.UmsMemberReceiveAddress;
import cn.tedu.gmall.service.CartService;
import cn.tedu.gmall.service.OrderService;
import cn.tedu.gmall.service.SkuService;
import cn.tedu.gmall.service.UserService;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    CartService cartService;

    @Reference
    UserService userService;

    @Reference
    OrderService orderService;

    @Reference
    SkuService skuService;


    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess = true)
    public ModelAndView submitOrder(String receiveAddressId, String tradeCode, BigDecimal totalAmount,
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    HttpSession session, ModelMap modelMap) {

        String memberId = String.valueOf(request.getAttribute("memberId"));
        String nickname = String.valueOf(request.getAttribute("nickname"));

        String outTradeNo = "gmall";
        outTradeNo = outTradeNo + System.currentTimeMillis();//将毫秒时间戳拼接到外部订单号
        SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDDHHmmss");
        outTradeNo = outTradeNo + sdf.format(new Date());//将时间字符串拼接到外部订单号

        //检验交易码
        String success = orderService.checkTradeCode(memberId, tradeCode);

        if (success.equals("success")) {
            List<OmsOrderItem> omsOrderItems = new ArrayList<>();
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setAutoConfirmDay(7);//确认收货
            omsOrder.setCreateTime(new Date());
            omsOrder.setDiscountAmount(null);
            //omsOrder.setFreightAmount();
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setNote("快点发货");
            omsOrder.setOrderSn(outTradeNo);
            omsOrder.setTotalAmount(totalAmount);
            omsOrder.setOrderType(1);
            UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getReceiveAddressById(receiveAddressId);
            omsOrder.setReceiverCity(umsMemberReceiveAddress.getCity());
            omsOrder.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            omsOrder.setReceiverName(umsMemberReceiveAddress.getName());
            omsOrder.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            omsOrder.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            omsOrder.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
            omsOrder.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            //当前日期加一天，一天后配送
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE, 1);
            Date time = c.getTime();
            omsOrder.setReceiveTime(time);
            omsOrder.setSourceType(0);
            omsOrder.setStatus(0);
            omsOrder.setTotalAmount(totalAmount);
            //根据用户id获得
            List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
            for (OmsCartItem omsCartItem : omsCartItems) {
                if (omsCartItem.getIsChecked().equals("1")) {
                    //获得订单详情列表
                    OmsOrderItem omsOrderItem = new OmsOrderItem();
                    //检价
                    boolean b = skuService.checkPrice(omsCartItem.getProductId(), omsCartItem.getPrice());
                    if (b == false) {
                        return new ModelAndView("tradeFail");
                    }

                    //验库存，远程调用库存系统
                    omsOrderItem.setProductPic(omsCartItem.getProductPic());
                    omsOrderItem.setProductName(omsCartItem.getProductName());

                    omsOrderItem.setOrderSn(outTradeNo);//订单号
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
                    omsOrderItem.setProductPrice(omsCartItem.getPrice());
                    omsOrderItem.setRealAmount(omsCartItem.getTotalPrice());
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                    omsOrderItem.setProductSkuCode("11111111");
                    omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                    omsOrderItem.setProductId(omsCartItem.getProductId());
                    omsOrderItem.setProductSn("仓库对应的商品编号");//在仓库中的skuid

                    omsOrderItems.add(omsOrderItem);
                }
            }
            omsOrder.setOmsOrderItems(omsOrderItems);


            //将订单和订单详情写入数据库
            //删除购物车的对应商品
            orderService.saveOrder(omsOrder);

            //重定向到支付系统
            ModelAndView mv = new ModelAndView("redirect:http://localhost:8087/index");
            mv.addObject("outTradeNo", outTradeNo);
            mv.addObject("totalAmount", totalAmount);
            return mv;
        } else {
            return new ModelAndView("tradeFail");
        }
    }

    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess = true)
    public String toTrade(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                          ModelMap modelMap) {

        String memberId = String.valueOf(request.getAttribute("memberId"));
        String nickname = String.valueOf(request.getAttribute("nickname"));

        List<UmsMemberReceiveAddress> receiveAddressByMemberId = userService.getReceiveAddressByMemberId(memberId);

        //将购物车集合转化为页面清单集合
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);


        List<OmsOrderItem> omsOrderItems = new ArrayList<>();

        for (OmsCartItem omsCartItem : omsCartItems) {
            //每循环一个购物车对象，就封装一个商品的详情到OmsOrderItem
            if (omsCartItem.getIsChecked().equals("1")) {
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());

                omsOrderItems.add(omsOrderItem);
            }
        }

        modelMap.addAttribute("omsOrderItems", omsOrderItems);
        modelMap.addAttribute("userAddressList", receiveAddressByMemberId);
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        modelMap.addAttribute("totalAmount", totalAmount);

        //生成交易码，为了在提交订单时做交易码的检验
        String tradeCode = orderService.genTradeCode(memberId);
        modelMap.addAttribute("tradeCode", tradeCode);
        return "trade";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        BigDecimal totalAmount = new BigDecimal("0");

        for (OmsCartItem omsCartItem : omsCartItems) {
            BigDecimal totalPrice = omsCartItem.getTotalPrice();
            if (omsCartItem.getIsChecked().equals("1")) {
                totalAmount = totalAmount.add(totalPrice);
            }
        }

        return totalAmount;
    }
}
