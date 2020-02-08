package cn.tedu.gmall.service;

import cn.tedu.gmall.bean.PmsSearchParam;
import cn.tedu.gmall.bean.PmsSearchSkuInfo;

import java.util.List;

public interface SearchService {
    List<PmsSearchSkuInfo> list(PmsSearchParam pmsSearchParam);
}
