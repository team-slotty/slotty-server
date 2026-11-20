package com.slotty.server.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass // 테이블이 아니라, 필드를 자식 엔티티에 물려주는 부모라는 뜻
@EntityListeners(AuditingEntityListener.class) // 저장 직전에 시각을 채워 주는 리스너 연결
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Getter
    private LocalDateTime createdAt;

}

// 이후 `created_at`이 있는 엔티티는 `extends BaseTimeEntity`만 하면 됨
