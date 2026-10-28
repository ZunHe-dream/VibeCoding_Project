package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.Result;
import org.example.entity.*;
import org.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/shop/dashboard")
public class ShopDashboardController {

    @Autowired private ShopOrderService orderService;
    @Autowired private ShopOrderItemService orderItemService;
    @Autowired private ShopProductService productService;
    @Autowired private ShopCategoryService categoryService;

    /**
     * KPI 汇总：今日营收、本周营收、成交订单数、客单价
     */
    @GetMapping("/kpi")
    public Result<Map<String, Object>> kpi(@RequestParam String belongMuseum) {
        // 取该馆所有已付款及以上订单的 item
        List<ShopOrderItem> allItems = orderItemService.list(
                new LambdaQueryWrapper<ShopOrderItem>()
                        .eq(ShopOrderItem::getBelongMuseum, belongMuseum));

        // 取所有有效订单（status >= 1，已付款）
        List<String> validOrderIds = allItems.stream()
                .map(ShopOrderItem::getOrderId).distinct().collect(Collectors.toList());

        List<ShopOrder> validOrders = validOrderIds.isEmpty() ? new ArrayList<>() :
                orderService.list(new LambdaQueryWrapper<ShopOrder>()
                        .in(ShopOrder::getOrderId, validOrderIds)
                        .ge(ShopOrder::getStatus, 1)); // 已付款及以上

        // 今日
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);
        List<ShopOrder> todayOrders = validOrders.stream()
                .filter(o -> o.getPayTime() != null
                        && !o.getPayTime().isBefore(todayStart)
                        && !o.getPayTime().isAfter(todayEnd))
                .collect(Collectors.toList());

        // 本周
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        List<ShopOrder> weekOrders = validOrders.stream()
                .filter(o -> o.getPayTime() != null && !o.getPayTime().isBefore(weekStart))
                .collect(Collectors.toList());

        BigDecimal todayRevenue = todayOrders.stream()
                .map(ShopOrder::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weekRevenue = weekOrders.stream()
                .map(ShopOrder::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

        int orderCount = validOrders.size();
        BigDecimal aov = orderCount > 0
                ? weekRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayRevenue", todayRevenue);
        result.put("weekRevenue", weekRevenue);
        result.put("orderCount", orderCount);
        result.put("aov", aov);
        return Result.success(result);
    }

    /**
     * 热门商品排行：销量 Top10 / 销售额 Top10
     */
    @GetMapping("/top-products")
    public Result<Map<String, Object>> topProducts(@RequestParam String belongMuseum) {
        List<ShopOrderItem> items = orderItemService.list(
                new LambdaQueryWrapper<ShopOrderItem>()
                        .eq(ShopOrderItem::getBelongMuseum, belongMuseum));

        // 按商品聚合
        Map<Long, String> nameMap = new HashMap<>();
        Map<Long, String> coverMap = new HashMap<>();
        Map<Long, Integer> qtyMap = new HashMap<>();
        Map<Long, BigDecimal> revenueMap = new HashMap<>();

        for (ShopOrderItem item : items) {
            Long pid = item.getProductId();
            nameMap.put(pid, item.getProductName());
            coverMap.put(pid, item.getCoverUrl());
            qtyMap.merge(pid, item.getQuantity(), Integer::sum);
            revenueMap.merge(pid, item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())), BigDecimal::add);
        }

