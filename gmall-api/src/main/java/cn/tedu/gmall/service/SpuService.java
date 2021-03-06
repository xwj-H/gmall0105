package cn.tedu.gmall.service;

import cn.tedu.gmall.bean.PmsBaseSaleAttr;
import cn.tedu.gmall.bean.PmsProductImage;
import cn.tedu.gmall.bean.PmsProductInfo;
import cn.tedu.gmall.bean.PmsProductSaleAttr;

import java.util.List;

public interface SpuService {
    List<PmsProductInfo> spuList(String catalog3Id);

    void saveSpuInfo(PmsProductInfo pmsProductInfo);

    List<PmsProductSaleAttr> spuSaleAttrList(String spuId);

    List<PmsProductImage> spuImageList(String spuId);

    List<PmsProductSaleAttr> spuSaleAttrListcheckBySku(String productId, String skuId);
}
