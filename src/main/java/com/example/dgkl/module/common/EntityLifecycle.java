package com.example.dgkl.module.common;

import java.time.LocalDateTime;

public final class EntityLifecycle {
    private EntityLifecycle() {
    }

    public static void forCreate(BaseEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setDeleted(0);
    }

    public static void forUpdate(BaseEntity entity) {
        entity.setUpdateTime(LocalDateTime.now());
    }
}
