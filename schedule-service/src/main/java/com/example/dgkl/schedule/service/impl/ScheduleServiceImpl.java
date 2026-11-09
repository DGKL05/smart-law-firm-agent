package com.example.dgkl.schedule.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.schedule.dto.ScheduleBlockRequest;
import com.example.dgkl.schedule.dto.ScheduleReservationRequest;
import com.example.dgkl.schedule.dto.ScheduleRuleRequest;
import com.example.dgkl.schedule.dto.ScheduleSlotResponse;
import com.example.dgkl.schedule.dto.WeeklyScheduleRequest;
import com.example.dgkl.schedule.dto.WeeklyScheduleResponse;
import com.example.dgkl.schedule.dto.WeeklyScheduleRule;
import com.example.dgkl.schedule.entity.ScheduleBlock;
import com.example.dgkl.schedule.entity.ScheduleReservation;
import com.example.dgkl.schedule.entity.ScheduleRule;
import com.example.dgkl.schedule.mapper.ScheduleBlockMapper;
import com.example.dgkl.schedule.mapper.ScheduleGuardMapper;
import com.example.dgkl.schedule.mapper.ScheduleReservationMapper;
import com.example.dgkl.schedule.mapper.ScheduleRuleMapper;
import com.example.dgkl.schedule.service.ScheduleLawyers;
import com.example.dgkl.schedule.service.SchedulePolicy;
import com.example.dgkl.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.spring.annotation.GlobalLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String RESERVED = "RESERVED";
    private static final String RELEASED = "RELEASED";
    private static final String REJECTED = "REJECTED";
    private static final int LIST_DAYS = 35;
    private static final Comparator<ScheduleRule> RULE_ORDER = Comparator.comparing(ScheduleRule::getWeekday)
            .thenComparing(ScheduleRule::getStartTime);

    private final ScheduleRuleMapper ruleMapper;
    private final ScheduleBlockMapper blockMapper;
    private final ScheduleReservationMapper reservationMapper;
    private final ScheduleGuardMapper guardMapper;
    private final ScheduleLawyers lawyers;

    @Override
    public PageResult<ScheduleRule> rules(Long owner, Long lawyerId, long pageNum, long pageSize) {
        Long effectiveLawyer = owner == null ? lawyerId : owner;
        Page<ScheduleRule> page = ruleMapper.selectPage(Page.of(pageNum, pageSize),
                Wrappers.<ScheduleRule>lambdaQuery()
                        .eq(effectiveLawyer != null, ScheduleRule::getLawyerId, effectiveLawyer)
                        .orderByAsc(ScheduleRule::getLawyerId, ScheduleRule::getWeekday, ScheduleRule::getStartTime));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public ScheduleRule rule(Long id, Long owner) {
        ScheduleRule rule = ruleMapper.selectById(id);
        if (rule == null || (owner != null && !owner.equals(rule.getLawyerId()))) {
            throw new BusinessException(404, "排班规则不存在");
        }
        return rule;
    }

    @Override
    @GlobalLock
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ScheduleRule createRule(ScheduleRuleRequest request, Long owner) {
        Long lawyerId = lawyers.forCreate(request.lawyerId(), owner);
        ScheduleRule newRule = new ScheduleRule();
        newRule.setLawyerId(lawyerId);
        applyRule(newRule, request);
        guardMapper.lock(lawyerId);
        validateRules(lawyerId, newRule);
        ruleMapper.insert(newRule);
        return ruleMapper.selectById(newRule.getId());
    }

    @Override
    @GlobalLock
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ScheduleRule updateRule(Long id, ScheduleRuleRequest request, Long owner) {
        Long lawyerId = rule(id, owner).getLawyerId();
        lawyers.requireUnchanged(request.lawyerId(), lawyerId);
        guardMapper.lock(lawyerId);
        ScheduleRule rule = rule(id, owner);
        applyRule(rule, request);
        validateRules(lawyerId, rule);
        ruleMapper.updateById(rule);
        return ruleMapper.selectById(id);
    }

    @Override
    @GlobalLock
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void deleteRule(Long id, Long owner) {
        Long lawyerId = rule(id, owner).getLawyerId();
        guardMapper.lock(lawyerId);
        rule(id, owner);
        List<ScheduleRule> remaining = allRules(lawyerId);
        remaining.removeIf(rule -> id.equals(rule.getId()));
        requireReservedSlotsStillOpen(lawyerId, remaining);
        ruleMapper.deleteById(id);
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public WeeklyScheduleResponse weeklyRules(Long lawyerId) {
        Long verifiedId = lawyers.forCreate(lawyerId, null);
        return weeklyResponse(verifiedId, ruleMapper.hasConfiguredRules(verifiedId), allRules(verifiedId));
    }

    @Override
    @GlobalLock
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public WeeklyScheduleResponse replaceWeeklyRules(Long lawyerId, WeeklyScheduleRequest request) {
        Long verifiedId = lawyers.forCreate(lawyerId, null);
        guardMapper.lock(verifiedId);
        List<ScheduleRule> currentRules = allRules(verifiedId);
        boolean configured = ruleMapper.hasConfiguredRules(verifiedId);
        WeeklyScheduleResponse current = weeklyResponse(verifiedId, configured, currentRules);
        if (!current.revision().equals(request.revision())) {
            throw new BusinessException(409, "周排班已被修改，请重新加载后再保存");
        }
        if (!configured && request.rules().isEmpty()) {
            throw new BusinessException("请保留至少一个时段，或将默认时段全部关闭");
        }

        Map<Long, ScheduleRule> existing = currentRules.stream()
                .collect(Collectors.toMap(ScheduleRule::getId, rule -> rule));
        List<ScheduleRule> replacement = replacementRules(verifiedId, request.rules(), existing);
        requireNoWeeklyOverlap(replacement);
        requireReservedSlotsStillOpen(verifiedId, replacement);
        saveWeeklyRules(existing, replacement);
        return weeklyResponse(verifiedId, true, allRules(verifiedId));
    }

    @Override
    public PageResult<ScheduleBlock> blocks(Long owner, Long lawyerId, long pageNum, long pageSize) {
        Long effectiveLawyer = owner == null ? lawyerId : owner;
        Page<ScheduleBlock> page = blockMapper.selectPage(Page.of(pageNum, pageSize),
                Wrappers.<ScheduleBlock>lambdaQuery()
                        .eq(effectiveLawyer != null, ScheduleBlock::getLawyerId, effectiveLawyer)
                        .orderByDesc(ScheduleBlock::getStartTime));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public ScheduleBlock block(Long id, Long owner) {
        ScheduleBlock block = blockMapper.selectById(id);
        if (block == null || (owner != null && !owner.equals(block.getLawyerId()))) {
            throw new BusinessException(404, "停诊记录不存在");
        }
        return block;
    }

    @Override
    @GlobalLock
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ScheduleBlock createBlock(ScheduleBlockRequest request, Long owner) {
        Long lawyerId = lawyers.forCreate(request.lawyerId(), owner);
        ScheduleBlock block = new ScheduleBlock();
        block.setLawyerId(lawyerId);
        applyBlock(block, request);
        guardMapper.lock(lawyerId);
        requireNoReservedOverlap(lawyerId, request.startTime(), request.endTime());
        blockMapper.insert(block);
        return blockMapper.selectById(block.getId());
    }

    @Override
    @GlobalLock
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ScheduleBlock updateBlock(Long id, ScheduleBlockRequest request, Long owner) {
        Long lawyerId = block(id, owner).getLawyerId();
        lawyers.requireUnchanged(request.lawyerId(), lawyerId);
        guardMapper.lock(lawyerId);
        ScheduleBlock block = block(id, owner);
        applyBlock(block, request);
        requireNoReservedOverlap(lawyerId, request.startTime(), request.endTime());
        blockMapper.updateById(block);
        return blockMapper.selectById(id);
    }

    @Override
    @GlobalLock
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void deleteBlock(Long id, Long owner) {
        Long lawyerId = block(id, owner).getLawyerId();
        guardMapper.lock(lawyerId);
        block(id, owner);
        blockMapper.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleSlotResponse> slots(Long lawyerId) {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        LocalDateTime end = now.plusDays(LIST_DAYS);
        List<ScheduleRule> rules = allRules(lawyerId);
        boolean configured = ruleMapper.hasConfiguredRules(lawyerId);
        List<ScheduleBlock> blocks = blocksBetween(lawyerId, now, end);
        List<ScheduleReservation> reservations = activeReservationsBetween(lawyerId, now, end);
        List<ScheduleSlotResponse> slots = new ArrayList<>();
        for (LocalDate date = now.toLocalDate(); date.isBefore(end.toLocalDate()); date = date.plusDays(1)) {
            List<LocalDateTime> starts = SchedulePolicy.starts(date, rules, configured).stream()
                    .filter(start -> start.isAfter(now))
                    .toList();
            for (LocalDateTime start : starts) {
                LocalDateTime slotEnd = start.plusHours(1);
                String reason = unavailableReason(start, slotEnd, blocks, reservations);
                slots.add(new ScheduleSlotResponse(date, start, slotEnd, reason == null, reason));
            }
        }
        return slots;
    }

    @Override
    @GlobalLock
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public boolean reserve(ScheduleReservationRequest request) {
        validateReservation(request);
        guardMapper.lock(request.lawyerId());
        ScheduleReservation existing = reservationMapper.selectById(request.appointmentId());
        if (existing != null) {
            requireSameReservation(existing, request);
            return RESERVED.equals(existing.getStatus());
        }

        LocalDateTime start = request.appointmentTime();
        LocalDateTime end = start.plusHours(1);
        List<LocalDateTime> starts = SchedulePolicy.starts(start.toLocalDate(), allRules(request.lawyerId()),
                ruleMapper.hasConfiguredRules(request.lawyerId()));
        boolean available = start.isAfter(LocalDateTime.now(BUSINESS_ZONE)) && starts.contains(start)
                && unavailableReason(start, end, blocksBetween(request.lawyerId(), start, end),
                        activeReservationsBetween(request.lawyerId(), start, end)) == null;
        reservationMapper.insert(newReservation(request, available ? RESERVED : REJECTED));
        return available;
    }

    @Override
    @GlobalLock
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void release(ScheduleReservationRequest request) {
        validateReservation(request);
        guardMapper.lock(request.lawyerId());
        ScheduleReservation existing = reservationMapper.selectById(request.appointmentId());
        if (existing == null) {
            // A tombstone prevents a delayed reserve request from reopening a cancelled reservation.
            reservationMapper.insert(newReservation(request, RELEASED));
            return;
        }
        requireSameReservation(existing, request);
        if (RESERVED.equals(existing.getStatus())) {
            existing.setStatus(RELEASED);
            reservationMapper.updateById(existing);
        }
    }

    @Override
    public long ruleCount() {
        return ruleMapper.selectCount(Wrappers.<ScheduleRule>lambdaQuery());
    }

    private List<ScheduleRule> allRules(Long lawyerId) {
        return new ArrayList<>(ruleMapper.selectList(Wrappers.<ScheduleRule>lambdaQuery()
                .eq(ScheduleRule::getLawyerId, lawyerId)
                .orderByAsc(ScheduleRule::getWeekday, ScheduleRule::getStartTime, ScheduleRule::getId)));
    }

    private void applyRule(ScheduleRule rule, ScheduleRuleRequest request) {
        applyRule(rule, request.weekday(), request.startTime(), request.endTime(), request.enabled());
    }

    private void applyRule(ScheduleRule rule, Integer weekday, LocalTime startTime, LocalTime endTime,
                           Integer enabled) {
        if (startTime.getSecond() != 0 || startTime.getNano() != 0
                || endTime.getSecond() != 0 || endTime.getNano() != 0
                || Duration.between(startTime, endTime).toMinutes() < 60) {
            throw new BusinessException("排班起止时间精确到分钟，且时长至少为一小时");
        }
        rule.setWeekday(weekday);
        rule.setStartTime(startTime);
        rule.setEndTime(endTime);
        rule.setEnabled(enabled);
    }

    private WeeklyScheduleResponse weeklyResponse(Long lawyerId, boolean configured, List<ScheduleRule> rules) {
        List<WeeklyScheduleRule> items = new ArrayList<>();
        if (configured) {
            rules.forEach(rule -> items.add(weeklyItem(rule, rule.getId())));
        } else {
            for (int weekday = 1; weekday <= 5; weekday++) {
                for (SchedulePolicy.TimeWindow window : SchedulePolicy.defaultWindows(weekday)) {
                    items.add(new WeeklyScheduleRule(null, weekday, window.startTime(), window.endTime(), 1));
                }
            }
        }
        return new WeeklyScheduleResponse(revision(lawyerId, configured, items), configured, items);
    }

    private String revision(Long lawyerId, boolean configured, List<WeeklyScheduleRule> rules) {
        StringBuilder state = new StringBuilder().append(lawyerId).append('|').append(configured);
        for (WeeklyScheduleRule rule : rules) {
            state.append('|').append(rule.id()).append(',').append(rule.weekday()).append(',')
                    .append(rule.startTime()).append(',').append(rule.endTime()).append(',')
                    .append(rule.enabled());
        }
        try {
            byte[] bytes = state.toString().getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private List<ScheduleRule> replacementRules(Long lawyerId, List<WeeklyScheduleRule> items,
                                                Map<Long, ScheduleRule> existing) {
        Set<Long> retained = new HashSet<>();
        for (WeeklyScheduleRule item : items) {
            if (item.id() == null) {
                continue;
            }
            if (!existing.containsKey(item.id())) {
                throw new BusinessException("排班规则不属于该律师或已删除");
            }
            if (!retained.add(item.id())) {
                throw new BusinessException("周排班包含重复的规则编号");
            }
        }
        Map<WeeklyScheduleRule, Long> unchanged = new HashMap<>();
        existing.values().stream().filter(rule -> !retained.contains(rule.getId()))
                .forEach(rule -> unchanged.put(weeklyItem(rule, null), rule.getId()));
        List<ScheduleRule> rules = new ArrayList<>(items.size());
        for (WeeklyScheduleRule item : items) {
            ScheduleRule rule = new ScheduleRule();
            rule.setId(item.id() == null ? unchanged.remove(item) : item.id());
            rule.setLawyerId(lawyerId);
            applyRule(rule, item.weekday(), item.startTime(), item.endTime(), item.enabled());
            rules.add(rule);
        }
        return rules;
    }

    private void requireNoWeeklyOverlap(List<ScheduleRule> rules) {
        rules.sort(RULE_ORDER);
        for (int index = 1; index < rules.size(); index++) {
            ScheduleRule previous = rules.get(index - 1);
            ScheduleRule current = rules.get(index);
            if (previous.getWeekday().equals(current.getWeekday())
                    && previous.getEndTime().isAfter(current.getStartTime())) {
                throw new BusinessException(409, "同一天的排班规则不得重叠");
            }
        }
    }

    private void saveWeeklyRules(Map<Long, ScheduleRule> existing, List<ScheduleRule> replacement) {
        Set<Long> retained = replacement.stream().map(ScheduleRule::getId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        existing.keySet().stream().filter(id -> !retained.contains(id)).forEach(ruleMapper::deleteById);
        for (ScheduleRule rule : replacement) {
            Long id = rule.getId();
            if (id == null) {
                ruleMapper.insert(rule);
            } else if (!weeklyItem(rule, id).equals(weeklyItem(existing.get(id), id))) {
                ruleMapper.updateById(rule);
            }
        }
    }

    private WeeklyScheduleRule weeklyItem(ScheduleRule rule, Long id) {
        return new WeeklyScheduleRule(id, rule.getWeekday(), rule.getStartTime(), rule.getEndTime(), rule.getEnabled());
    }

    private void validateRules(Long lawyerId, ScheduleRule candidate) {
        List<ScheduleRule> remaining = allRules(lawyerId);
        remaining.removeIf(rule -> Objects.equals(rule.getId(), candidate.getId()));
        if (remaining.stream().anyMatch(rule -> rule.getWeekday().equals(candidate.getWeekday())
                && rule.getStartTime().isBefore(candidate.getEndTime())
                && candidate.getStartTime().isBefore(rule.getEndTime()))) {
            throw new BusinessException(409, "同一天的排班规则不得重叠");
        }
        remaining.add(candidate);
        requireReservedSlotsStillOpen(lawyerId, remaining);
    }

    private void requireReservedSlotsStillOpen(Long lawyerId, List<ScheduleRule> candidateRules) {
        LocalDateTime earliest = LocalDateTime.now(BUSINESS_ZONE).minusHours(1);
        List<ScheduleReservation> active = reservationMapper.selectList(Wrappers.<ScheduleReservation>lambdaQuery()
                .eq(ScheduleReservation::getLawyerId, lawyerId)
                .eq(ScheduleReservation::getStatus, RESERVED)
                .gt(ScheduleReservation::getSlotStart, earliest));
        for (ScheduleReservation reservation : active) {
            LocalDateTime start = reservation.getSlotStart();
            if (!SchedulePolicy.starts(start.toLocalDate(), candidateRules, true).contains(start)) {
                throw new BusinessException(409, "该排班仍有有效预约，请先处理预约");
            }
        }
    }

    private void applyBlock(ScheduleBlock block, ScheduleBlockRequest request) {
        if (!request.startTime().isBefore(request.endTime()) || request.startTime().getNano() != 0
                || request.endTime().getNano() != 0) {
            throw new BusinessException("停诊结束时间须晚于开始时间，且精确到秒");
        }
        block.setStartTime(request.startTime());
        block.setEndTime(request.endTime());
        String reason = request.reason();
        block.setReason(reason == null || reason.isBlank() ? "律师停诊" : reason.trim());
    }

    private void requireNoReservedOverlap(Long lawyerId, LocalDateTime start, LocalDateTime end) {
        LocalDateTime effectiveStart = start.isBefore(LocalDateTime.now(BUSINESS_ZONE))
                ? LocalDateTime.now(BUSINESS_ZONE) : start;
        if (effectiveStart.isBefore(end) && !activeReservationsBetween(lawyerId, effectiveStart, end).isEmpty()) {
            throw new BusinessException(409, "停诊时段仍有有效预约，请先处理预约");
        }
    }

    private List<ScheduleReservation> activeReservationsBetween(Long lawyerId, LocalDateTime start,
                                                               LocalDateTime end) {
        return reservationMapper.selectList(Wrappers.<ScheduleReservation>lambdaQuery()
                .eq(ScheduleReservation::getLawyerId, lawyerId)
                .eq(ScheduleReservation::getStatus, RESERVED)
                .gt(ScheduleReservation::getSlotStart, start.minusHours(1))
                .lt(ScheduleReservation::getSlotStart, end));
    }

    private List<ScheduleBlock> blocksBetween(Long lawyerId, LocalDateTime start, LocalDateTime end) {
        return blockMapper.selectList(Wrappers.<ScheduleBlock>lambdaQuery()
                .eq(ScheduleBlock::getLawyerId, lawyerId)
                .gt(ScheduleBlock::getEndTime, start)
                .lt(ScheduleBlock::getStartTime, end));
    }

    private String unavailableReason(LocalDateTime start, LocalDateTime end, List<ScheduleBlock> blocks,
                                     List<ScheduleReservation> reservations) {
        if (reservations.stream().anyMatch(reservation ->
                SchedulePolicy.overlaps(start, end, reservation.getSlotStart(),
                        reservation.getSlotStart().plusHours(1)))) {
            return "已预约";
        }
        if (blocks.stream().anyMatch(block -> SchedulePolicy.overlaps(start, end, block.getStartTime(),
                block.getEndTime()))) {
            return "律师停诊";
        }
        return null;
    }

    private ScheduleReservation newReservation(ScheduleReservationRequest request, String status) {
        ScheduleReservation reservation = new ScheduleReservation();
        reservation.setAppointmentId(request.appointmentId());
        reservation.setLawyerId(request.lawyerId());
        reservation.setSlotStart(request.appointmentTime());
        reservation.setStatus(status);
        return reservation;
    }

    private void validateReservation(ScheduleReservationRequest request) {
        if (request.globalTransaction() && !RootContext.inGlobalTransaction()) {
            throw new BusinessException(503, "排班服务未接收到预约事务标识");
        }
        if (request.appointmentTime().getNano() != 0) {
            throw new BusinessException("预约时间须精确到秒");
        }
    }

    private void requireSameReservation(ScheduleReservation existing, ScheduleReservationRequest request) {
        if (!existing.getLawyerId().equals(request.lawyerId())
                || !existing.getSlotStart().equals(request.appointmentTime())) {
            throw new BusinessException(409, "预约编号与已记录的时段不一致");
        }
    }
}
