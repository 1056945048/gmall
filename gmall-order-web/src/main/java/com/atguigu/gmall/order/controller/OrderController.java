package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
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

/**
 * @author yangkun
 * @date 2020/3/19
 */
@Controller
public class OrderController {
    @Reference
    SkuService skuService;
    @Reference
    CartService cartService;
   @Reference
   UserService userService;
   @Reference
   OrderService orderService;

   @LoginRequired(loginSuccess = true)
   @RequestMapping("toTrade")   //对应cart-web页面上中的toTrade
   public String toTrade(HttpServletRequest request, ModelMap map, HttpServletResponse response, HttpSession session){
       String memberId = (String) request.getAttribute("memberId");
       String nickname = (String) request.getAttribute("nickname");

       //用户收获地址
       List<UmsMemberReceiveAddress>  userAddressList= new ArrayList<>();
       userAddressList = userService.getReceiveAddressByMemberId(memberId);
       map.put("userAddressList",userAddressList);

       //交易的商品信息
       //一、先取出购物车中的商品
       List<OmsOrderItem> omsOrderItems = new ArrayList<>();
       List<OmsCartItem> cartLists = cartService.getCartList(memberId);
       //将购物车信息封装成订单信息
       for (OmsCartItem cartList:cartLists){
           if(cartList.getIsChecked().equals("1")){
               OmsOrderItem omsOrderItem = new OmsOrderItem();
               omsOrderItem.setProductId(cartList.getProductId());
               omsOrderItem.setProductPic(cartList.getProductPic());
               omsOrderItem.setProductName(cartList.getProductName());
               omsOrderItem.setProductSkuId(cartList.getProductSkuId());
               omsOrderItem.setProductPrice(cartList.getPrice());
               omsOrderItem.setProductQuantity(cartList.getQuantity());
               omsOrderItem.setMemberId(cartList.getMemberId());
               omsOrderItems.add(omsOrderItem);
           }
       }
       //生成订单号
       String tradeCode = orderService.geneTradeCode(memberId);
       map.put("omsOrderItems",omsOrderItems);
       map.put("totalAmount",getTotalAmount(cartLists));
       map.put("tradeCode",tradeCode);
       return "trade";
   }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
       BigDecimal totalAmount =BigDecimal.ZERO;
       for(OmsCartItem omsCartItem:omsCartItems){
           BigDecimal totalPrice = omsCartItem.getTotalPrice();
           if(omsCartItem.getIsChecked().equals("1")){
               totalAmount = totalAmount.add(totalPrice);
           }
       }
       return totalAmount;
    }

    //页面中的---提交订单
    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess = true)
    public ModelAndView submitOrder(String receiveAddressId,BigDecimal totalAmount,String tradeCode,HttpServletRequest request,HttpServletResponse response)
    {
       String nickname = (String)request.getAttribute("nickname");
       String memberId =(String)request.getAttribute("memberId");
        //校验订单传递的交易码是否和redis缓存生成的交易码相同
        String success = orderService.checkTradeCode(memberId,tradeCode);
        if("success".equals(success)){

            //订单中的每一个商品信息
            List<OmsOrderItem> omsOrderItems = new ArrayList<>();
            //new 一个订单信息
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setAutoConfirmDay(7);
            omsOrder.setCreateTime(new Date());
            omsOrder.setDiscountAmount(null);
            //omsOrder.setFreightAmount(); 运费，支付后，在生成物流信息时
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setNote("快点发货");
            String outTradeNo = "gmall";
            outTradeNo = outTradeNo + System.currentTimeMillis();// 将毫秒时间戳拼接到外部订单号
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            outTradeNo = outTradeNo + sdf.format(new Date());// 将时间字符串拼接到外部订单号
            omsOrder.setOrderSn(outTradeNo);//外部订单号
            UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getReceiceAddress(receiveAddressId);
            omsOrder.setReceiverCity(umsMemberReceiveAddress.getCity());
            omsOrder.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            omsOrder.setReceiverName(umsMemberReceiveAddress.getName());
            omsOrder.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            omsOrder.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
            omsOrder.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            omsOrder.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            omsOrder.setPayAmount(totalAmount);
            omsOrder.setOrderType("1");
            // 当前日期加一天，一天后配送
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE,1);
            Date time = c.getTime();
            omsOrder.setPayType("0");
            omsOrder.setSourceType("0");
            omsOrder.setReceiveTime(time);
            omsOrder.setStatus("0");
            omsOrder.setOrderType("0");
            omsOrder.setTotalAmount(totalAmount);
            //获取交易的商品信息
            List<OmsCartItem> cartLists = cartService.getCartList(memberId);
            for(OmsCartItem omsCartItem : cartLists){
                if("1".equals(omsCartItem.getIsChecked())){
                    //获取订单详情列表
                    OmsOrderItem omsOrderItem = new OmsOrderItem();
                    //检价
//                    boolean b = skuService.checkPrice(omsCartItem.getProductSkuId(),omsCartItem.getPrice());
//                    if(!b){
//                        ModelAndView mv = new ModelAndView("tradeFail");
//                        return mv;
///                  } b
                   //调用远程服务验库存
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                   omsOrderItem.setProductPrice(omsCartItem.getPrice());
                   omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                   omsOrderItem.setProductName(omsCartItem.getProductName());
                   omsOrderItem.setOrderSn(outTradeNo);//每件商品的订单号和整个订单是一样的
                    omsOrderItem.setProductPic(omsCartItem.getProductPic());
                    omsOrderItem.setProductId(omsCartItem.getProductId());
                    omsOrderItem.setProductAttr(omsCartItem.getProductAttr());
                    omsOrderItem.setRealAmount(omsCartItem.getTotalPrice());
                   omsOrderItems.add(omsOrderItem);
                }
            }
            omsOrder.setOmsOrderItems(omsOrderItems);
            //将订单和订单详情写入数据库，即要保存订单，还要保存订单里的每一个订单商品信息
            //删除用户购物车中的信息
            orderService.saveOrder(omsOrder);

            ModelAndView mv  = new ModelAndView("redirect:http://payment.gmall.com:8053/index");
            mv.addObject("outTradeNo",outTradeNo);
            mv.addObject("totalAmount",totalAmount);
            return mv;

        }else{
            //核对订单交易码失败
            ModelAndView mv = new ModelAndView("tradeFail");
            return mv;
        }
    }
}
