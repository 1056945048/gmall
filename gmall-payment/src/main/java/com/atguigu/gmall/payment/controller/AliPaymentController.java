package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yangkun
 * @date 2020/3/22
 */
@Controller
public class AliPaymentController {
    @Reference
    OrderService orderService;
    @Autowired
    AlipayClient alipayClient;
    //开始使用的reference不行
    @Autowired
    PaymentService paymentService;
    /**
     * 支付宝回调接口方法
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("alipay/callback/return")
    @LoginRequired(loginSuccess = true)
    public String alipayReturn(HttpServletRequest request,HttpServletResponse response){
        //获取支付宝中的回调参数
        String sign = request.getParameter("sign");
        String trade_no = request.getParameter("trade_no");
        String out_trade_no = request.getParameter("out_trade_no");
        String trade_status = request.getParameter("trade_status");
        String total_amount = request.getParameter("total_amount");
        String callbackContent = request.getQueryString();
        //还要通过paramsmap进行验签，2.0没了此参数，只能异步验签，所以这里忽略
        if(StringUtils.isNotBlank(sign)){
           PaymentInfo paymentInfo = new PaymentInfo();
           paymentInfo.setOrderSn(out_trade_no);//自己的订单号
           paymentInfo.setPaymentStatus("已支付");
           paymentInfo.setCallbackTime(new Date());
           paymentInfo.setAlipayTradeNo(trade_no);//支付宝中的订单号
           paymentInfo.setCallbackContent(callbackContent);
            /**
             * 产生支付消息队列
             */
           paymentService.updatePaymentInfo(paymentInfo);
        }

    return "finish";
    }

    @RequestMapping("alipay/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String AliPayment(String outTradeNo,BigDecimal totalAmount,HttpServletRequest request,HttpServletResponse response) {

        //创建支付宝请求接口
        AlipayTradeAppPayRequest alipayTradeAppPayRequest = new AlipayTradeAppPayRequest();
        alipayTradeAppPayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        map.put("total_amount","0.1");
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("subject","尚硅谷感光徕卡Pro300瞎命名系列手机");
        String param = JSON.toJSONString(map);
        alipayTradeAppPayRequest.setBizContent(param);
        String form ="";
        try {
            form = alipayClient.pageExecute(alipayTradeAppPayRequest).getBody();
            System.out.println(form);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //生成并且保存用户的支付信息
        OmsOrder omsOrder = orderService.getOrderInfo(outTradeNo);//根据订单编号获得订单信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderSn(outTradeNo);
        paymentInfo.setTotalAmount(totalAmount);
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setPaymentStatus("未支付");
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setSubject("尚硅谷感光徕卡Pro300瞎命名系列手机");
        paymentService.savePaymentInfo(paymentInfo);
        /**
         *向消息中间件发送一个检查支付状态的延迟消息队列
         */
       paymentService.sendDelayPaymentResultCheckQueue(outTradeNo,5);
       return form;
    }
    @RequestMapping("index")
    @LoginRequired(loginSuccess = true)
    public String index(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, HttpServletResponse response, ModelMap map){
     String memberId=(String)request.getAttribute("memberId");
     String nickname = (String)request.getAttribute("nickname");
     map.put("outTradeNo",outTradeNo);
     map.put("totalAmount",totalAmount);
     map.put("memberId",memberId);
     return "index";
    }
}
