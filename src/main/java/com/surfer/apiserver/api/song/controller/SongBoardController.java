package com.surfer.apiserver.api.song.controller;

import java.util.*;
import com.surfer.apiserver.api.album.service.AlbumService;
import com.surfer.apiserver.api.song.dto.ProducerDTO;
import com.surfer.apiserver.api.song.dto.SongRes;
import com.surfer.apiserver.api.song.dto.SongReplyReq;
import com.surfer.apiserver.api.song.dto.SongReplyRes;
import com.surfer.apiserver.api.song.service.SongBoardService;
import com.surfer.apiserver.api.song.service.SongService;
import com.surfer.apiserver.common.exception.BusinessException;
import com.surfer.apiserver.common.response.ApiResponseCode;
import com.surfer.apiserver.common.response.BaseResponse;
import com.surfer.apiserver.common.response.RestApiResponse;
import com.surfer.apiserver.common.util.AES256Util;
import com.surfer.apiserver.domain.database.entity.MemberEntity;
import com.surfer.apiserver.domain.database.entity.SongEntity;
import com.surfer.apiserver.domain.database.entity.SongSingerEntity;
import com.surfer.apiserver.domain.database.repository.MemberRepository;
import com.surfer.apiserver.domain.database.repository.SongLikeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.List;

@RestController
@RequestMapping("/api/song")
public class SongBoardController {

    @Autowired
    private SongBoardService songBoardService;
    @Autowired
    private AlbumService albumService;
    @Autowired
    private SongService songService;
    @Autowired
    private SongLikeRepository songLikeRepository;
    @Autowired
    private MemberRepository memberRepository;

