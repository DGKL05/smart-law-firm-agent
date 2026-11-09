package com.example.dgkl.lawyer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.dgkl.lawyer.entity.LawFirm;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LawFirmMapper extends BaseMapper<LawFirm> {
    default LawFirm selectForUpdate(Long id) {
        return selectOne(Wrappers.lambdaQuery(LawFirm.class).eq(LawFirm::getId, id).last("FOR UPDATE"));
    }
}
