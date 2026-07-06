package com.example.dgkl.lawyer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.security.SecurityUtils;
import com.example.dgkl.lawyer.entity.Lawyer;
import com.example.dgkl.lawyer.mapper.LawyerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lawyer-center")
@RequiredArgsConstructor
public class LawyerCenterController {
    private final LawyerMapper lawyerMapper;

    @GetMapping("/profile")
    public ApiResponse<Lawyer> profile() {
        Long userId = SecurityUtils.currentUserId();
        Lawyer lawyer = lawyerMapper.selectOne(new LambdaQueryWrapper<Lawyer>().eq(Lawyer::getUserId, userId).eq(Lawyer::getDeleted, 0));
        if (lawyer == null) {
            throw new BusinessException("当前账号未绑定律师资料");
        }
        return ApiResponse.success(lawyer);
    }
}
