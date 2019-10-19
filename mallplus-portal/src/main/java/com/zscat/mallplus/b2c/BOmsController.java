package com.zscat.mallplus.b2c;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zscat.mallplus.annotation.IgnoreAuth;
import com.zscat.mallplus.annotation.SysLog;
import com.zscat.mallplus.bill.entity.BillAftersales;
import com.zscat.mallplus.bill.entity.BillAftersalesItems;
import com.zscat.mallplus.bill.service.IBillAftersalesItemsService;
import com.zscat.mallplus.bill.service.IBillAftersalesService;
import com.zscat.mallplus.cms.entity.CmsSubject;
import com.zscat.mallplus.enums.AllEnum;
import com.zscat.mallplus.enums.OrderStatus;
import com.zscat.mallplus.exception.ApiMallPlusException;
import com.zscat.mallplus.oms.entity.OmsCartItem;
import com.zscat.mallplus.oms.entity.OmsOrder;
import com.zscat.mallplus.oms.entity.OmsOrderItem;
import com.zscat.mallplus.oms.entity.OmsPayments;
import com.zscat.mallplus.oms.service.IOmsCartItemService;
import com.zscat.mallplus.oms.service.IOmsOrderItemService;
import com.zscat.mallplus.oms.service.IOmsOrderService;
import com.zscat.mallplus.oms.service.IOmsPaymentsService;
import com.zscat.mallplus.oms.vo.CartProduct;
import com.zscat.mallplus.oms.vo.ConfirmOrderResult;
import com.zscat.mallplus.oms.vo.OrderParam;
import com.zscat.mallplus.pms.entity.PmsProductConsult;
import com.zscat.mallplus.pms.mapper.PmsProductMapper;
import com.zscat.mallplus.pms.service.IPmsProductConsultService;
import com.zscat.mallplus.pms.service.IPmsSkuStockService;
import com.zscat.mallplus.pms.vo.ProductConsultParam;
import com.zscat.mallplus.single.ApiBaseAction;
import com.zscat.mallplus.sms.service.ISmsGroupService;
import com.zscat.mallplus.ums.entity.OmsShip;
import com.zscat.mallplus.ums.entity.UmsMember;
import com.zscat.mallplus.ums.entity.UmsMemberReceiveAddress;
import com.zscat.mallplus.ums.mapper.UmsMemberMapper;
import com.zscat.mallplus.ums.mapper.UmsMemberReceiveAddressMapper;
import com.zscat.mallplus.ums.service.IOmsShipService;
import com.zscat.mallplus.ums.service.IUmsMemberReceiveAddressService;
import com.zscat.mallplus.ums.service.IUmsMemberService;
import com.zscat.mallplus.ums.service.RedisService;
import com.zscat.mallplus.util.JsonUtils;
import com.zscat.mallplus.util.UserUtils;
import com.zscat.mallplus.utils.CommonResult;
import com.zscat.mallplus.utils.ValidatorUtils;
import com.zscat.mallplus.vo.ApiContext;
import com.zscat.mallplus.vo.CartParam;
import com.zscat.mallplus.vo.OrderStatusCount;
import com.zscat.mallplus.vo.Rediskey;
import com.zscat.mallplus.vo.home.ServiceMenu;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

/**
 * @Auther: shenzhuan
 * @Date: 2019/4/2 15:02
 * @Description:
 */
@Slf4j
@RestController
@Api(tags = "OmsController", description = "订单管理系统")
public class BOmsController extends ApiBaseAction {
    @Resource
    private UmsMemberMapper memberMapper;
    @Resource
    private ISmsGroupService groupService;
    @Resource
    private IOmsOrderService orderService;
    @Resource
    private IOmsOrderItemService orderItemService;

    @Autowired
    private IOmsCartItemService cartItemService;
    @Autowired
    private IUmsMemberService memberService;
    @Autowired
    private IOmsPaymentsService paymentsService;
    @Autowired
    private IPmsSkuStockService pmsSkuStockService;
    @Autowired
    private IUmsMemberReceiveAddressService memberReceiveAddressService;
    @Resource
    private PmsProductMapper productMapper;
    @Resource
    private UmsMemberReceiveAddressMapper addressMapper;
    @Resource
    private RedisService redisService;
    @Autowired
    private ApiContext apiContext;

