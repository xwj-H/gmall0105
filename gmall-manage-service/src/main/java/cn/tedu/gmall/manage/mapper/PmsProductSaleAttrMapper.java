package cn.tedu.gmall.manage.mapper;

import cn.tedu.gmall.bean.PmsProductSaleAttr;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface PmsProductSaleAttrMapper extends Mapper<PmsProductSaleAttr> {
    List<PmsProductSaleAttr> selectSpuSaleAttrListcheckBySku(@Param("productId") String productId, @Param("skuId") String skuId);
}
