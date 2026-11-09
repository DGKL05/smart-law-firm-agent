package com.example.dgkl.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dgkl.notification.entity.Notification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface NotificationMapper extends BaseMapper<Notification> {
    // 同一个事件只能投递一次，删除通知后仍保留事件键。
    @Insert("""
            INSERT INTO notification (event_key, user_id, title, content)
            VALUES (#{eventKey}, #{userId}, #{title}, #{content})
            ON DUPLICATE KEY UPDATE event_key = event_key
            """)
    void insertEvent(@Param("eventKey") String eventKey, @Param("userId") Long userId,
                     @Param("title") String title, @Param("content") String content);
}
