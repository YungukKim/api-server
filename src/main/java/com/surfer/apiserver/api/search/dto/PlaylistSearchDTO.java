package com.surfer.apiserver.api.search.dto;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PlaylistSearchDTO {

    private String playListName;
    private String albumImage;
    private String memberName;
    private Long playlistGroupSeq;
    private int albumState;

    //플레이리스트 3
    public PlaylistSearchDTO(String playListName, String albumImage, String memberName,Long playlistGroupSeq,int albumState) {
        this.playListName = playListName;
        this.albumImage = albumImage;
        this.memberName = memberName;
        this.playlistGroupSeq = playlistGroupSeq;
        this.albumState = albumState;
    }

}
