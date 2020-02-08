package cn.tedu.gmall.item.controller;

import cn.tedu.gmall.bean.PmsProductSaleAttr;
import cn.tedu.gmall.bean.PmsSkuInfo;
import cn.tedu.gmall.bean.PmsSkuSaleAttrValue;
import cn.tedu.gmall.service.SkuService;
import cn.tedu.gmall.service.SpuService;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@CrossOrigin
public class ItemController {

    @Reference
    SpuService spuService;

    @Reference
    SkuService skuService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap modelMap, HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        //request.getHeader("");//负载均衡

        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId, remoteAddr);
        //sku对象
        modelMap.put("skuInfo", pmsSkuInfo);

        List<PmsProductSaleAttr> pmsProductSaleAttrs = spuService.spuSaleAttrListcheckBySku(pmsSkuInfo.getProductId()
                , pmsSkuInfo.getSpuId());
        //销售属性列表
        modelMap.put("spuSaleAttrListcheckBySku", pmsSkuInfo);
        //查询当前sku的spu的其他sku的集合的hash表
        Map<String, String> skuSaleAttrHash = new HashMap<>();
        List<PmsSkuInfo> pmsSkuInfos = skuService.getSkuSaleAttrValueListBySpu(pmsSkuInfo.getProductId());
        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            String k = "";
            String v = skuInfo.getId();
            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                k += pmsSkuSaleAttrValue.getSaleAttrValueId() + "|";

            }
            skuSaleAttrHash.put(k, v);
        }
        String skuSaleAttrHashJsonStr = JSON.toJSONString(skuSaleAttrHash);
        modelMap.put("skuSaleAttrHashJsonStr", skuSaleAttrHashJsonStr);

        return "item";
    }

    @RequestMapping("index")
    public String index(ModelMap modelMap) {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        modelMap.put("list", list);
        return "index";
    }
}
