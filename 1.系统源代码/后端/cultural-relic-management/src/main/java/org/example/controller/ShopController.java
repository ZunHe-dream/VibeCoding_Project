package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.Result;
import org.example.entity.*;
import org.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private ShopProductService productService;
    @Autowired private ShopCategoryService categoryService;
    @Autowired private ShopCartService cartService;
    @Autowired private ShopOrderService orderService;
    @Autowired private ShopOrderItemService orderItemService;
    @Autowired private UserAddressService addressService;
    @Autowired private ShopReviewService reviewService;

    // ── 分类 ──────────────────────────────
    @GetMapping("/category/list")
    public Result<List<ShopCategory>> categoryList() {
        return Result.success(categoryService.list(
                new LambdaQueryWrapper<ShopCategory>().orderByAsc(ShopCategory::getSort)));
    }

    @PostMapping("/category/save")
    public Result<String> categorySave(@RequestBody ShopCategory cat) {
        return categoryService.saveOrUpdate(cat) ? Result.success("保存成功") : Result.error("保存失败");
    }

    @DeleteMapping("/category/{id}")
    public Result<String> categoryDelete(@PathVariable Long id) {
        return categoryService.removeById(id) ? Result.success("删除成功") : Result.error("删除失败");
    }

    // ── 商品 ──────────────────────────────
    @GetMapping("/product/list")
    public Result<List<ShopProduct>> productList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long catId,
            @RequestParam(required = false) String museum,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String sort) {
        return Result.success(productService.search(keyword, catId, museum, type, sort));
    }

    @GetMapping("/product/{id}")
    public Result<ShopProduct> productDetail(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    @PostMapping("/product/save")
    public Result<Long> productSave(@RequestBody ShopProduct product) {
        boolean ok = productService.saveOrUpdate(product);
        return ok ? Result.success(product.getProductId()) : Result.error("保存失败");
    }

    @DeleteMapping("/product/{id}")
    public Result<String> productDelete(@PathVariable Long id) {
        return productService.removeById(id) ? Result.success("删除成功") : Result.error("删除失败");
    }

    // ── 购物车 ────────────────────────────
//    @GetMapping("/cart/list")
//    public Result<List<ShopCart>> cartList(@RequestParam Long userId) {
//        List<ShopCart> carts = cartService.list(
//                new LambdaQueryWrapper<ShopCart>().eq(ShopCart::getUserId, userId));
//        carts.forEach(c -> c.setProduct(productService.getById(c.getProductId())));
//        return Result.success(carts);
//    }
    @GetMapping("/cart/list")
    public Result<List<ShopCart>> cartList(@RequestParam Long userId) {
        List<ShopCart> carts = cartService.list(
                new LambdaQueryWrapper<ShopCart>().eq(ShopCart::getUserId, userId));
        carts.forEach(c -> {
            ShopProduct p = productService.getById(c.getProductId());
            // ✨ 如果有skuId，用SKU价格覆盖商品默认价格
            if (c.getSkuId() != null) {
                ShopProductSku sku = skuService.getById(c.getSkuId());
                if (sku != null && p != null) {
                    p.setPrice(sku.getPrice());
                    p.setCoverUrl(sku.getCoverUrl() != null ? sku.getCoverUrl() : p.getCoverUrl());
                }
            }
            c.setProduct(p);
        });
        return Result.success(carts);
    }

    @PostMapping("/cart/add")
    public Result<String> cartAdd(@RequestBody ShopCart cart) {
        ShopCart existing = cartService.getOne(new LambdaQueryWrapper<ShopCart>()
                .eq(ShopCart::getUserId, cart.getUserId())
                .eq(ShopCart::getProductId, cart.getProductId())
                .eq(cart.getSkuId() != null, ShopCart::getSkuId, cart.getSkuId())
                .isNull(cart.getSkuId() == null, ShopCart::getSkuId)
        );
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + cart.getQuantity());
            cartService.updateById(existing);
        } else {
            cartService.save(cart);
        }
        return Result.success("已加入购物车");
    }

    @PostMapping("/cart/update")
    public Result<String> cartUpdate(@RequestBody ShopCart cart) {
        if (cart.getQuantity() <= 0) {
            cartService.removeById(cart.getCartId());
            return Result.success("已移除");
        }
        cartService.updateById(cart);
        return Result.success("已更新");
    }

    @DeleteMapping("/cart/{cartId}")
    public Result<String> cartDelete(@PathVariable Long cartId) {
        return cartService.removeById(cartId) ? Result.success("已移除") : Result.error("移除失败");
    }

    // ── 地址 ──────────────────────────────
    @GetMapping("/address/list")
    public Result<List<UserAddress>> addressList(@RequestParam Long userId) {
        return Result.success(addressService.list(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)));
    }

    @PostMapping("/address/save")
    public Result<String> addressSave(@RequestBody UserAddress addr) {
        if (addr.getIsDefault() == 1) {
            // 取消其他默认
            UserAddress update = new UserAddress();
            update.setIsDefault(0);
            addressService.update(update, new LambdaQueryWrapper<UserAddress>()
                    .eq(UserAddress::getUserId, addr.getUserId()));
        }
        return addressService.saveOrUpdate(addr) ? Result.success("保存成功") : Result.error("保存失败");
    }

    @DeleteMapping("/address/{id}")
    public Result<String> addressDelete(@PathVariable Long id) {
        return addressService.removeById(id) ? Result.success("删除成功") : Result.error("删除失败");
    }

    // ── 订单 ──────────────────────────────
    @PostMapping("/order/create")
    public Result<String> createOrder(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Long addrId = Long.valueOf(params.get("addrId").toString());
        List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");
        String remark = params.getOrDefault("remark", "").toString();

        UserAddress addr = addressService.getById(addrId);
        if (addr == null) return Result.error("收货地址不存在");

        // 生成订单号
        String orderId = "ORD" + System.currentTimeMillis();
        BigDecimal total = BigDecimal.ZERO;

        List<ShopOrderItem> orderItems = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Long productId = Long.valueOf(item.get("productId").toString());
            int qty = Integer.parseInt(item.get("quantity").toString());
            ShopProduct product = productService.getById(productId);
            if (product == null || product.getStatus() == 0) return Result.error("商品 " + productId + " 不可购买");

// ✨ 取SKU价格
            BigDecimal unitPrice = product.getPrice();
            if (item.get("skuId") != null) {
                Long skuId = Long.valueOf(item.get("skuId").toString());
                ShopProductSku sku = skuService.getById(skuId);
                if (sku != null) {
                    unitPrice = sku.getPrice();
                    // 扣SKU库存
                    if (sku.getStock() < qty) return Result.error("商品 [" + product.getName() + "] 库存不足");
                    sku.setStock(sku.getStock() - qty);
                    skuService.updateById(sku);
                }
            } else {
                if (product.getStock() < qty) return Result.error("商品 [" + product.getName() + "] 库存不足");
            }

// 扣商品总库存
            product.setStock(product.getStock() - qty);
            product.setSales(product.getSales() + qty);
            productService.updateById(product);

            ShopOrderItem oi = new ShopOrderItem();
            oi.setOrderId(orderId);
            oi.setProductId(productId);
            oi.setProductName(product.getName());
            oi.setCoverUrl(product.getCoverUrl());
            oi.setPrice(unitPrice); // ✨ 用SKU价格
            oi.setQuantity(qty);
            oi.setBelongMuseum(product.getBelongMuseum());
            orderItems.add(oi);
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(qty)));
        }

        // ✨ 加在这里，循环结束后、创建order对象之前
        if (params.containsKey("shippingFee") && params.get("shippingFee") != null) {
            BigDecimal fee = new BigDecimal(params.get("shippingFee").toString());
            total = total.add(fee);
        }

        ShopOrder order = new ShopOrder();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setTotalPrice(total);
        order.setStatus(0);
        order.setAddrId(addrId);
        order.setReceiver(addr.getReceiver());
        order.setPhone(addr.getPhone());
        order.setAddress(addr.getProvince() + addr.getCity() + addr.getDistrict() + addr.getDetail());
        order.setRemark(remark);
        orderService.save(order);
        orderItemService.saveBatch(orderItems);

        // 清购物车
        if (params.containsKey("cartIds")) {
            List<Integer> cartIds = (List<Integer>) params.get("cartIds");
            cartService.removeByIds(cartIds.stream().map(Long::valueOf).collect(Collectors.toList()));
        }


        return Result.success(orderId);
    }

    @GetMapping("/order/list")
    public Result<List<ShopOrder>> orderList(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<ShopOrder> w = new LambdaQueryWrapper<ShopOrder>()
                .eq(ShopOrder::getUserId, userId)
                .eq(status != null, ShopOrder::getStatus, status)
                .orderByDesc(ShopOrder::getCreateTime);
        List<ShopOrder> orders = orderService.list(w);
        orders.forEach(o -> o.setItems(orderItemService.list(
                new LambdaQueryWrapper<ShopOrderItem>().eq(ShopOrderItem::getOrderId, o.getOrderId()))));
        return Result.success(orders);
    }

    @PostMapping("/order/pay")
    public Result<String> payOrder(@RequestParam String orderId) {
        ShopOrder order = orderService.getById(orderId);
        if (order == null) return Result.error("订单不存在");
        if (order.getStatus() != 0) return Result.error("订单状态异常");
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        return orderService.updateById(order) ? Result.success("支付成功") : Result.error("支付失败");
    }
