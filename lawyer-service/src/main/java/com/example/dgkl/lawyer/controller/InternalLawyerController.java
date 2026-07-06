package com.example.dgkl.lawyer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.lawyer.dto.LawyerSnapshotResponse;
import com.example.dgkl.lawyer.entity.LawFirm;
import com.example.dgkl.lawyer.entity.Lawyer;
import com.example.dgkl.lawyer.mapper.LawFirmMapper;
import com.example.dgkl.lawyer.mapper.LawyerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/lawyers")
@RequiredArgsConstructor
public class InternalLawyerController {
    private final LawyerMapper lawyerMapper;
    private final LawFirmMapper lawFirmMapper;

    @GetMapping("/{id}/snapshot")
    public ApiResponse<LawyerSnapshotResponse> snapshot(@PathVariable Long id) {
        return ApiResponse.success(toSnapshot(requireLawyer(lawyerMapper.selectById(id))));
    }

    @GetMapping("/by-user/{userId}/snapshot")
    public ApiResponse<LawyerSnapshotResponse> snapshotByUserId(@PathVariable Long userId) {
        Lawyer lawyer = lawyerMapper.selectOne(new LambdaQueryWrapper<Lawyer>()
                .eq(Lawyer::getUserId, userId)
                .eq(Lawyer::getDeleted, 0));
        return ApiResponse.success(toSnapshot(requireLawyer(lawyer)));
    }

    private Lawyer requireLawyer(Lawyer lawyer) {
        if (lawyer == null || Integer.valueOf(1).equals(lawyer.getDeleted())) {
            throw new BusinessException("律师不存在");
        }
        return lawyer;
    }

    private LawyerSnapshotResponse toSnapshot(Lawyer lawyer) {
        LawFirm firm = lawFirmMapper.selectById(lawyer.getLawFirmId());
        return LawyerSnapshotResponse.of(lawyer, firm == null ? null : firm.getName());
    }
}
