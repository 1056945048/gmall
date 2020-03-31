package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author yangkun
 * @date 2020/3/2
 */
@Controller
public class CartController {
    @Reference
    CartService cartService;
    @Reference
    SkuService skuService;
    @RequestMapping("checkAll")
    public String checkAll(){
        return "1000";
    }


    /**
    每点击一次购物车中物品的选中按钮就会执行这个方法
     **/
    @LoginRequired(loginSuccess = false)
    @RequestMapping("checkCart")
    public String checkCart(HttpServletRequest request,String isChecked,String skuId,ModelMap map){
        //调用服务修改状态
        String memberId =(String)request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setIsChecked(isChecked);
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setMemberNickname(nickname);
        cartService.checkCart(omsCartItem);
        //将最新的数据从缓存中查出，渲染给数据页
        List<OmsCartItem> omsCartItems = cartService.getCartList(memberId);
        map.put("cartList",omsCartItems);
        //被勾选商品的总额
        BigDecimal totalAmount =getTotalAmount(omsCartItems);
        map.put("totalAmount",totalAmount);
        return "cartListInner";
    }

    /**
     * 点击success.html中的去购物车结算
     * @param request
     * @param response
     * @param session
     * @param map
     * @return
     */
    @RequestMapping("cartList")
    @LoginRequired(loginSuccess = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap map){
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        String memberId =(String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");
        //从cookie中取
        String cartListCookies = CookieUtil.getCookieValue(request,"cartListCookie",true);
        if(StringUtils.isNotBlank(cartListCookies)){
            omsCartItems = JSON.parseArray(cartListCookies, OmsCartItem.class);
        }
        if(StringUtils.isNotBlank(memberId)){
            //登陆后需要将cookie中商品和redis进行合并??
            /**
             * 将cookie中的信息保存在数据库中即可
             */
            if(StringUtils.isNotBlank(cartListCookies)){
                cartService.createCookieQueue(omsCartItems,memberId);
            }

            omsCartItems = cartService.getCartList(memberId);
        }


        for(OmsCartItem cart:omsCartItems){
            cart.setTotalPrice(cart.getPrice().multiply(cart.getQuantity()));
        }
        map.put("cartList",omsCartItems);
        //被勾选商品的总价
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        map.put("totalAmount",totalAmount);
        return "cartList";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        BigDecimal totalAmount =BigDecimal.ZERO;
        for (OmsCartItem omsCartItem:omsCartItems){
            if("1".equals(omsCartItem.getIsChecked())){
               totalAmount = totalAmount.add(omsCartItem.getTotalPrice());
            }
        }
        return totalAmount;
    }

    @RequestMapping("addToCart")
    @LoginRequired(loginSuccess = false)
    public String addToCart(String skuId, int quantity, HttpServletRequest request, HttpServletResponse response, HttpSession session){
        //调用商品查询
        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId);
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        // 将商品信息封装成购物车信息
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(pmsSkuInfo.getPrice());
        omsCartItem.setProductAttr("颜色漂亮");
        omsCartItem.setProductBrand("摩托罗拉");
        omsCartItem.setProductCategoryId(pmsSkuInfo.getCatalog3Id());
        omsCartItem.setProductId(pmsSkuInfo.getProductId());
        omsCartItem.setProductName(pmsSkuInfo.getSkuName());
        omsCartItem.setProductPic(pmsSkuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode("11111111111");
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(new BigDecimal(quantity));
        omsCartItem.setIsChecked("1");
        //判断用户是否登陆
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");
        if(StringUtils.isBlank(memberId)){
            //未登录
           String cartListCookie = CookieUtil.getCookieValue(request,"cartListCookie",true);
           if(StringUtils.isBlank(cartListCookie))
           {
               //Cookie中的信息为空,就向购物车中添加信息
               omsCartItems.add(omsCartItem);
           } else
               {
               List<OmsCartItem> omsCartItemCookie = JSON.parseArray(cartListCookie,OmsCartItem.class);
                   //判断Cookie中是否已经有这个商品
               boolean exist =if_cart_exist(omsCartItemCookie,omsCartItem);
               if(exist){
                   //之前添加过更改购物车数量
                   for(OmsCartItem cart:omsCartItemCookie){
                       if(cart.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                           cart.setQuantity(omsCartItem.getQuantity().add(cart.getQuantity()));
                           cart.setIsChecked("1");
                       }
                   }
               }else{
                   //之前cookie中没有添加
                   omsCartItems.add(omsCartItem);
               }
               //将原先cookie中的商品信息也添加到cookie中
                   omsCartItems.addAll(omsCartItemCookie);
           }

           //更新Cookie
            CookieUtil.setCookie(request,response,"cartListCookie",JSON.toJSONString(omsCartItems),60*60*10,true);
        } else {
            //已登陆
            //从数据库中查出信息
            OmsCartItem omsCartItemFromDB = cartService.ifCartExistByUser(memberId,skuId);
            if(omsCartItemFromDB == null){
                //没有添加过这件商品,就将它加入表中
                omsCartItem.setMemberId(memberId);
                omsCartItem.setProductSkuId(skuId);
                omsCartItem.setMemberNickname(nickname);
                cartService.addCart(omsCartItem);
            }else{
                //更新商品数量
                omsCartItemFromDB.setQuantity(omsCartItemFromDB.getQuantity().add(omsCartItem.getQuantity()));
                cartService.updateCart(omsCartItemFromDB);
            }
            //更新缓存
            cartService.flushCache(memberId);
        }
        return "success";
    }

    private boolean if_cart_exist(List<OmsCartItem> omsCartItemCookie, OmsCartItem omsCartItem) {
        boolean exist = false;
        for(OmsCartItem cookie:omsCartItemCookie){
            if (cookie.getProductSkuId().equals(omsCartItem.getProductSkuId())) {
                exist = true;
                break;
            }
        }
        return exist;
    }

}