    @Autowired
    private IPmsProductConsultService pmsProductConsultService;

    @ApiOperation("添加商品到购物车")
    @RequestMapping(value = "/cart.add")
    @ResponseBody
    public Object addCart(CartParam cartParam) {
        try {
            return orderService.addCart(cartParam);
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;


    }

    @ApiOperation("获取某个会员的购物车列表")
    @RequestMapping(value = "/cart.getlist", method = RequestMethod.POST)
    @ResponseBody
    public Object listCart() {
        UmsMember umsMember = UserUtils.getCurrentMember();
        if (umsMember != null && umsMember.getId() != null) {
            List<OmsCartItem> cartItemList = cartItemService.list(umsMember.getId(), null);
            for (OmsCartItem item : cartItemList){
                if (ValidatorUtils.notEmpty(item.getProductSkuId())){
                    item.setSkuStock(pmsSkuStockService.getById(item.getProductSkuId()));
                }else {
                    item.setProduct(productMapper.selectById(item.getProductId()));
                }
            }
            return new CommonResult().success(cartItemList);
        }
        return new ArrayList<OmsCartItem>();
    }



    @ApiOperation("修改购物车中某个商品的数量")
    @RequestMapping(value = "/cart.setnums", method = RequestMethod.POST)
    @ResponseBody
    public Object updateQuantity(@RequestParam Long id,
                                 @RequestParam Integer quantity) {
        int count = cartItemService.updateQuantity(id, UserUtils.getCurrentMember().getId(), quantity);
        if (count > 0) {
            return new CommonResult().success(count);
        }
        return new CommonResult().failed();
    }

    @ApiOperation("修改购物车中某个商品的数量")
    @RequestMapping(value = "/cart.getnumber", method = RequestMethod.POST)
    @ResponseBody
    public Object getnumber() {
        int count = cartItemService.count(new QueryWrapper<OmsCartItem>().eq("member_id",UserUtils.getCurrentMember().getId()));
        if (count > 0) {
            return new CommonResult().success(count);
        }
        return new CommonResult().success(0);
    }


    @ApiOperation("获取购物车中某个商品的规格,用于重选规格")
    @RequestMapping(value = "/getProduct/{productId}", method = RequestMethod.POST)
    @ResponseBody
    public Object getCartProduct(@PathVariable Long productId) {
        CartProduct cartProduct = cartItemService.getCartProduct(productId);
        return new CommonResult().success(cartProduct);
    }

    @ApiOperation("修改购物车中商品的规格")
    @RequestMapping(value = "/update/attr", method = RequestMethod.POST)
    @ResponseBody
    public Object updateAttr(@RequestBody OmsCartItem cartItem) {
        int count = cartItemService.updateAttr(cartItem);
        if (count > 0) {
            return new CommonResult().success(count);
        }
        return new CommonResult().failed();
    }

    @ApiOperation("删除购物车中的某个商品")
    @RequestMapping(value = "/cart.del")
    @ResponseBody
    public Object delete(String ids) {
        if (StringUtils.isEmpty(ids)) {
            return new CommonResult().failed("参数为空");
        }
        List<Long> resultList = new ArrayList<>(ids.split(",").length);
        for (String s : ids.split(",")) {
            resultList.add(Long.valueOf(s));
        }
        int count = cartItemService.delete(UserUtils.getCurrentMember().getId(), resultList);
        if (count > 0) {
            return new CommonResult().success(count);
        }
        return new CommonResult().failed();
    }

    @ApiOperation("清空购物车")
    @RequestMapping(value = "/clear", method = RequestMethod.POST)
    @ResponseBody
    public Object clear() {
        int count = cartItemService.clear(UserUtils.getCurrentMember().getId());
        if (count > 0) {
            return new CommonResult().success(count);
        }
        return new CommonResult().failed();
    }

    IOmsShipService omsShipService;


    @IgnoreAuth
    @ApiOperation("获取配送方式")
    @RequestMapping(value = "/user.getship", method = RequestMethod.POST)
    @ResponseBody
    public Object getship() {
        List<OmsShip> addressList = omsShipService.list(new QueryWrapper<OmsShip>());
        return new CommonResult().success(addressList);
    }


    @ApiOperation("删除收货地址")
    @RequestMapping(value = "/user.removeship")
    @ResponseBody
    public Object delete(@RequestParam(value = "id", required = false, defaultValue = "0") Long id) {
        boolean count = memberReceiveAddressService.removeById(id);
        if (count) {
            return new CommonResult().success(count);
        }
        return new CommonResult().failed();
    }

    @ApiOperation("修改收货地址")
    @RequestMapping(value = "/user.vuesaveusership")
    @ResponseBody
    public Object update(UmsMemberReceiveAddress address) {
        boolean count = false;
        Long memberId = UserUtils.getCurrentMember().getId();
        if (ValidatorUtils.empty(memberId)){
            return new CommonResult().fail(100);

        }
        address.setMemberId(memberId);
        if (address.getDefaultStatus()==1){
            addressMapper.updateStatusByMember(memberId);
        }
        if (address != null && address.getId() != null) {
            count = memberReceiveAddressService.updateById(address);
        } else {
            count = memberReceiveAddressService.save(address);
        }
        if (count) {
            return new CommonResult().success(count);
        }
        return new CommonResult().failed();    }

    @ApiOperation("微信存储收货地址")
    @RequestMapping(value = "/user.saveusership")
    @ResponseBody
    public Object saveusership(UmsMemberReceiveAddress address) {
        boolean count = false;
        if (address.getDefaultStatus()==1){
            addressMapper.updateStatusByMember(UserUtils.getCurrentMember().getId());
        }
        if (address != null && address.getId() != null) {
            count = memberReceiveAddressService.updateById(address);
        } else {
            count = memberReceiveAddressService.save(address);
        }
        if (count) {
            return new CommonResult().success(count);
        }
        return new CommonResult().failed();    }

    @IgnoreAuth
    @ApiOperation("显示所有收货地址")
    @RequestMapping(value = "/user.getusership", method = RequestMethod.POST)
    @ResponseBody
    public Object list() {
        UmsMember umsMember = UserUtils.getCurrentMember();
        if (umsMember != null && umsMember.getId() != null) {
            List<UmsMemberReceiveAddress> addressList = memberReceiveAddressService.list(new QueryWrapper<UmsMemberReceiveAddress>().eq("member_id",umsMember.getId()));
            return new CommonResult().success(addressList);
        }
        return new ArrayList<UmsMemberReceiveAddress>();
    }

    @IgnoreAuth
    @ApiOperation("显示所有收货地址")
    @RequestMapping(value = "/user.getshipdetail", method = RequestMethod.POST)
    @ResponseBody
    public Object getItem(@RequestParam(value = "id", required = false, defaultValue = "0") Long id) {
        UmsMemberReceiveAddress address = memberReceiveAddressService.getById(id);
        return new CommonResult().success(address);
    }
    @IgnoreAuth
    @ApiOperation("显示所有收货地址")
    @RequestMapping(value = "/payments.getlist", method = RequestMethod.POST)
    @ResponseBody
    public Object getPayments() {
       List<OmsPayments> paymentss = paymentsService.list(new QueryWrapper<OmsPayments>().eq("status",1));
        return new CommonResult().success(paymentss);
    }
    @IgnoreAuth
    @ApiOperation("显示所有收货地址")
    @RequestMapping(value = "/payments.getinfo", method = RequestMethod.POST)
    @ResponseBody
    public Object getPaymentsInfo(@RequestParam(value = "id", required = false, defaultValue = "0") Long id) {
        return new CommonResult().success(paymentsService.getById(id));
    }
    @IgnoreAuth
    @ApiOperation("显示默认收货地址")
    @RequestMapping(value = "/user.getuserdefaultship", method = RequestMethod.POST)
    @ResponseBody
    public Object getItemDefautl() {
        UmsMemberReceiveAddress address = memberReceiveAddressService.getDefaultItem();
        return new CommonResult().success(address);
    }


    /**
     * @param id
     * @return
     */
    @ApiOperation("设为默认地址")
    @RequestMapping(value = "/user.setdefship")
    @ResponseBody
    public Object setDefault(@RequestParam(value = "id", required = false, defaultValue = "0") Long id) {
        int count = memberReceiveAddressService.setDefault(id);
        if (count > 0) {
            return new CommonResult().success(count);
        }
        return new CommonResult().failed();
    }

    @IgnoreAuth
    @SysLog(MODULE = "oms", REMARK = "查询订单列表")
    @ApiOperation(value = "查询订单列表")
    @PostMapping(value = "/order.getorderlist")
    public Object orderList(OmsOrder order,
                            @RequestParam(value = "pageSize", required = false, defaultValue = "100") Integer pageSize,
                            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum) {

        IPage<OmsOrder> page = null;
        if (ValidatorUtils.empty(order.getStatus()) || order.getStatus()==0){
            page = orderService.page(new Page<OmsOrder>(pageNum, pageSize), new QueryWrapper<OmsOrder>().eq("member_id",UserUtils.getCurrentMember().getId()).orderByDesc("create_time")) ;
        }else {
            page = orderService.page(new Page<OmsOrder>(pageNum, pageSize), new QueryWrapper<>(order).orderByDesc("create_time")) ;

        }
        for (OmsOrder omsOrder : page.getRecords()){
            List<OmsOrderItem> itemList = orderItemService.list(new QueryWrapper<OmsOrderItem>().eq("order_id",omsOrder.getId()));
            omsOrder.setOrderItemList(itemList);
        }
        return new CommonResult().success(page);
    }


    @ApiOperation("获取订单详情:订单信息、商品信息、操作记录")
    @RequestMapping(value = "/order.details", method = RequestMethod.POST)
    @ResponseBody
    public Object detail(@RequestParam(value = "id", required = false, defaultValue = "0") Long id) {
        OmsOrder orderDetailResult = null;
        String key = Rediskey.orderDetail+apiContext.getCurrentProviderId()+"orderid"+id;
        String json = redisService.get(key);
        if (ValidatorUtils.notEmpty(json)){
            orderDetailResult = JsonUtils.jsonToPojo(json,OmsOrder.class);
            return   new CommonResult().success(orderDetailResult);
        }
            orderDetailResult = orderService.getById(id);
            OmsOrderItem query = new OmsOrderItem();
            query.setOrderId(id);
            List<OmsOrderItem> orderItemList = orderItemService.list(new QueryWrapper<>(query));
            orderDetailResult.setOrderItemList(orderItemList);
            UmsMember member = memberMapper.selectById(orderDetailResult.getMemberId());
            if(member!=null){
                orderDetailResult.setBlance(member.getBlance());
            }
        redisService.set(key,JsonUtils.objectToJson(orderDetailResult));
        redisService.expire(key,3600*5);
        return new CommonResult().success(orderDetailResult);
    }

    @SysLog(MODULE = "小程序订单管理", REMARK = "取消订单")
    @ApiOperation("关闭订单")
    @RequestMapping(value = "/order.cancel", method = RequestMethod.POST)
    public Object closeOrder(@ApiParam("订单id") @RequestParam Long orderId) {
        try {
            if (ValidatorUtils.empty(orderId)) {
                return new CommonResult().paramFailed("订单id is empty");
            }
            OmsOrder newE = orderService.getById(orderId);
            if (newE.getStatus() != OrderStatus.INIT.getValue()) {
                return new CommonResult().paramFailed("订单已支付，不能关闭");
            }
            if (orderService.closeOrder(newE)) {
                String key = Rediskey.orderDetail+apiContext.getCurrentProviderId()+"orderid"+orderId;
                redisService.remove(key);
                return new CommonResult().success();
            }
        } catch (Exception e) {
            return new CommonResult().failed(e.getMessage());
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "小程序订单管理", REMARK = "删除订单")
    @ApiOperation("删除订单")
    @RequestMapping(value = "/order.del", method = RequestMethod.POST)
    public Object delOrder(@ApiParam("订单id") @RequestParam Long orderId) {
        try {
            if (ValidatorUtils.empty(orderId)) {
                return new CommonResult().paramFailed("订单id is empty");
            }
            OmsOrder newE = orderService.getById(orderId);
            if (newE.getStatus() < 6) {
                return new CommonResult().paramFailed("订单已支付，不能删除");
            }
            if (orderService.removeById(orderId)) {
                String key = Rediskey.orderDetail+apiContext.getCurrentProviderId()+"orderid"+orderId;
                redisService.remove(key);
                return new CommonResult().success();
            }
        } catch (Exception e) {
            return new CommonResult().failed(e.getMessage());
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "订单管理", REMARK = "订单确认收货")
    @ApiOperation("订单确认收货")
    @RequestMapping(value = "/order.confirm", method = RequestMethod.POST)
    @ResponseBody
    public Object confimDelivery(@ApiParam("订单id") @RequestParam Long id) {
        try {
            return new CommonResult().success(orderService.confimDelivery(id));
        } catch (Exception e) {
            log.error("订单确认收货：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }

    }
    /**
     * 查看物流
     */
    @ApiOperation("查看物流")
    @ResponseBody
    @RequestMapping("/order.logisticbyapi")
    public Object getWayBillInfo(@RequestParam(value = "orderId", required = false, defaultValue = "0") Long orderId) throws Exception {
        try {
            UmsMember member = UserUtils.getCurrentMember();
            OmsOrder order = orderService.getById(orderId);
            if (order == null) {
                return null;
            }
            if (!order.getMemberId().equals(member.getId())) {
                return new CommonResult().success("非当前用户订单");
            }

            //    ExpressInfoModel expressInfoModel = orderService.queryExpressInfo(orderId);
            return new CommonResult().success(null);
        } catch (Exception e) {
            log.error("get waybillInfo error. error=" + e.getMessage(), e);
            return new CommonResult().failed("获取物流信息失败，请稍后重试");
        }

    }
    @SysLog(MODULE = "订单管理", REMARK = "取消发货")
    @ApiOperation("取消发货")
    @RequestMapping(value = "/cancleDelivery", method = RequestMethod.POST)
    @ResponseBody
    public Object cancleDelivery(@ApiParam("订单id") @RequestParam Long id,
                                 @ApiParam(value = "订单备注", defaultValue = "我就是想取消") @RequestParam String remark) {
        OmsOrder order = orderService.getById(id);
        if(order == null){
            return new CommonResult().paramFailed("没有找到id为{"+id+"}的订单");
        }

        if (order.getStatus() != OrderStatus.DELIVERED.getValue()) {
            return new CommonResult().paramFailed("已发货订单的物流信息才能取消发货");
        }
        int count = orderService.cancleDelivery(order, remark);

        if (count > 0) {
            return new CommonResult().success(count);
        }
        return new CommonResult().failed();

    }

    @ResponseBody
    @PostMapping("/submitPreview")
    public Object submitPreview(OrderParam orderParam) {
        try {
            ConfirmOrderResult result = orderService.submitPreview(orderParam);
            return new CommonResult().success(result);
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 提交订单
     *
     * @param orderParam
     * @return
     */
    @ApiOperation("根据购物车信息生成订单")
    @RequestMapping(value = "/order.create")
    @ResponseBody
    public Object generateOrder(OrderParam orderParam) {
        try {
            return orderService.generateOrder(orderParam);
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @ApiOperation("发起拼团")
    @RequestMapping(value = "/addGroup")
    @ResponseBody
    public Object addGroup(OrderParam orderParam) {
        try {
            return new CommonResult().success(orderService.addGroup(orderParam));
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @ApiOperation("提交拼团")
    @RequestMapping(value = "/acceptGroup")
    @ResponseBody
    public Object acceptGroup(OrderParam orderParam) {
        try {
            return orderService.acceptGroup(orderParam);
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Resource
    private IBillAftersalesService billAftersalesService;
    @Resource
    private IBillAftersalesItemsService billAftersalesItemsService;

    @IgnoreAuth
    @SysLog(MODULE = "oms", REMARK = "售后单列表")
    @ApiOperation(value = "售后单列表")
    @PostMapping(value = "/order.aftersaleslist")
    public Object afterSalesList(BillAftersales order,
                            @RequestParam(value = "pageSize", required = false, defaultValue = "100") Integer pageSize,
                            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum) {

        IPage<BillAftersales> page = billAftersalesService.page(new Page<BillAftersales>(pageNum, pageSize), new QueryWrapper<>(order).orderByDesc("ctime")) ;
        for (BillAftersales omsOrder : page.getRecords()){
            List<BillAftersalesItems> itemList = billAftersalesItemsService.list(new QueryWrapper<BillAftersalesItems>().eq("aftersales_id",omsOrder.getAftersalesId()));
            omsOrder.setItemList(itemList);
        }
        return new CommonResult().success(page);
    }

    @IgnoreAuth
    @SysLog(MODULE = "oms", REMARK = "售后单详情")
    @ApiOperation(value = "售后单详情")
    @PostMapping(value = "/order.aftersalesinfo")
    public Object afterSalesInfo(@RequestParam Long id) {
        BillAftersales aftersales = billAftersalesService.getById(id);
        List<BillAftersalesItems> itemList = billAftersalesItemsService.list(new QueryWrapper<BillAftersalesItems>().eq("aftersales_id",aftersales.getAftersalesId()));
        aftersales.setItemList(itemList);
        return new CommonResult().success(aftersales);
    }

    @SysLog(MODULE = "cms", REMARK = "订单售后状态")
    @ApiOperation(value = "订单售后状态")
    @PostMapping(value = "/order.aftersalesstatus")
    public Object afterSalesStatus(CmsSubject subject, BindingResult result) {
        CommonResult commonResult;
        UmsMember member = UserUtils.getCurrentMember();

        return null;
    }


    @SysLog(MODULE = "cms", REMARK = "添加售后单")
    @ApiOperation(value = "添加售后单")
    @PostMapping(value = "/order.addaftersales")
    public Object addAfterSales(BillAftersales aftersales, BindingResult result) {
        UmsMember member = UserUtils.getCurrentMember();
        aftersales.setUserId(member.getId());

        return new CommonResult().success( billAftersalesService.save(aftersales));
    }

    @SysLog(MODULE = "cms", REMARK = "用户发送退货包裹")
    @ApiOperation(value = "用户发送退货包裹")
    @PostMapping(value = "/order.sendreship")
    public Object sendShip(CmsSubject subject, BindingResult result) {
        CommonResult commonResult;
        UmsMember member = UserUtils.getCurrentMember();

        return null;
    }

    @SysLog(MODULE = "cms", REMARK = "添加订单评论")
    @ApiOperation(value = "添加订单评论")
    @PostMapping(value = "/user.orderevaluate")
    public Object addGoodsConsult( @RequestParam(value = "orderId", defaultValue = "1") Long orderId,
                                   @RequestParam(value = "items", defaultValue = "10") String items) throws Exception {
        CommonResult commonResult;
        UmsMember member = UserUtils.getCurrentMember();

        List<ProductConsultParam> itemss = JsonUtils.json2list(items,ProductConsultParam.class);
        for (ProductConsultParam param : itemss){
            PmsProductConsult productConsult = new PmsProductConsult();
            if (member!=null){
                productConsult.setPic(member.getIcon());
                productConsult.setMemberName(member.getNickname());
                productConsult.setMemberId(member.getId());
            }else {
                return new CommonResult().failed("请先登录");
            }
            productConsult.setGoodsId(param.getGoodsId());
            productConsult.setConsultContent(param.getTextarea());
            productConsult.setStars(param.getScore());
            productConsult.setEmail(Arrays.toString(param.getImages()));
            productConsult.setConsultAddtime(new Date());
            productConsult.setType(AllEnum.ConsultType.ORDER.code());
            pmsProductConsultService.save(productConsult);
        }
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setId(orderId);
        omsOrder.setIsComment(2);
        omsOrder.setStatus(OrderStatus.TRADE_SUCCESS.getValue());
        if ( orderService.updateById(omsOrder)) {
            commonResult = new CommonResult().success(1);
        } else {
            commonResult = new CommonResult().failed();
        }
        return commonResult;
    }

    @IgnoreAuth
    @ApiOperation("获取订单不同状态的数量")
    @SysLog(MODULE = "applet", REMARK = "获取订单不同状态的数量")
    @PostMapping("/order.getorderstatusnum")
    public Object getOrderStatusSum() {
        Map<String, Object> objectMap = new HashMap<>();

        String key = Rediskey.getorderstatusnum+apiContext.getCurrentProviderId();
        String json = redisService.get(key);
        if (ValidatorUtils.notEmpty(json)){
            objectMap = JsonUtils.readJsonToMap1(json);
            return   new CommonResult().success(objectMap);
        }

        UmsMember umsMember = UserUtils.getCurrentMember();
        OrderStatusCount count = new OrderStatusCount();
        if (umsMember != null && umsMember.getId() != null) {
            OmsOrder param = new OmsOrder();
            param.setMemberId(umsMember.getId());
            List<OmsOrder> list = orderService.list(new QueryWrapper<>(param));
            int status0 = 0;
            int status1 = 0;
            int status2 = 0;
            int status3 = 0;
            int status4 = 0;
            int status5 = 0;
            int status14 = 0;

            int statusAll = 0;
            BigDecimal payAmount = BigDecimal.ZERO;
            for (OmsOrder consult : list) {
                if (consult.getStatus() == OrderStatus.INIT.getValue()) {
                    status0++;
                }
                if (consult.getStatus() == OrderStatus.REFUND.getValue()) {
                    status14++;
                }
                if (consult.getStatus() == OrderStatus.TO_DELIVER.getValue()) {
                    status1++;
                    payAmount=payAmount.add(consult.getPayAmount());
                }
                if (consult.getStatus() == OrderStatus.DELIVERED.getValue()) {
                    status2++;
                    payAmount=payAmount.add(consult.getPayAmount());

                }
                if (consult.getStatus() == OrderStatus.TO_COMMENT.getValue()) {
                    status3++;
                    payAmount=payAmount.add(consult.getPayAmount());

                }
                if (consult.getStatus() == OrderStatus.TRADE_SUCCESS.getValue()) {
                    status4++;
                    payAmount=payAmount.add(consult.getPayAmount());

                }
                if (consult.getStatus() == OrderStatus.RIGHT_APPLY.getValue()) {
                    status5++;
                    payAmount=payAmount.add(consult.getPayAmount());

                }
            }
            statusAll = status1+status2+status3+status4+status5;
            count.setPayAmount(payAmount);
            count.setStatusAll(statusAll);
            count.setStatus0(status0);
            count.setStatus1(status1);
            count.setStatus2(status2);
            count.setStatus3(status3);
            count.setStatus4(status4);
            count.setStatus5(status5);
            count.setStatus14(status14);

        }
        objectMap.put("user", umsMember);
        objectMap.put("count", count);
        List<ServiceMenu> menuList = new ArrayList<>();
        menuList.add(new ServiceMenu("会员中心","http://datong.crmeb.net/public/uploads/attach/2019/03/28/5c9ccc9934a7c.png","/pages/user_vip/index","/user/vip"));
        menuList.add(new ServiceMenu("砍价记录","http://datong.crmeb.net/public/uploads/attach/2019/03/28/5c9ccc9918091.png","/pages/activity/user_goods_bargain_list/index","/activity/bargain/record"));
        menuList.add(new ServiceMenu("我的推广","http://datong.crmeb.net/public/uploads/attach/2019/03/28/5c9ccc9943575.png","/pages/user_spread_user/index","/user/user_promotion"));
        menuList.add(new ServiceMenu("我的余额","http://datong.crmeb.net/public/uploads/attach/2019/03/28/5c9ccc992db31.png","/pages/user_money/index","/user/account"));
        menuList.add(new ServiceMenu("地址信息","http://datong.crmeb.net/public/uploads/attach/2019/03/28/5c9ccc99101a8.png","/pages/user_address_list/index","/user/add_manage"));
        menuList.add(new ServiceMenu("我的收藏","http://datong.crmeb.net/public/uploads/attach/2019/03/28/5c9ccc99269d1.png","/pages/user_goods_collection/index","/collection"));
        menuList.add(new ServiceMenu("优惠券","http://datong.crmeb.net/public/uploads/attach/2019/03/28/5c9ccc991f394.png","/pages/user_coupon/index","/user/user_coupon"));
        menuList.add(new ServiceMenu("联系客服","http://kaifa.crmeb.net/uploads/attach/2019/07/20190730/0ded3d3f72d654fb33c8c9f30a268c97.png","/pages/service/index","/customer/list"));
        objectMap.put("menuList",menuList);
        redisService.set(key,JsonUtils.objectToJson(objectMap));
        redisService.expire(key,3600*5);
        return new CommonResult().success(objectMap);

    }
}
