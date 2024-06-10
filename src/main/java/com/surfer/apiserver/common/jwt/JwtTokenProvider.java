package com.surfer.apiserver.common.jwt;

import com.surfer.apiserver.api.auth.dto.AuthDTO.TokenInfo;
import com.surfer.apiserver.common.exception.BusinessException;
import com.surfer.apiserver.common.response.ApiResponseCode;
import com.surfer.apiserver.common.util.AES256Util;
import com.surfer.apiserver.domain.database.entity.MemberEntity;
import com.surfer.apiserver.domain.database.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class JwtTokenProvider implements InitializingBean {

    private final MemberRepository memberRepository;

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.access-token-validity-in-second}")
    private long accessTokenValidityInSecond;
    @Value("${jwt.refresh-token-validity-in-second}")
    private long refreshTokenValidityInSecond;
    @Value("${jwt.issuer}")
    private String issuer;
    @Value("${jwt.access-token-header}")
    private String accessTokenHeader;
    private SecretKey key;

    @Override
    public void afterPropertiesSet() throws Exception {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public TokenInfo createToken(MemberEntity member) {
        String accessToken = Jwts.builder()
                .issuer(issuer)
                .subject("authentication.getName()")
                .claim("user", AES256Util.encrypt(String.valueOf(member.getMemberSeq())))
                .claim("authorities", member.getRole())
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + accessTokenValidityInSecond))
                .signWith(key)
                .compact();

        String refreshToken = getRefreshToken(member.getMemberSeq());
        return TokenInfo.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public Claims validateToken(String token) throws Exception {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getAccessTokenByRequestHeader(HttpServletRequest request) throws Exception {
        return request.getHeader(accessTokenHeader).split(" ")[1];
    }


    private String getRefreshToken(Long memberSeq) {
        MemberEntity memberEntity = memberRepository.findByMemberSeq(memberSeq).orElseThrow(
                () -> new BusinessException(ApiResponseCode.INVALID_USER_ID, HttpStatus.BAD_REQUEST));
        if (memberEntity.getRefreshTokenExpiredAt() == null
                || memberEntity.getRefreshTokenExpiredAt() >= new Date().getTime() + refreshTokenValidityInSecond
                || memberEntity.getRefreshToken() == null) {
            memberEntity.setRefreshTokenExpiredAt(System.currentTimeMillis() + refreshTokenValidityInSecond);
            memberEntity.setRefreshToken(UUID.randomUUID().toString());
            memberEntity = memberRepository.save(memberEntity);
        }
        return memberEntity.getRefreshToken();
    }

}




    /*public TokenInfo createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream().findFirst().get().toString();

        String accessToken = Jwts.builder()
                .issuer(issuer)
                .subject("authentication.getName()")
                .claim("user", authentication.getName())
                .claim("authorities", authorities)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + accessTokenValidityInSecond))
                .signWith(key)
                .compact();
        String refreshToken = getRefreshToken(authentication.getName());
        return TokenInfo.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }*/
