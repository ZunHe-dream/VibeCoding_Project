package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.ShopProduct;

import java.util.List;

public interface ShopProductService extends IService<ShopProduct> {
    List<ShopProduct> search(String keyword, Long catId, String museum, Integer type, String sort);
}