        // 销量 Top10
        List<Map<String, Object>> topSales = qtyMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productId", e.getKey());
                    m.put("name", nameMap.get(e.getKey()));
                    m.put("coverUrl", coverMap.get(e.getKey()));
                    m.put("quantity", e.getValue());
                    m.put("revenue", revenueMap.get(e.getKey()));
                    return m;
                }).collect(Collectors.toList());

        // 销售额 Top10
        List<Map<String, Object>> topRevenue = revenueMap.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productId", e.getKey());
                    m.put("name", nameMap.get(e.getKey()));
                    m.put("coverUrl", coverMap.get(e.getKey()));
                    m.put("quantity", qtyMap.get(e.getKey()));
                    m.put("revenue", e.getValue());
                    return m;
                }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("topSales", topSales);
        result.put("topRevenue", topRevenue);
        return Result.success(result);
    }

    /**
     * 分类占比（按销量）
     */
    @GetMapping("/category-analysis")
    public Result<List<Map<String, Object>>> categoryAnalysis(@RequestParam String belongMuseum) {
        List<ShopOrderItem> items = orderItemService.list(
                new LambdaQueryWrapper<ShopOrderItem>()
                        .eq(ShopOrderItem::getBelongMuseum, belongMuseum));

        Set<Long> productIds = items.stream().map(ShopOrderItem::getProductId).collect(Collectors.toSet());
        if (productIds.isEmpty()) return Result.success(new ArrayList<>());

        List<ShopProduct> products = productService.list(
                new LambdaQueryWrapper<ShopProduct>().in(ShopProduct::getProductId, productIds));
        Map<Long, Long> productCatMap = products.stream()
                .collect(Collectors.toMap(ShopProduct::getProductId, p -> p.getCatId() != null ? p.getCatId() : 0L));

        List<ShopCategory> cats = categoryService.list();
        Map<Long, String> catNameMap = cats.stream()
                .collect(Collectors.toMap(ShopCategory::getCatId, ShopCategory::getName));

        Map<Long, Integer> catQtyMap = new HashMap<>();
        for (ShopOrderItem item : items) {
            Long catId = productCatMap.getOrDefault(item.getProductId(), 0L);
            catQtyMap.merge(catId, item.getQuantity(), Integer::sum);
        }

        int total = catQtyMap.values().stream().mapToInt(Integer::intValue).sum();
        return Result.success(catQtyMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("catId", e.getKey());
                    m.put("name", catNameMap.getOrDefault(e.getKey(), "未分类"));
                    m.put("quantity", e.getValue());
                    m.put("percent", total > 0
                            ? BigDecimal.valueOf(e.getValue() * 100.0 / total).setScale(1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO);
                    return m;
                }).collect(Collectors.toList()));
    }

    /**
     * 库存预警商品列表
     */
    @GetMapping("/low-stock")
    public Result<List<ShopProduct>> lowStock(@RequestParam String belongMuseum) {
        List<ShopProduct> list = productService.list(
                new LambdaQueryWrapper<ShopProduct>()
                        .eq(ShopProduct::getBelongMuseum, belongMuseum)
                        .eq(ShopProduct::getStatus, 1)
                        .apply("stock <= stock_warning")
                        .orderByAsc(ShopProduct::getStock));
        return Result.success(list);
    }

    /**
     * 最近10笔订单流水
     */
    @GetMapping("/recent-orders")
    public Result<List<Map<String, Object>>> recentOrders(@RequestParam String belongMuseum) {
        List<ShopOrderItem> items = orderItemService.list(
                new LambdaQueryWrapper<ShopOrderItem>()
                        .eq(ShopOrderItem::getBelongMuseum, belongMuseum));
        if (items.isEmpty()) return Result.success(new ArrayList<>());

        List<String> orderIds = items.stream().map(ShopOrderItem::getOrderId).distinct().collect(Collectors.toList());
        List<ShopOrder> orders = orderService.list(
                new LambdaQueryWrapper<ShopOrder>()
                        .in(ShopOrder::getOrderId, orderIds)
                        .ge(ShopOrder::getStatus, 1)
                        .orderByDesc(ShopOrder::getPayTime)
                        .last("LIMIT 10"));

        Map<String, List<ShopOrderItem>> itemsByOrder = items.stream()
                .collect(Collectors.groupingBy(ShopOrderItem::getOrderId));

        return Result.success(orders.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderId", o.getOrderId());
            m.put("receiver", o.getReceiver());
            m.put("address", o.getAddress() != null && o.getAddress().length() > 4
                    ? o.getAddress().substring(0, 4) : o.getAddress());
            m.put("totalPrice", o.getTotalPrice());
            m.put("payTime", o.getPayTime());
            List<ShopOrderItem> ois = itemsByOrder.getOrDefault(o.getOrderId(), new ArrayList<>());
            m.put("productName", ois.isEmpty() ? "" : ois.get(0).getProductName()
                    + (ois.size() > 1 ? " 等" + ois.size() + "件" : ""));
            m.put("coverUrl", ois.isEmpty() ? "" : ois.get(0).getCoverUrl());
            return m;
        }).collect(Collectors.toList()));
    }

    /**
     * 近7天每日营收趋势
     */
    @GetMapping("/revenue-trend")
    public Result<List<Map<String, Object>>> revenueTrend(@RequestParam String belongMuseum) {
        List<ShopOrderItem> allItems = orderItemService.list(
                new LambdaQueryWrapper<ShopOrderItem>()
                        .eq(ShopOrderItem::getBelongMuseum, belongMuseum));
        if (allItems.isEmpty()) return Result.success(new ArrayList<>());

        List<String> orderIds = allItems.stream().map(ShopOrderItem::getOrderId).distinct().collect(Collectors.toList());
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();

        List<ShopOrder> orders = orderService.list(
                new LambdaQueryWrapper<ShopOrder>()
                        .in(ShopOrder::getOrderId, orderIds)
                        .ge(ShopOrder::getStatus, 1)
                        .ge(ShopOrder::getPayTime, sevenDaysAgo));

        Map<LocalDate, BigDecimal> dailyRevenue = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            dailyRevenue.put(LocalDate.now().minusDays(i), BigDecimal.ZERO);
        }
        for (ShopOrder o : orders) {
            if (o.getPayTime() != null) {
                LocalDate d = o.getPayTime().toLocalDate();
                dailyRevenue.merge(d, o.getTotalPrice(), BigDecimal::add);
            }
        }

        return Result.success(dailyRevenue.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", e.getKey().toString());
            m.put("revenue", e.getValue());
            return m;
        }).collect(Collectors.toList()));
    }
}