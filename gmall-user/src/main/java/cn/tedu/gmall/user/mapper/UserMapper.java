package cn.tedu.gmall.user.mapper;

import cn.tedu.gmall.user.bean.UmsMember;
import tk.mybatis.mapper.common.Mapper;

import javax.annotation.Resource;
import java.util.List;
@Resource
public interface UserMapper extends Mapper<UmsMember> {
    List<UmsMember> selectAllUser();
}