//取消订单
//    @PostMapping("/order/cancel")
//    public Result<String> cancelOrder(@RequestParam String orderId) {
//        ShopOrder order = orderService.getById(orderId);
//        if (order == null) return Result.error("订单不存在");
//        if (order.getStatus() != 0) return Result.error("只能取消待付款订单");
//
//        // 恢复库存
//        List<ShopOrderItem> items = orderItemService.list(
//                new LambdaQueryWrapper<ShopOrderItem>().eq(ShopOrderItem::getOrderId, orderId));
//        items.forEach(item -> {
//            ShopProduct p = productService.getById(item.getProductId());
//            if (p != null) {
//                p.setStock(p.getStock() + item.getQuantity());
//                p.setSales(Math.max(0, p.getSales() - item.getQuantity()));
//                productService.updateById(p);
//            }
//        });
//
//        order.setStatus(4);
//        return orderService.updateById(order) ? Result.success("已取消") : Result.error("取消失败");
//    }
@PostMapping("/order/cancel")
public Result<String> cancelOrder(@RequestParam String orderId) {
    ShopOrder order = orderService.getById(orderId);
    if (order == null) return Result.error("订单不存在");
    if (order.getStatus() != 0) return Result.error("只能取消待付款订单");

    List<ShopOrderItem> items = orderItemService.list(
            new LambdaQueryWrapper<ShopOrderItem>().eq(ShopOrderItem::getOrderId, orderId));
    items.forEach(item -> {
        // 恢复库存
        ShopProduct p = productService.getById(item.getProductId());
        if (p != null) {
            p.setStock(p.getStock() + item.getQuantity());
            p.setSales(Math.max(0, p.getSales() - item.getQuantity()));
            productService.updateById(p);
        }
        item.setItemStatus(3); // ✨ 标记已取消
    });
    orderItemService.updateBatchById(items);

    order.setStatus(4);
    return orderService.updateById(order) ? Result.success("已取消") : Result.error("取消失败");
}

    // 管理员发货
