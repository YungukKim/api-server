package com.surfer.apiserver.api.auth.controller;

import com.surfer.apiserver.api.auth.service.AuthService;
import com.surfer.apiserver.common.response.ApiResponseCode;
import com.surfer.apiserver.common.response.BaseResponse;
import com.surfer.apiserver.common.response.RestApiResponse;
import com.surfer.apiserver.common.util.AES256Util;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.surfer.apiserver.api.auth.dto.AuthDTO.*;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Auth API")
public class AuthController {
    private final AuthService authService;

    @PostMapping(value = "/sign-up", consumes = "application/json", produces = "application/json")
    @Operation(summary = "회원 가입", description = "회원 가입 시 요청되는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "회원가입 성공.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400 UNIQUE_PARAMETER_VIOLATION_EMAIL", description = "email 중복", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400 UNIQUE_PARAMETER_VIOLATION_NICKNAME", description = "nickname 중복", content = @Content(mediaType = "application/json"))
    })
    @Parameters({
            @Parameter(name = "email", description = "이메일", example = "test@test.com"),
            @Parameter(name = "password", description = "비밀번호", example = "test1234"),
            @Parameter(name = "nickname", description = "닉네임", example = "test nickname"),
            @Parameter(name = "name", description = "이름", example = "test name"),
    })
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        authService.signUp(signUpRequest);
        RestApiResponse restApiResponse = new RestApiResponse();
        restApiResponse.setResult(new BaseResponse(ApiResponseCode.REGISTER_SUCCESS));
        return new ResponseEntity<>(restApiResponse, HttpStatus.CREATED);
    }

    @PostMapping(value = "/sign-in", consumes = "application/json", produces = "application/json")
    @Operation(summary = "로그인", description = "로그인 시 요청되는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "OK", description = "요청에 성공하였습니다.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "4000", description = "데이터베이스 연결에 실패하였습니다.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "4011", description = "비밀번호 암호화에 실패하였습니다.", content = @Content(mediaType = "application/json"))
    })
    @Parameters({
            @Parameter(name = "email", description = "이메일", example = "chrome123@naver.com"),
            @Parameter(name = "password", description = "6자~12자 이내", example = "abcd1234"),
    })
    public ResponseEntity<?> signIn(@Valid @RequestBody(required = false) SignInRequest signInRequest,
                                    @RequestHeader Map<String, String> headers) {
        TokenInfo token = headers.containsKey("refresh-token") ?
                authService.signInByRefreshToken(headers.get("refresh-token")) :
                authService.signInByEmailAndPassword(signInRequest);

        RestApiResponse restApiResponse = new RestApiResponse();
        restApiResponse.setResult(new BaseResponse(ApiResponseCode.SUCCESS), token);
        return new ResponseEntity<>(restApiResponse, HttpStatus.OK);
    }

    @GetMapping(value = "/test")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> test() {
        RestApiResponse restApiResponse = new RestApiResponse();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        restApiResponse.setResult(new BaseResponse(ApiResponseCode.SUCCESS), AES256Util.decrypt(auth.getName()));
        return new ResponseEntity<>(restApiResponse, HttpStatus.OK);
    }

    @GetMapping(value = "/hi/{test}/bye")
    public ResponseEntity<?> test1(@PathVariable String test) {
        RestApiResponse restApiResponse = new RestApiResponse();
        restApiResponse.setResult(new BaseResponse(ApiResponseCode.SUCCESS), test);
        return new ResponseEntity<>(restApiResponse, HttpStatus.OK);
    }
}