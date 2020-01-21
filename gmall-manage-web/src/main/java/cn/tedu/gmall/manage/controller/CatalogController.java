package cn.tedu.gmall.manage.controller;

import cn.tedu.gmall.bean.PmsBaseCatalog1;
import cn.tedu.gmall.bean.PmsBaseCatalog2;
import cn.tedu.gmall.bean.PmsBaseCatalog3;
import cn.tedu.gmall.service.CatalogService;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@CrossOrigin
public class CatalogController {

    @Reference
    CatalogService catalogService;

    @RequestMapping("getCatalog1")
    @ResponseBody
    public List<PmsBaseCatalog1> getCatalog1() {
        List<PmsBaseCatalog1>  catalog1s =catalogService.getCatalog1();
        return catalog1s;
    }
    @RequestMapping("getCatalog2")
    @ResponseBody
    public List<PmsBaseCatalog2> getCatalog2(String catalog1Id) {
        List<PmsBaseCatalog2>  catalog2s =catalogService.getCatalog2(catalog1Id);
        return catalog2s;
    }
    @RequestMapping("getCatalog3")
    @ResponseBody
    public List<PmsBaseCatalog3> getCatalog3(String catalog2Id) {
        List<PmsBaseCatalog3>  catalog3s =catalogService.getCatalog3(catalog2Id);
        return catalog3s;
    }
}