//    @PostMapping("/order/ship")
//    public Result<String> shipOrder(@RequestParam String orderId) {
//        ShopOrder order = orderService.getById(orderId);
//        if (order == null) return Result.error("订单不存在");
//        if (order.getStatus() != 1) return Result.error("订单未付款");
//        order.setStatus(2);
//        order.setShipTime(LocalDateTime.now());
//        return orderService.updateById(order) ? Result.success("发货成功") : Result.error("发货失败");
//    }
    // 管理员发货（只发本馆商品）
    @PostMapping("/order/ship")
    public Result<String> shipOrder(@RequestParam String orderId,
                                    @RequestParam String belongMuseum) {
        List<ShopOrderItem> items = orderItemService.list(
                new LambdaQueryWrapper<ShopOrderItem>()
                        .eq(ShopOrderItem::getOrderId, orderId)
                        .eq(ShopOrderItem::getBelongMuseum, belongMuseum)
                        .eq(ShopOrderItem::getItemStatus, 0)
        );
        if (items.isEmpty()) return Result.error("无待发货商品");
        items.forEach(item -> {
            item.setItemStatus(1);
            item.setShipTime(LocalDateTime.now());
        });
        orderItemService.updateBatchById(items);
        return Result.success("发货成功");
    }

    // 用户确认收货
