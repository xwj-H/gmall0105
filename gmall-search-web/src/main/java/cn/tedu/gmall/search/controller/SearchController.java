package cn.tedu.gmall.search.controller;

import cn.tedu.gmall.bean.PmsBaseAttrInfo;
import cn.tedu.gmall.bean.PmsSearchParam;
import cn.tedu.gmall.bean.PmsSearchSkuInfo;
import cn.tedu.gmall.bean.PmsSkuAttrValue;
import cn.tedu.gmall.service.AttrService;
import cn.tedu.gmall.service.SearchService;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;

    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap modelMap) {//三级分类Id,关键字、平台属性集合

        //调用搜索服务，返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = searchService.list(pmsSearchParam);

        //抽取检索结果所包含的平台属性集合
        Set<String> valueIdStr = new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : pmsSkuAttrValues) {
                String valueId = pmsSkuAttrValue.getValueId();
                valueIdStr.add(valueId);
            }
        }
        //根据属性值Id将属性查出来，调用attrService通过id查出属性值
        List<PmsBaseAttrInfo> pmsBaseAttrInfos=attrService.getAttrValueListByValueId(valueIdStr);

        modelMap.put("skuLsInfoList", pmsSearchSkuInfos);
        modelMap.put("attrList", pmsBaseAttrInfos);
        return "list";
    }

    @RequestMapping("index")
    public String index() {

        return "index";
    }
}
