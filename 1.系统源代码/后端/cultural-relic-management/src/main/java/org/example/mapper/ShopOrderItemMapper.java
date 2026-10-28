package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.ShopOrderItem;

@Mapper
public interface ShopOrderItemMapper extends BaseMapper<ShopOrderItem> {}