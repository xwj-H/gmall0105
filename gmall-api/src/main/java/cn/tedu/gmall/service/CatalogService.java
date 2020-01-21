package cn.tedu.gmall.service;

import cn.tedu.gmall.bean.PmsBaseCatalog1;
import cn.tedu.gmall.bean.PmsBaseCatalog2;
import cn.tedu.gmall.bean.PmsBaseCatalog3;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CatalogService {

    List<PmsBaseCatalog1> getCatalog1();

    List<PmsBaseCatalog2> getCatalog2(String catalog1Id);

    List<PmsBaseCatalog3> getCatalog3(String catalog2Id);
}
