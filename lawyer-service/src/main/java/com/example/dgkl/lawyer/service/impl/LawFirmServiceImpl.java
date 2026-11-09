package com.example.dgkl.lawyer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.lawyer.dto.LawFirmRequest;
import com.example.dgkl.lawyer.entity.LawFirm;
import com.example.dgkl.lawyer.entity.Lawyer;
import com.example.dgkl.lawyer.mapper.LawFirmMapper;
import com.example.dgkl.lawyer.mapper.LawyerMapper;
import com.example.dgkl.lawyer.service.LawFirmService;
import com.example.dgkl.lawyer.service.LawyerMediaService;
import com.example.dgkl.lawyer.service.PublicDirectoryCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LawFirmServiceImpl implements LawFirmService {
    private static final int ENABLED = 1;

    private final LawFirmMapper lawFirmMapper;
    private final LawyerMapper lawyerMapper;
    private final LawyerMediaService mediaService;
    private final PublicDirectoryCache directoryCache;

    @Override
    public PageResult<LawFirm> list(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<LawFirm> query = Wrappers.lambdaQuery(LawFirm.class).orderByDesc(LawFirm::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(condition -> condition.like(LawFirm::getName, keyword)
                    .or().like(LawFirm::getCity, keyword)
                    .or().like(LawFirm::getSpecialties, keyword)
                    .or().like(LawFirm::getLicenseNo, keyword));
        }
        Page<LawFirm> page = lawFirmMapper.selectPage(Page.of(pageNum, pageSize), query);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public LawFirm get(Long id) {
        return requireFirm(lawFirmMapper.selectById(id));
    }

    @Override
    @Transactional
    public LawFirm create(LawFirmRequest request) {
        LawFirm firm = new LawFirm();
        firm.setName(request.name().strip());
        firm.setProvinceCode(request.provinceCode());
        firm.setProvinceName(request.provinceName());
        firm.setCity(request.city());
        firm.setAddress(request.address());
        firm.setPhone(request.phone());
        firm.setEmail(request.email());
        firm.setSpecialties(request.specialties());
        firm.setDescription(request.description());
        firm.setLicenseNo(request.licenseNo());
        firm.setStatus(Objects.requireNonNullElse(request.status(), ENABLED));
        lawFirmMapper.insert(firm);
        directoryCache.invalidateLawFirm(firm.getId());
        return get(firm.getId());
    }

    @Override
    @Transactional
    public LawFirm update(Long id, LawFirmRequest request) {
        requireFirm(lawFirmMapper.selectForUpdate(id));
        lawFirmMapper.update(null, Wrappers.lambdaUpdate(LawFirm.class)
                .eq(LawFirm::getId, id)
                .set(LawFirm::getName, request.name().strip())
                .set(LawFirm::getProvinceCode, request.provinceCode())
                .set(LawFirm::getProvinceName, request.provinceName())
                .set(LawFirm::getCity, request.city())
                .set(LawFirm::getAddress, request.address())
                .set(LawFirm::getPhone, request.phone())
                .set(LawFirm::getEmail, request.email())
                .set(LawFirm::getSpecialties, request.specialties())
                .set(LawFirm::getDescription, request.description())
                .set(LawFirm::getLicenseNo, request.licenseNo())
                .set(request.status() != null, LawFirm::getStatus, request.status()));
        directoryCache.invalidateLawFirm(id);
        return get(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LawFirm firm = requireFirm(lawFirmMapper.selectForUpdate(id));
        boolean hasLawyers = lawyerMapper.exists(Wrappers.lambdaQuery(Lawyer.class).eq(Lawyer::getLawFirmId, id));
        if (hasLawyers) {
            throw new BusinessException(409, "该律所仍有律师，请先调整律师归属");
        }
        lawFirmMapper.deleteById(id);
        mediaService.remove(firm.getPhotoUrl());
        directoryCache.invalidateLawFirm(id);
    }

    @Override
    @Transactional
    public String uploadPhoto(Long id, MultipartFile file) {
        LawFirm firm = requireFirm(lawFirmMapper.selectForUpdate(id));
        String url = mediaService.replace(firm.getPhotoUrl(), file);
        lawFirmMapper.update(null, Wrappers.lambdaUpdate(LawFirm.class)
                .eq(LawFirm::getId, id).set(LawFirm::getPhotoUrl, url));
        directoryCache.invalidateLawFirm(id);
        return url;
    }

    private LawFirm requireFirm(LawFirm firm) {
        if (firm == null) {
            throw new BusinessException(404, "律所不存在");
        }
        return firm;
    }
}