    /**
     * 곡 정보 상세보기
     */
    @GetMapping("/detail/{seq}")
    @Operation(summary = "곡 상세페이지", description = "곡 상세페이지 조회 시 요청되는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청이 성공적으로 처리되었습니다.", content = @Content(mediaType = "application/json"))
    })
    @Parameters({
            @Parameter(name = "nowPage", description = "현재 댓글 페이지", example = "1"),
            @Parameter(name = "sort", description = "정렬 기준", example = "Like or RegDate")
    })
    public ResponseEntity<?> read(@PathVariable Long seq, @RequestParam(defaultValue = "1") int nowPage,
                                  @RequestParam(defaultValue = "regDate") String sort,
                                  @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        //nowPage: 댓글페이징을 위해 추가
        //sort: 최신순/추천순 정렬 구분을 위해 추가

        //seq에 해당하는 song 찾기
        SongEntity songEntity = songBoardService.selectById(seq);

        Page<SongReplyRes> pageReplyList;

        //댓글 페이징 처리
        if ("Like".equals(sort)) {
            //추천순 정렬
            pageReplyList = songBoardService.getSongReplyLikeList(songEntity, nowPage);
        } else {
            //최신순 정렬
            pageReplyList = songBoardService.getSongReplyRegList(songEntity, nowPage);
        }

        //가수 목록 불러오기
        List<SongSingerEntity> songSingerList = songBoardService.getSongSingerList(songEntity);

        //작곡가, 작사가, 편곡자 분리하기
        ProducerDTO producer = songBoardService.getProducer(songEntity.getProducer());

        SongRes songDTO = new SongRes(songEntity, pageReplyList, songSingerList, producer);

        //앨범 이미지 url
        URL albumImgFileUrl = albumService.generateAlbumImgFileUrl(songDTO.getAlbumImage());
        songDTO.setAlbumImage(albumImgFileUrl.toString());

        //음원 url
        URL songFileUrl = songService.generateSongFileUrl(songDTO.getSoundSourceName());
        songDTO.setSoundSourceUrl(songFileUrl.toString());


        songDTO.setIsLiked(authorizationHeader !=null &&
                !songLikeRepository.findBySongAndMember(songEntity,
                        memberRepository.findById(Long.parseLong(AES256Util.decrypt(authorizationHeader)))
                                .orElseThrow(() -> new BusinessException(ApiResponseCode.NOT_FOUND, HttpStatus.OK))).isEmpty());

        songDTO.setLikeCount(songLikeRepository.countBySong(songEntity));

        RestApiResponse restApiResponse = new RestApiResponse();
        restApiResponse.setResult(new BaseResponse(ApiResponseCode.SUCCESS), songDTO);
        return new ResponseEntity<>(restApiResponse, HttpStatus.OK);
    }

    @PostMapping("/{seq}/reply")
    @Operation(summary = "곡 상세페이지 댓글 작성", description = "댓글 작성 시 요청되는 api")
    public ResponseEntity<?> insertSongReply(@PathVariable Long seq, @RequestBody SongReplyReq songReply) {
        //sercurity에 저장된 member 정보 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String memberSeq = AES256Util.decrypt(authentication.getName());


        songBoardService.songReplyInsert(songReply, Long.parseLong(memberSeq), seq);

        RestApiResponse restApiResponse = new RestApiResponse();
        restApiResponse.setResult(new BaseResponse(ApiResponseCode.SUCCESS));
        return new ResponseEntity<>(restApiResponse, HttpStatus.CREATED);
    }

    @PutMapping("/{seq}/reply/{replySeq}")
    @Operation(summary = "곡 상세페이지 댓글 수정", description = "댓글 수정 시 요청되는 api")
    public ResponseEntity<?> updateSongReply(@PathVariable Long seq, @RequestBody SongReplyReq songReplyReq,
                                             @PathVariable Long replySeq) {
        //sercurity에 저장된 member 정보 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String memberSeq = AES256Util.decrypt(authentication.getName());

        songBoardService.songReplyUpdate(seq, songReplyReq, Long.parseLong(memberSeq), replySeq);

        RestApiResponse restApiResponse = new RestApiResponse();
        restApiResponse.setResult(new BaseResponse(ApiResponseCode.SUCCESS));
        return new ResponseEntity<>(restApiResponse, HttpStatus.OK);
    }

    @DeleteMapping("/{seq}/reply/{replySeq}")
    @Operation(summary = "곡 상세페이지 댓글 삭제", description = "댓글 삭제 시 요청되는 api")
    public ResponseEntity<?> deleteSongReply(@PathVariable Long seq, @PathVariable Long replySeq) {
        //sercurity에 저장된 member 정보 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String memberSeq = AES256Util.decrypt(authentication.getName());

        songBoardService.songReplyDelete(seq, Long.parseLong(memberSeq), replySeq);

        RestApiResponse restApiResponse = new RestApiResponse();
        restApiResponse.setResult(new BaseResponse(ApiResponseCode.SUCCESS));
        return new ResponseEntity<>(restApiResponse, HttpStatus.OK);
    }


    @GetMapping("/{seq}/reply/{replySeq}/like")
    @Operation(summary = "곡 상세페이지 댓글 좋아요 여부 조회", description = "댓글 좋아요 여부 확인 시 요청되는 api")
    public ResponseEntity<?> checkSongReplyLike(@PathVariable Long seq, @PathVariable Long replySeq) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String memberSeq = AES256Util.decrypt(authentication.getName());

        Boolean isLike = songBoardService.songReplyLike(seq, Long.parseLong(memberSeq), replySeq);

        RestApiResponse restApiResponse = new RestApiResponse();
        restApiResponse.setResult(new BaseResponse(ApiResponseCode.SUCCESS), isLike);
        return new ResponseEntity<>(restApiResponse, HttpStatus.OK);
    }

    @PutMapping("/{seq}/reply/{replySeq}/like")
    @Operation(summary = "곡 상세페이지 댓글 좋아요 등록", description = "댓글 좋아요 시 요청되는 api")
    public ResponseEntity<?> increaseSongReplyLike(@PathVariable Long seq, @PathVariable Long replySeq) {
        //security에 저장된 member 정보 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String memberSeq = AES256Util.decrypt(authentication.getName());

        //곡 댓글 좋아요 테이블 수정
        songBoardService.songReplyLikeInsert(seq, Long.parseLong(memberSeq), replySeq);

        //곡 댓글 테이블 수정
        songBoardService.songReplyLikeUpdate(replySeq, Boolean.TRUE);

        RestApiResponse restApiResponse = new RestApiResponse();
        restApiResponse.setResult(new BaseResponse(ApiResponseCode.SUCCESS));
        return new ResponseEntity<>(restApiResponse, HttpStatus.OK);
    }

    @DeleteMapping("/{seq}/reply/{replySeq}/like")
    @Operation(summary = "곡 상세페이지 댓글 좋아요 삭제", description = "댓글 좋아요 취소 시 요청되는 api")
    public ResponseEntity<?> decreaseSongReplyLike(@PathVariable Long seq, @PathVariable Long replySeq) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String memberSeq = AES256Util.decrypt(authentication.getName());

        //곡 댓글 좋아요 테이블 수정
        songBoardService.songReplyLikeDelete(seq, Long.parseLong(memberSeq), replySeq);

        //곡 댓글 테이블 수정
        songBoardService.songReplyLikeUpdate(replySeq, Boolean.FALSE);

        RestApiResponse restApiResponse = new RestApiResponse();
        restApiResponse.setResult(new BaseResponse(ApiResponseCode.SUCCESS));
        return new ResponseEntity<>(restApiResponse, HttpStatus.OK);
    }

    @GetMapping("/rank")
    @Operation(summary = "조회수 기반 곡의 순위 반환", description = "조회수를 기반으로 10곡의 순위와 정보를 반환하는 API")
    public ResponseEntity<?> getSongRank() {
        return ResponseEntity.ok().body(new RestApiResponse(new BaseResponse(ApiResponseCode.SUCCESS), songService.getSongRank()));
    }

    @GetMapping("/all")
    @Operation(summary = "전체 곡 리스트 반환")
    public ResponseEntity<?> getAllSongs() {
        return ResponseEntity.ok().body(new RestApiResponse(new BaseResponse(ApiResponseCode.SUCCESS), songService.getAllSongs()));
    }

    @GetMapping("/genre/{genre}")
    @Operation(summary = "장르 별 곡 리스트 반환")
    public ResponseEntity<?> getSongsByGenre(@PathVariable String genre) {
        return genre.equals("전체") ? ResponseEntity.ok().body(new RestApiResponse(new BaseResponse(ApiResponseCode.SUCCESS), songService.getAllSongs())) :
                ResponseEntity.ok().body(new RestApiResponse(new BaseResponse(ApiResponseCode.SUCCESS), songService.getAllSongsByGenre(genre)));
    }


}
