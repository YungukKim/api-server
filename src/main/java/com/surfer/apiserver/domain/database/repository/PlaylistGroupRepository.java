package com.surfer.apiserver.domain.database.repository;

import com.surfer.apiserver.common.constant.CommonCode;
import com.surfer.apiserver.domain.database.entity.MemberEntity;
import com.surfer.apiserver.domain.database.entity.PlaylistGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaylistGroupRepository extends JpaRepository<PlaylistGroupEntity, Long> {
    /**
     * member 가 가지고 있는 playlistGroup 을 전부 조회
     */
    @Query("SELECT pg FROM PlaylistGroupEntity pg JOIN FETCH pg.playlistTrackEntities WHERE pg.memberEntity = :member ORDER BY pg.regDate")
    List<PlaylistGroupEntity> findByMember(@Param("member") MemberEntity member);

    /**
     * 등급이 dj 인 member 들이 가지고 있는 플레이리스트를 전부 조회
     */
    @Query("SELECT pg FROM PlaylistGroupEntity pg JOIN FETCH MemberAuthorityEntity ma WHERE ma.authority = :authority AND pg.isOpen = 1")
    List<PlaylistGroupEntity> findByAuthority(@Param("authority") CommonCode.MemberAuthority authority);
}
