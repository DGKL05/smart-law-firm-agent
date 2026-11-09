package com.example.dgkl.schedule.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.schedule.dto.ScheduleBlockRequest;
import com.example.dgkl.schedule.dto.ScheduleReservationRequest;
import com.example.dgkl.schedule.dto.ScheduleRuleRequest;
import com.example.dgkl.schedule.dto.WeeklyScheduleRequest;
import com.example.dgkl.schedule.dto.WeeklyScheduleResponse;
import com.example.dgkl.schedule.dto.WeeklyScheduleRule;
import com.example.dgkl.schedule.entity.ScheduleRule;
import com.example.dgkl.schedule.mapper.ScheduleBlockMapper;
import com.example.dgkl.schedule.mapper.ScheduleGuardMapper;
import com.example.dgkl.schedule.mapper.ScheduleReservationMapper;
import com.example.dgkl.schedule.mapper.ScheduleRuleMapper;
import com.example.dgkl.schedule.service.impl.ScheduleServiceImpl;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CustomizableThreadCreator;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleReservationTest {
    private static final Long LAWYER_ID = 1L;
    private static final Long OTHER_LAWYER_ID = 2L;
    private final JdbcDataSource dataSource = new JdbcDataSource();
    private TransactionTemplate transaction;
    private ScheduleService service;

    @BeforeEach
    void setUp() throws Exception {
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        String ddl = new ClassPathResource("schedule-schema/001_schema.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("USE smart_law_schedule;", "")
                .replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", "")
                .replace(" STORED,", ",");
        new ResourceDatabasePopulator(new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8))).execute(dataSource);

        MybatisConfiguration config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.addMapper(ScheduleRuleMapper.class);
        config.addMapper(ScheduleBlockMapper.class);
        config.addMapper(ScheduleReservationMapper.class);
        config.addMapper(ScheduleGuardMapper.class);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(config);
        SqlSessionTemplate session = new SqlSessionTemplate(factory.getObject());
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        ScheduleLawyers lawyers = mock(ScheduleLawyers.class);
        when(lawyers.forCreate(null, LAWYER_ID)).thenReturn(LAWYER_ID);
        when(lawyers.forCreate(null, OTHER_LAWYER_ID)).thenReturn(OTHER_LAWYER_ID);
        when(lawyers.forCreate(LAWYER_ID, null)).thenReturn(LAWYER_ID);
        service = new ScheduleServiceImpl(session.getMapper(ScheduleRuleMapper.class),
                session.getMapper(ScheduleBlockMapper.class), session.getMapper(ScheduleReservationMapper.class),
                session.getMapper(ScheduleGuardMapper.class), lawyers);
    }

    @AfterEach
    void cleanUp() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }

    @Test
    void rejectsReservationOutsideDefaultWorkingWeek() {
        ScheduleReservationRequest weekend = request(1L, next(DayOfWeek.SATURDAY));
        assertFalse(inTransaction(() -> service.reserve(weekend)));
    }

    @Test
    void listsAvailabilityBeyondTheFirstWorkWeekWithinTheCalendarHorizon() {
        LocalDateTime slot = next(DayOfWeek.MONDAY).plusWeeks(2);
        ScheduleReservationRequest reservation = request(1L, slot);
        assertTrue(inTransaction(() -> service.reserve(reservation)));

        var slots = inTransaction(() -> service.slots(LAWYER_ID));
        LocalDate exclusiveEnd = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(35);
        assertTrue(slots.stream().anyMatch(entry -> entry.startTime().equals(slot) && !entry.available()));
        assertTrue(slots.stream().allMatch(entry -> entry.date().isBefore(exclusiveEnd)));
    }

    @Test
    void savingTheDefaultWeekPersistsRuleIdsAndRejectsAStaleEditor() {
        WeeklyScheduleResponse initial = service.weeklyRules(LAWYER_ID);
        assertFalse(initial.configured());
        assertEquals(10, initial.rules().size());
        assertEquals(new WeeklyScheduleRule(null, 1, LocalTime.of(9, 0), LocalTime.NOON, 1),
                initial.rules().get(0));
        assertEquals(new WeeklyScheduleRule(null, 5, LocalTime.of(14, 0), LocalTime.of(17, 0), 1),
                initial.rules().get(9));

        WeeklyScheduleResponse saved = replace(initial, initial.rules());
        assertTrue(saved.configured());
        assertNotEquals(initial.revision(), saved.revision());
        WeeklyScheduleRule monday = saved.rules().get(0);
        assertNotNull(monday.id());
        List<WeeklyScheduleRule> draft = new ArrayList<>(saved.rules());
        draft.set(0, new WeeklyScheduleRule(monday.id(), monday.weekday(), monday.startTime(), monday.endTime(), 0));
        WeeklyScheduleRule friday = draft.get(9);
        draft.set(9, new WeeklyScheduleRule(null, friday.weekday(), friday.startTime(), friday.endTime(), 1));

        WeeklyScheduleResponse modified = replace(saved, draft);
        assertEquals(monday.id(), modified.rules().get(0).id());
        assertEquals(0, modified.rules().get(0).enabled());
        assertEquals(friday.id(), modified.rules().get(9).id());
        BusinessException stale = assertThrows(BusinessException.class, () -> replace(saved, saved.rules()));
        assertEquals(409, stale.getCode());
        assertEquals(modified, service.weeklyRules(LAWYER_ID));
    }

    @Test
    void aNewBookingKeepsTheEditRevisionButProtectsItsTimeSlot() {
        WeeklyScheduleResponse draft = service.weeklyRules(LAWYER_ID);
        assertTrue(inTransaction(() -> service.reserve(request(1L, next(DayOfWeek.MONDAY)))));
        assertEquals(draft, service.weeklyRules(LAWYER_ID));

        List<WeeklyScheduleRule> removed = draft.rules().subList(1, draft.rules().size());
        BusinessException conflict = assertThrows(BusinessException.class, () -> replace(draft, removed));
        assertEquals(409, conflict.getCode());
        assertEquals(draft, service.weeklyRules(LAWYER_ID));
        assertTrue(replace(draft, draft.rules()).configured());
    }

    @Test
    void overlappingRulesAndForeignOrDuplicateIdsAreRejectedWithoutEditingTheWeek() {
        WeeklyScheduleResponse initial = service.weeklyRules(LAWYER_ID);
        WeeklyScheduleResponse saved = replace(initial,
                List.of(new WeeklyScheduleRule(null, 1, LocalTime.of(9, 0), LocalTime.NOON, 1)));
        WeeklyScheduleRule owned = saved.rules().get(0);
        ScheduleRule other = inTransaction(() -> service.createRule(
                new ScheduleRuleRequest(null, 2, LocalTime.of(9, 0), LocalTime.NOON, 1), OTHER_LAWYER_ID));
        WeeklyScheduleRule overlap = new WeeklyScheduleRule(null, 1, LocalTime.of(11, 0), LocalTime.of(13, 0), 1);
        WeeklyScheduleRule foreign = new WeeklyScheduleRule(other.getId(), 2, LocalTime.of(9, 0), LocalTime.NOON, 1);

        assertEquals(409, assertThrows(BusinessException.class,
                () -> replace(saved, List.of(owned, overlap))).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> replace(saved, List.of(owned, foreign))).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> replace(saved, List.of(owned, owned))).getCode());
        assertEquals(saved, service.weeklyRules(LAWYER_ID));
    }

    @Test
    void clearingAConfiguredWeekDoesNotRestoreDefaultAvailability() {
        WeeklyScheduleResponse initial = service.weeklyRules(LAWYER_ID);
        assertThrows(BusinessException.class, () -> replace(initial, List.of()));
        WeeklyScheduleResponse saved = replace(initial, List.of(initial.rules().get(0)));

        WeeklyScheduleResponse cleared = replace(saved, List.of());
        assertTrue(cleared.configured());
        assertTrue(cleared.rules().isEmpty());
        assertEquals(cleared, service.weeklyRules(LAWYER_ID));
        assertTrue(inTransaction(() -> service.slots(LAWYER_ID)).isEmpty());
    }

    @Test
    void aDatabaseFailureRollsBackTheWholeReplacement() throws Exception {
        WeeklyScheduleResponse initial = service.weeklyRules(LAWYER_ID);
        WeeklyScheduleResponse saved = replace(initial, List.of(initial.rules().get(2)));
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE lawyer_schedule ADD CONSTRAINT reject_sunday CHECK (weekday <> 7)");
        }
        WeeklyScheduleRule sunday = new WeeklyScheduleRule(null, 7, LocalTime.of(9, 0), LocalTime.NOON, 1);
        assertThrows(DataIntegrityViolationException.class,
                () -> replace(saved, List.of(initial.rules().get(0), sunday)));
        assertEquals(saved, service.weeklyRules(LAWYER_ID));
    }

    @Test
    void atReservationWithoutThePropagatedXidDoesNotOccupyTheSlot() {
        LocalDateTime slot = next(DayOfWeek.MONDAY);
        ScheduleReservationRequest unbound = new ScheduleReservationRequest(1L, LAWYER_ID, slot, true);

        assertThrows(BusinessException.class, () -> inTransaction(() -> service.reserve(unbound)));
        assertTrue(inTransaction(() -> service.reserve(request(2L, slot))));
    }

    @Test
    void concurrentRequestsHaveOneWinnerAndCancellationReleasesTheSlot() throws Exception {
        LocalDateTime slot = next(DayOfWeek.MONDAY);
        ScheduleReservationRequest first = request(1L, slot);
        ScheduleReservationRequest second = request(2L, slot);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CustomizableThreadCreator threads = new CustomizableThreadCreator("schedule-test-");
        ExecutorService workers = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(2), threads::createThread);
        boolean firstWon;
        try {
            Future<Boolean> firstResult = workers.submit(() -> reserveConcurrently(first, ready, start));
            Future<Boolean> secondResult = workers.submit(() -> reserveConcurrently(second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            firstWon = firstResult.get(5, TimeUnit.SECONDS);
            assertNotEquals(firstWon, secondResult.get(5, TimeUnit.SECONDS));
        } finally {
            workers.shutdownNow();
        }

        ScheduleReservationRequest winner = firstWon ? first : second;
        ScheduleReservationRequest loser = firstWon ? second : first;
        assertTrue(inTransaction(() -> service.reserve(winner)));
        release(winner);
        release(winner);
        assertFalse(inTransaction(() -> service.reserve(winner)));
        assertFalse(inTransaction(() -> service.reserve(loser)));
        assertTrue(inTransaction(() -> service.reserve(request(3L, slot))));
    }

    @Test
    void cannotRemoveOpeningOrBlockAnActiveReservation() {
        LocalDateTime slot = next(DayOfWeek.MONDAY);
        ScheduleRule rule = inTransaction(() -> service.createRule(
                new ScheduleRuleRequest(null, 1, LocalTime.of(9, 0), LocalTime.of(12, 0), 1), LAWYER_ID));
        ScheduleReservationRequest reservation = request(1L, slot);
        assertTrue(inTransaction(() -> service.reserve(reservation)));

        assertThrows(BusinessException.class, () -> inTransaction(() -> {
            service.deleteRule(rule.getId(), LAWYER_ID);
            return null;
        }));
        ScheduleBlockRequest block = new ScheduleBlockRequest(null, slot, slot.plusHours(1), "临时停诊");
        assertThrows(BusinessException.class,
                () -> inTransaction(() -> service.createBlock(block, LAWYER_ID)));

        release(reservation);
        inTransaction(() -> service.createBlock(block, LAWYER_ID));
        assertFalse(inTransaction(() -> service.reserve(request(2L, slot))));
        inTransaction(() -> {
            service.deleteRule(rule.getId(), LAWYER_ID);
            return null;
        });
        assertFalse(inTransaction(() -> service.reserve(request(3L, slot.plusWeeks(1)))));
    }

    @Test
    void releaseBeforeDelayedReservationLeavesTheSlotFree() {
        LocalDateTime slot = next(DayOfWeek.MONDAY);
        ScheduleReservationRequest delayed = request(1L, slot);
        release(delayed);
        assertFalse(inTransaction(() -> service.reserve(delayed)));
        assertTrue(inTransaction(() -> service.reserve(request(2L, slot))));
    }

    private boolean reserveConcurrently(ScheduleReservationRequest reservation, CountDownLatch ready,
                                        CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent booking test did not start");
        }
        return inTransaction(() -> service.reserve(reservation));
    }

    private void release(ScheduleReservationRequest request) {
        inTransaction(() -> {
            service.release(request);
            return null;
        });
    }

    private <T> T inTransaction(Supplier<T> operation) {
        return transaction.execute(status -> operation.get());
    }

    private WeeklyScheduleResponse replace(WeeklyScheduleResponse current, List<WeeklyScheduleRule> rules) {
        return inTransaction(() -> service.replaceWeeklyRules(LAWYER_ID,
                new WeeklyScheduleRequest(current.revision(), rules)));
    }

    private static LocalDateTime next(DayOfWeek weekday) {
        return LocalDate.now(ZoneId.of("Asia/Shanghai")).with(TemporalAdjusters.next(weekday)).atTime(10, 0);
    }

    private static ScheduleReservationRequest request(Long appointmentId, LocalDateTime time) {
        return new ScheduleReservationRequest(appointmentId, LAWYER_ID, time);
    }
}