//    @PostMapping("/order/finish")
//    public Result<String> finishOrder(@RequestParam String orderId) {
//        ShopOrder order = orderService.getById(orderId);
//        if (order == null) return Result.error("订单不存在");
//        if (order.getStatus() != 2) return Result.error("订单未发货");
//        order.setStatus(3);
//        order.setFinishTime(LocalDateTime.now());
//        return orderService.updateById(order) ? Result.success("确认收货成功") : Result.error("操作失败");
//    }
    @PostMapping("/order/finish")
    public Result<String> finishOrder(@RequestParam String orderId) {
        // 把该订单所有已发货的item改为已完成
        List<ShopOrderItem> shipped = orderItemService.list(
                new LambdaQueryWrapper<ShopOrderItem>()
                        .eq(ShopOrderItem::getOrderId, orderId)
                        .eq(ShopOrderItem::getItemStatus, 1)
        );
        shipped.forEach(item -> {
            item.setItemStatus(2);
            item.setFinishTime(LocalDateTime.now());
        });
        if (!shipped.isEmpty()) orderItemService.updateBatchById(shipped);

        // 检查是否全部完成
        long unfinished = orderItemService.count(
                new LambdaQueryWrapper<ShopOrderItem>()
                        .eq(ShopOrderItem::getOrderId, orderId)
                        .ne(ShopOrderItem::getItemStatus, 2)
                        .ne(ShopOrderItem::getItemStatus, 3)
        );
        if (unfinished == 0) {
            ShopOrder order = orderService.getById(orderId);
            order.setStatus(3);
            order.setFinishTime(LocalDateTime.now());
            orderService.updateById(order);
        }
        return Result.success("确认收货成功");
    }

    // 管理员查询本馆订单
    @GetMapping("/order/admin/list")
    public Result<List<ShopOrder>> adminOrderList(
            @RequestParam String belongMuseum,
            @RequestParam(required = false) Integer status) {
        List<ShopOrderItem> items = orderItemService.list(
                new LambdaQueryWrapper<ShopOrderItem>().eq(ShopOrderItem::getBelongMuseum, belongMuseum));
        if (items.isEmpty()) return Result.success(new ArrayList<>());

        List<String> orderIds = items.stream().map(ShopOrderItem::getOrderId).distinct().collect(Collectors.toList());
        LambdaQueryWrapper<ShopOrder> w = new LambdaQueryWrapper<ShopOrder>()
                .in(ShopOrder::getOrderId, orderIds)
                .eq(status != null, ShopOrder::getStatus, status)
                .orderByDesc(ShopOrder::getCreateTime);
        List<ShopOrder> orders = orderService.list(w);
        orders.forEach(o -> o.setItems(orderItemService.list(
                new LambdaQueryWrapper<ShopOrderItem>().eq(ShopOrderItem::getOrderId, o.getOrderId()))));
        return Result.success(orders);
    }

    // ── 评价 ──────────────────────────────
    @PostMapping("/review/save")
    public Result<String> reviewSave(@RequestBody ShopReview review) {
        ShopOrder order = orderService.getById(review.getOrderId());
        if (order == null || order.getStatus() != 3) return Result.error("订单未完成，不能评价");
        return reviewService.save(review) ? Result.success("评价成功") : Result.error("评价失败");
    }

    @GetMapping("/review/list")
    public Result<List<ShopReview>> reviewList(@RequestParam Long productId) {
        List<ShopReview> reviews = reviewService.list(
                new LambdaQueryWrapper<ShopReview>()
                        .eq(ShopReview::getProductId, productId)
                        .orderByDesc(ShopReview::getCreateTime));
        reviews.forEach(r -> {
            // 补充用户名（可选）
        });
        return Result.success(reviews);
    }

    @Autowired private ShopProductSpecService specService;
    @Autowired private ShopProductSkuService skuService;

    // 查询商品的规格和SKU
    @GetMapping("/product/detail/{productId}")
    public Result<ShopProduct> productDetailWithSku(@PathVariable Long productId) {
        ShopProduct p = productService.getById(productId);
        if (p == null) return Result.error("商品不存在");
        p.setSpecs(specService.list(new LambdaQueryWrapper<ShopProductSpec>()
                .eq(ShopProductSpec::getProductId, productId)));
        p.setSkus(skuService.list(new LambdaQueryWrapper<ShopProductSku>()
                .eq(ShopProductSku::getProductId, productId)));
        return Result.success(p);
    }

    // 保存规格和SKU（先删后插）
    @PostMapping("/product/sku/save")
    public Result<String> saveSkus(@RequestBody Map<String, Object> params) {
        Long productId = Long.valueOf(params.get("productId").toString());

        // 删旧数据
        specService.remove(new LambdaQueryWrapper<ShopProductSpec>()
                .eq(ShopProductSpec::getProductId, productId));
        skuService.remove(new LambdaQueryWrapper<ShopProductSku>()
                .eq(ShopProductSku::getProductId, productId));

        // 保存规格
        List<Map<String, Object>> specs = (List<Map<String, Object>>) params.get("specs");
        if (specs != null) {
            specs.forEach(s -> {
                ShopProductSpec spec = new ShopProductSpec();
                spec.setProductId(productId);
                spec.setSpecName(s.get("specName").toString());
                spec.setSpecValues(s.get("specValues").toString());
                specService.save(spec);
            });
        }

        // 保存SKU
        List<Map<String, Object>> skus = (List<Map<String, Object>>) params.get("skus");
        if (skus != null) {
            skus.forEach(s -> {
                ShopProductSku sku = new ShopProductSku();
                sku.setProductId(productId);
                sku.setSkuName(s.get("skuName").toString());
                sku.setPrice(new BigDecimal(s.get("price").toString()));
                sku.setStock(Integer.parseInt(s.get("stock").toString()));
                sku.setStockWarning(Integer.parseInt(s.getOrDefault("stockWarning", "10").toString()));
                // ✨ 加这段
                if (s.get("coverUrl") != null && !s.get("coverUrl").toString().isEmpty()) {
                    sku.setCoverUrl(s.get("coverUrl").toString());
                }
                skuService.save(sku);
            });
        }
        return Result.success("保存成功");
    }

    // 用户申请退款
    @PostMapping("/order/refund/apply")
    public Result<String> applyRefund(@RequestBody Map<String, Object> params) {
        String orderId = params.get("orderId").toString();
        String reason = params.getOrDefault("reason", "").toString();
        ShopOrder order = orderService.getById(orderId);
        if (order == null) return Result.error("订单不存在");
        if (order.getStatus() != 1 && order.getStatus() != 2)
            return Result.error("当前订单状态不支持退款");
        order.setPrevStatus(order.getStatus()); // 先保存再改状态
        order.setStatus(5);
        order.setRefundReason(reason);
        return orderService.updateById(order) ? Result.success("退款申请已提交") : Result.error("提交失败");
    }

    // 管理员同意退款
    @PostMapping("/order/refund/approve")
    public Result<String> approveRefund(@RequestBody Map<String, Object> params) {
        String orderId = params.get("orderId").toString();
        String remark = params.getOrDefault("remark", "").toString();
        ShopOrder order = orderService.getById(orderId);
        if (order == null) return Result.error("订单不存在");
        if (order.getStatus() != 5) return Result.error("该订单不在退款中状态");

        // 恢复库存
        List<ShopOrderItem> items = orderItemService.list(
                new LambdaQueryWrapper<ShopOrderItem>().eq(ShopOrderItem::getOrderId, orderId));
        items.forEach(item -> {
            ShopProduct p = productService.getById(item.getProductId());
            if (p != null) {
                p.setStock(p.getStock() + item.getQuantity());
                p.setSales(Math.max(0, p.getSales() - item.getQuantity()));
                productService.updateById(p);
            }
        });

        order.setStatus(6);
        order.setRefundRemark(remark);
        order.setRefundTime(LocalDateTime.now());
        return orderService.updateById(order) ? Result.success("退款成功") : Result.error("操作失败");
    }

    // 管理员拒绝退款
    @PostMapping("/order/refund/reject")
    public Result<String> rejectRefund(@RequestBody Map<String, Object> params) {
        String orderId = params.get("orderId").toString();
        String remark = params.getOrDefault("remark", "").toString();
        Integer prevStatus = Integer.valueOf(params.get("prevStatus").toString());
        ShopOrder order = orderService.getById(orderId);
        if (order == null) return Result.error("订单不存在");
        if (order.getStatus() != 5) return Result.error("该订单不在退款中状态");
        order.setStatus(prevStatus); // 恢复原状态
        order.setRefundRemark(remark);
        order.setRefundTime(LocalDateTime.now());
        return orderService.updateById(order) ? Result.success("已拒绝退款") : Result.error("操作失败");
    }
}