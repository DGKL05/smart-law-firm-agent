package com.example.dgkl.lawyer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.lawyer.client.AppointmentClient;
import com.example.dgkl.lawyer.client.AuthUserClient;
import com.example.dgkl.lawyer.dto.LawyerAdminRequest;
import com.example.dgkl.lawyer.dto.LawyerProfileRequest;
import com.example.dgkl.lawyer.dto.LawyerResponse;
import com.example.dgkl.lawyer.dto.LawyerSnapshotResponse;
import com.example.dgkl.lawyer.entity.LawFirm;
import com.example.dgkl.lawyer.entity.Lawyer;
import com.example.dgkl.lawyer.mapper.LawFirmMapper;
import com.example.dgkl.lawyer.mapper.LawyerMapper;
import com.example.dgkl.lawyer.service.LawyerMediaService;
import com.example.dgkl.lawyer.service.LawyerService;
import com.example.dgkl.lawyer.service.PublicDirectoryCache;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LawyerServiceImpl implements LawyerService {
    private static final int ENABLED = 1;

    private final LawyerMapper lawyerMapper;
    private final LawFirmMapper lawFirmMapper;
    private final AuthUserClient authUserClient;
    private final AppointmentClient appointmentClient;
    private final LawyerMediaService mediaService;
    private final PublicDirectoryCache directoryCache;

    @Override
    public PageResult<LawyerResponse> list(int pageNum, int pageSize, String keyword, Long lawFirmId) {
        LambdaQueryWrapper<Lawyer> query = Wrappers.lambdaQuery(Lawyer.class)
                .eq(lawFirmId != null, Lawyer::getLawFirmId, lawFirmId)
                .orderByDesc(Lawyer::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(condition -> condition.like(Lawyer::getName, keyword)
                    .or().like(Lawyer::getCategory, keyword)
                    .or().like(Lawyer::getGoodAt, keyword));
        }
        Page<Lawyer> page = lawyerMapper.selectPage(Page.of(pageNum, pageSize), query);
        List<Long> firmIds = page.getRecords().stream().map(Lawyer::getLawFirmId).distinct().toList();
        Map<Long, String> firmNames = firmIds.isEmpty() ? Map.of() : lawFirmMapper.selectByIds(firmIds).stream()
                .collect(Collectors.toMap(LawFirm::getId, LawFirm::getName));
        List<LawyerResponse> records = page.getRecords().stream()
                .map(lawyer -> LawyerResponse.of(lawyer, firmNames.get(lawyer.getLawFirmId()), List.of()))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public LawyerResponse get(Long id) {
        return toResponse(requireLawyer(lawyerMapper.selectById(id)));
    }

    @Override
    @Transactional
    public LawyerResponse create(LawyerAdminRequest request) {
        validateBinding(request.getUserId());
        requireFirmForUpdate(request.getLawFirmId());
        Lawyer lawyer = new Lawyer();
        BeanUtils.copyProperties(request, lawyer);
        lawyer.setName(request.getName().strip());
        lawyer.setExperienceYears(experienceYears(request));
        lawyer.setStatus(Objects.requireNonNullElse(request.getStatus(), ENABLED));
        try {
            lawyerMapper.insert(lawyer);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "该账号已绑定其他律师");
        }
        directoryCache.invalidateLawyer(lawyer.getId());
        return get(lawyer.getId());
    }

    @Override
    @Transactional
    public LawyerResponse update(Long id, LawyerAdminRequest request) {
        validateBinding(request.getUserId());
        Lawyer lawyer = lockLawyer(id);
        if (!Objects.equals(lawyer.getUserId(), request.getUserId())
                || !Objects.equals(lawyer.getLawFirmId(), request.getLawFirmId())) {
            requireNoActiveWork(id);
        }
        requireFirmForUpdate(request.getLawFirmId());
        LambdaUpdateWrapper<Lawyer> update = profileChanges(id, request)
                .set(Lawyer::getLawFirmId, request.getLawFirmId())
                .set(Lawyer::getUserId, request.getUserId())
                .set(request.getStatus() != null, Lawyer::getStatus, request.getStatus());
        try {
            lawyerMapper.update(null, update);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "该账号已绑定其他律师");
        }
        directoryCache.invalidateLawyer(id);
        return get(lawyer.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Lawyer lawyer = lockLawyer(id);
        if (Integer.valueOf(ENABLED).equals(lawyer.getStatus())) {
            throw new BusinessException(409, "请先停用律师档案，再处理在途业务");
        }
        requireNoActiveWork(id);
        lawyerMapper.deleteById(id);
        mediaService.remove(lawyer.getAvatarUrl());
        directoryCache.invalidateLawyer(id);
    }

    @Override
    public LawyerResponse getProfile(Long userId) {
        return toResponse(requireBoundLawyer(userId, false));
    }

    @Override
    @Transactional
    public LawyerResponse updateProfile(Long userId, LawyerProfileRequest request) {
        Lawyer lawyer = requireBoundLawyer(userId, true);
        lawyerMapper.update(null, profileChanges(lawyer.getId(), request));
        directoryCache.invalidateLawyer(lawyer.getId());
        return get(lawyer.getId());
    }

    @Override
    @Transactional
    public String uploadAvatar(Long id, MultipartFile file) {
        return storeAvatar(lockLawyer(id), file);
    }

    @Override
    @Transactional
    public String uploadProfileAvatar(Long userId, MultipartFile file) {
        return storeAvatar(requireBoundLawyer(userId, true), file);
    }

    @Override
    public LawyerSnapshotResponse snapshot(Long id) {
        Lawyer lawyer = requireLawyer(lawyerMapper.selectById(id));
        return LawyerSnapshotResponse.of(lawyer, firmName(lawyer));
    }

    @Override
    public LawyerSnapshotResponse snapshotByUserId(Long userId) {
        Lawyer lawyer = requireBoundLawyer(userId, false);
        return LawyerSnapshotResponse.of(lawyer, firmName(lawyer));
    }

    @Override
    public Map<String, Long> stats() {
        return Map.of("lawFirmCount", lawFirmMapper.selectCount(null), "lawyerCount", lawyerMapper.selectCount(null));
    }

    private Lawyer lockLawyer(Long id) {
        return requireLawyer(lawyerMapper.selectOne(Wrappers.lambdaQuery(Lawyer.class)
                .eq(Lawyer::getId, id).last("FOR UPDATE")));
    }

    private Lawyer requireBoundLawyer(Long userId, boolean lock) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        LambdaQueryWrapper<Lawyer> query = Wrappers.lambdaQuery(Lawyer.class).eq(Lawyer::getUserId, userId);
        if (lock) {
            query.last("FOR UPDATE");
        }
        Lawyer lawyer = lawyerMapper.selectOne(query);
        if (lawyer == null) {
            throw new BusinessException(404, "当前账号未绑定律师资料");
        }
        return lawyer;
    }

    private Lawyer requireLawyer(Lawyer lawyer) {
        if (lawyer == null) {
            throw new BusinessException(404, "律师不存在");
        }
        return lawyer;
    }

    private void requireFirmForUpdate(Long id) {
        if (lawFirmMapper.selectForUpdate(id) == null) {
            throw new BusinessException(400, "所属律所不存在");
        }
    }

    private void validateBinding(Long userId) {
        if (userId == null) {
            return;
        }
        ApiResponse<Boolean> result;
        try {
            result = authUserClient.exists(userId);
        } catch (FeignException exception) {
            log.warn("校验律师绑定账号失败，userId={}，下游状态={}", userId, exception.status());
            throw new BusinessException(503, "暂时无法校验账号，请稍后重试");
        }
        if (result == null || result.getCode() != 200) {
            throw new BusinessException(503, "暂时无法校验账号，请稍后重试");
        }
        if (!Boolean.TRUE.equals(result.getData())) {
            throw new BusinessException("账号不存在或已停用");
        }
    }

    private void requireNoActiveWork(Long lawyerId) {
        ApiResponse<Boolean> result;
        try {
            result = appointmentClient.hasActiveWork(lawyerId);
        } catch (FeignException exception) {
            log.warn("校验律师在途业务失败，lawyerId={}，下游状态={}", lawyerId, exception.status());
            throw new BusinessException(503, "暂时无法确认在途业务，请稍后重试");
        }
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new BusinessException(503, "暂时无法确认在途业务，请稍后重试");
        }
        if (result.getData()) {
            throw new BusinessException(409, "请先处理该律师的预约、案件和咨询");
        }
    }

    private LambdaUpdateWrapper<Lawyer> profileChanges(Long id, LawyerProfileRequest request) {
        return Wrappers.lambdaUpdate(Lawyer.class)
                .eq(Lawyer::getId, id)
                .set(Lawyer::getName, request.getName().strip())
                .set(Lawyer::getGender, request.getGender())
                .set(Lawyer::getPhone, request.getPhone())
                .set(Lawyer::getEmail, request.getEmail())
                .set(Lawyer::getCategory, request.getCategory())
                .set(Lawyer::getTitle, request.getTitle())
                .set(Lawyer::getExperienceYears, experienceYears(request))
                .set(Lawyer::getDescription, request.getDescription())
                .set(Lawyer::getGoodAt, request.getGoodAt());
    }

    private Integer experienceYears(LawyerProfileRequest request) {
        return Objects.requireNonNullElse(request.getExperienceYears(), 0);
    }

    private String storeAvatar(Lawyer lawyer, MultipartFile file) {
        String url = mediaService.replace(lawyer.getAvatarUrl(), file);
        lawyerMapper.update(null, Wrappers.lambdaUpdate(Lawyer.class)
                .eq(Lawyer::getId, lawyer.getId()).set(Lawyer::getAvatarUrl, url));
        directoryCache.invalidateLawyer(lawyer.getId());
        return url;
    }

    private LawyerResponse toResponse(Lawyer lawyer) {
        return LawyerResponse.of(lawyer, firmName(lawyer), List.of());
    }

    private String firmName(Lawyer lawyer) {
        LawFirm firm = lawFirmMapper.selectById(lawyer.getLawFirmId());
        return firm == null ? null : firm.getName();
    }
}
