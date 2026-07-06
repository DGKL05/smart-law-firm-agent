package com.example.dgkl.lawyer.dto;

import com.example.dgkl.lawyer.entity.LawFirm;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Data
public class LawFirmDetailResponse extends LawFirm {
    private List<LawyerResponse> lawyers;

    public static LawFirmDetailResponse of(LawFirm firm, List<LawyerResponse> lawyers) {
        LawFirmDetailResponse response = new LawFirmDetailResponse();
        BeanUtils.copyProperties(firm, response);
        response.setLawyers(lawyers);
        return response;
    }
}
