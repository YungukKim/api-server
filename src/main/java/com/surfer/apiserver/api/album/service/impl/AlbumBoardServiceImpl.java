package com.surfer.apiserver.api.album.service.impl;

import com.surfer.apiserver.api.album.dto.AlbumReplyReq;
import com.surfer.apiserver.api.album.dto.AlbumReplyRes;
import com.surfer.apiserver.api.album.service.AlbumBoardService;
import com.surfer.apiserver.common.exception.BusinessException;
import com.surfer.apiserver.common.response.ApiResponseCode;
import com.surfer.apiserver.domain.database.entity.AlbumEntity;
import com.surfer.apiserver.domain.database.entity.AlbumReplyEntity;
import com.surfer.apiserver.domain.database.entity.AlbumSingerEntity;
import com.surfer.apiserver.domain.database.entity.MemberEntity;
import com.surfer.apiserver.domain.database.repository.AlbumReplyRepository;
import com.surfer.apiserver.domain.database.repository.AlbumRepository;
import com.surfer.apiserver.domain.database.repository.AlbumSingerRepository;
import com.surfer.apiserver.domain.database.repository.MemberRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AlbumBoardServiceImpl implements AlbumBoardService {

    //페이지 처리를 위해 추가
    private final static int PAGE_COUNT=10; //한 페이지 당 댓글 10개

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private AlbumReplyRepository albumReplyRepository;
    @Autowired
    private AlbumSingerRepository albumSingerRepository;
    @Autowired
    private MemberRepository memberRepository;

    @Override
    public AlbumEntity selectById(Long seq) {
        //seq와 일치하는 앨범이 있는지 검색
        AlbumEntity album = albumRepository.findById(seq).orElseThrow(
                ()->new BusinessException(ApiResponseCode.INVALID_ALBUM_ID, HttpStatus.BAD_REQUEST)
        );
        //앨범 state 확인
        //0: 심사중, 1: 등록완료, 2: 반려됨
        int albumState = album.getAlbumState();
        if(albumState != 1){
            throw new BusinessException(ApiResponseCode.INVALID_ALBUM_STATE, HttpStatus.BAD_REQUEST);
        }

        return album;
    }

    @Override
    public void albumReplyInsert(AlbumReplyReq albumReplyReq, Long memberSeq, Long albumSeq) {
        //db에 회원이 존재하는지 확인
        Optional<MemberEntity> memberEntity = memberRepository.findById(memberSeq);
        if(memberEntity.isEmpty()) {
            throw new BusinessException(ApiResponseCode.INVALID_CLIENT_ID_OR_CLIENT_SECRET, HttpStatus.BAD_REQUEST);
        }
        MemberEntity member = memberEntity.get();

        //앨범 seq에 해당하는 album이 있는지 확인한다.
        AlbumEntity albumEntity = albumRepository.findById(albumSeq).orElseThrow(
                ()-> new BusinessException(ApiResponseCode.INVALID_ALBUM_ID, HttpStatus.BAD_REQUEST)
        );

        albumReplyRepository.save(
                AlbumReplyEntity.builder()
                        .albumReplyContent(albumReplyReq.getAlbumReplyContent())
                        .memberEntity(member)
                        .albumEntity(albumEntity)
                .build()
        );
    }

    @Override
    public void albumReplyUpdate(Long albumSeq, AlbumReplyReq albumReplyEntity, Long memberSeq, Long replySeq) {
        //db에서 댓글 seq에 해당하는 댓글을 찾는다
        AlbumReplyEntity replyEntity = albumReplyRepository.findById(albumSeq).orElseThrow(
                ()->new BusinessException(ApiResponseCode.INVALID_REPLY_ID, HttpStatus.BAD_REQUEST)
        );

        //앨범 seq와 댓글에 저장된 앨범 seq가 일치하는지 확인
        if(!replyEntity.getAlbumEntity().getAlbumSeq().equals(albumSeq)){
            throw new BusinessException(ApiResponseCode.INVALID_ALBUM_ID, HttpStatus.BAD_REQUEST);
        }

        //db에 회원이 존재하는지 확인
        Optional<MemberEntity> memberEntity = memberRepository.findById(memberSeq);
        if(memberEntity.isEmpty()) {
            throw new BusinessException(ApiResponseCode.INVALID_CLIENT_ID_OR_CLIENT_SECRET, HttpStatus.BAD_REQUEST);
        }
        MemberEntity member = memberEntity.get();

        //댓글 작성자와 수정하려는 사람이 일치하는지 확인
        if(!member.getMemberId().equals(replyEntity.getMemberEntity().getMemberId())){
            throw new BusinessException(ApiResponseCode.FAILED_UPDATE_REPLY, HttpStatus.BAD_REQUEST);
        }

        //댓글 내용을 새로 입력된 내용으로 수정한다.
        replyEntity.setAlbumReplyContent(albumReplyEntity.getAlbumReplyContent());
        //수정여부를 true로 변경한다.
        replyEntity.setAlbumReplyCorrect(true);
    }

    @Override
    public void albumReplyDelete(Long albumSeq, Long memberSeq, Long replySeq) {
        //db에서 댓글 seq에 해당하는 댓글을 찾는다
        AlbumReplyEntity replyEntity = albumReplyRepository.findById(replySeq).orElseThrow(
                ()->new BusinessException(ApiResponseCode.INVALID_REPLY_ID, HttpStatus.BAD_REQUEST)
        );

        //앨범 seq와 댓글에 저장된 앨범 seq가 일치하는지 확인
        if(!replyEntity.getAlbumEntity().getAlbumSeq().equals(albumSeq)){
            throw new BusinessException(ApiResponseCode.INVALID_ALBUM_ID, HttpStatus.BAD_REQUEST);
        }

        //db에 회원이 존재하는지 확인
        Optional<MemberEntity> memberEntity = memberRepository.findById(memberSeq);
        if(memberEntity.isEmpty()) {
            throw new BusinessException(ApiResponseCode.INVALID_CLIENT_ID_OR_CLIENT_SECRET, HttpStatus.BAD_REQUEST);
        }
        MemberEntity member = memberEntity.get();

        //댓글 작성자와 삭제하려는 사람이 일치하는지 확인
        if(!member.getMemberId().equals(replyEntity.getMemberEntity().getMemberId())){
            throw new BusinessException(ApiResponseCode.FAILED_UPDATE_REPLY, HttpStatus.BAD_REQUEST);
        }

        //댓글 삭제
        albumReplyRepository.deleteById(replyEntity.getAlbumReplySeq());
    }

    @Override
    public Page<AlbumReplyRes> getAlbumReplyRegList(AlbumEntity albumEntity, int nowPage) {
        //댓글 등록순으로 정렬(최신순)
        Pageable pageable =
                PageRequest.of(nowPage-1, PAGE_COUNT, Sort.Direction.DESC, "albumReplyRegdate");
        return albumReplyRepository.findAllByAlbum(albumEntity, pageable);
    }

    @Override
    public Page<AlbumReplyRes> getAlbumReplyLikeList(AlbumEntity albumEntity, int nowPage) {
        Pageable pageable =
                PageRequest.of(nowPage-1, PAGE_COUNT, Sort.Direction.DESC, "albumReplyLike");
        return albumReplyRepository.findAllByAlbum(albumEntity, pageable);
    }

    @Override
    public List<AlbumSingerEntity> getAlbumSingerList(AlbumEntity albumEntity) {
        return albumSingerRepository.findAllByAlbum(albumEntity);
    }

}