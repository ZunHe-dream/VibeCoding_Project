package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ShopCategory;
import org.example.entity.ShopProduct;
import org.example.entity.ShopProductSku;
import org.example.mapper.ShopProductMapper;
import org.example.service.ShopCategoryService;
import org.example.service.ShopProductService;
import org.example.service.ShopProductSkuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
public class ShopProductServiceImpl extends ServiceImpl<ShopProductMapper, ShopProduct>
        implements ShopProductService {
    @Autowired
    private ShopCategoryService categoryService;
    @Autowired private ShopProductSkuService skuService;
//    @Override
//    public List<ShopProduct> search(String keyword, Long catId, String museum, Integer type, String sort) {
//        LambdaQueryWrapper<ShopProduct> w = new LambdaQueryWrapper<>();
//        w.eq(ShopProduct::getStatus, 1); // 只查上架
//        w.like(StringUtils.hasText(keyword), ShopProduct::getName, keyword);
//        w.eq(catId != null, ShopProduct::getCatId, catId);
//        w.eq(StringUtils.hasText(museum), ShopProduct::getBelongMuseum, museum);
//        w.eq(type != null, ShopProduct::getType, type);
//        if ("price_asc".equals(sort)) w.orderByAsc(ShopProduct::getPrice);
//        else if ("price_desc".equals(sort)) w.orderByDesc(ShopProduct::getPrice);
//        else w.orderByDesc(ShopProduct::getSales);
//
//        List<ShopProduct> list = this.list(w);
//        // 补充分类名和库存预警
//        list.forEach(p -> {
//            if (p.getCatId() != null) {
//                ShopCategory cat = categoryService.getById(p.getCatId());
//                if (cat != null) p.setCatName(cat.getName());
//            }
//            p.setLowStock(p.getStock() <= p.getStockWarning());
//
//            // ✨ 查SKU取最低价
//            List<ShopProductSku> skus = skuService.list(
//                    new LambdaQueryWrapper<ShopProductSku>()
//                            .eq(ShopProductSku::getProductId, p.getProductId())
//                            .orderByAsc(ShopProductSku::getPrice)
//            );
//            if (!skus.isEmpty()) {
//                p.setPrice(skus.get(0).getPrice());
//                p.setHasMultiSku(skus.size() > 1);
//            }
//        });
//        return list;
//    }
    @Override
    public List<ShopProduct> search(String keyword, Long catId, String museum, Integer type, String sort) {
        LambdaQueryWrapper<ShopProduct> w = new LambdaQueryWrapper<>();
        w.eq(ShopProduct::getStatus, 1);
        w.like(StringUtils.hasText(keyword), ShopProduct::getName, keyword);
        w.eq(catId != null, ShopProduct::getCatId, catId);
        w.eq(StringUtils.hasText(museum), ShopProduct::getBelongMuseum, museum);
        w.eq(type != null, ShopProduct::getType, type);
        // ✨ 去掉原来的数据库排序，改在后面Java里排
        if (!"price_asc".equals(sort) && !"price_desc".equals(sort)) {
            w.orderByDesc(ShopProduct::getSales); // 非价格排序直接DB排
        }

        List<ShopProduct> list = this.list(w);

        list.forEach(p -> {
            if (p.getCatId() != null) {
                ShopCategory cat = categoryService.getById(p.getCatId());
                if (cat != null) p.setCatName(cat.getName());
            }
            p.setLowStock(p.getStock() <= p.getStockWarning());

            List<ShopProductSku> skus = skuService.list(
                    new LambdaQueryWrapper<ShopProductSku>()
                            .eq(ShopProductSku::getProductId, p.getProductId())
                            .orderByAsc(ShopProductSku::getPrice)
            );
            if (!skus.isEmpty()) {
                p.setPrice(skus.get(0).getPrice()); // 替换为SKU最低价
                p.setHasMultiSku(skus.size() > 1);
            }
        });

        // ✨ 替换完价格后再排序
        if ("price_asc".equals(sort)) {
            list.sort(Comparator.comparing(ShopProduct::getPrice));
        } else if ("price_desc".equals(sort)) {
            list.sort(Comparator.comparing(ShopProduct::getPrice).reversed());
        }

        return list;
    }
}