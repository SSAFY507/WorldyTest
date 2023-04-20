package com.ssafy.worldy.model.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.worldy.jwt.TokenProvider;
import com.ssafy.worldy.model.user.dto.KakaoLoginDto;
import com.ssafy.worldy.model.user.dto.TokenDto;
import com.ssafy.worldy.model.user.entity.Authority;
import com.ssafy.worldy.model.user.entity.User;
import com.ssafy.worldy.model.user.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@RequiredArgsConstructor
@Service
public class KakaoUserService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepo userRepo;

    private final TokenProvider tokenProvider;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final static String ROLE_USER = "ROLE_USER";

    @Value("${kakao.client.id}")
    private String clientId;

    @Transactional
    public KakaoLoginDto kakaoLogin(String code) throws JsonProcessingException {

        // 1. 인가코드로 access 토큰 요청
        String accessToken = getAccessToken(code, "http://localhost:3000/user/kakao/callback");

        System.out.println("===== 인가코드 =====" + accessToken);

        // 2. 없는 회원의 경우 회원가입
        User user = registKakaoUser(accessToken);

        // 3. 로그인 JWT 토큰 발행
        TokenDto tokenDto = createToken(user);

        return new KakaoLoginDto(tokenDto, user.getKakaoId(), user.getProfileImg());
    }

    // access, refresh 토큰 발급 및 저장
    public TokenDto createToken(User user) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(user.getKakaoId(), user.getKakaoId());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.createAccessToken(authentication);
        String refreshToken = tokenProvider.createRefreshToken();

        user.updateToken(refreshToken);

        return TokenDto.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }

    // Kakao code로 Kakao token 발급
    private String getAccessToken(String code, String redirectUri) throws JsonProcessingException {

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        System.out.println("headers : " + headers);
        System.out.println("body : " + body);

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(body, headers);

        RestTemplate rt = new RestTemplate();
//        ResponseEntity<String> stringResponseEntity = rt.postForEntity("https://kauth.kakao.com/oauth/token", kakaoTokenRequest, String.class);
        ResponseEntity<String> response = rt.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("access_token").asText();
    }

    // 처음 로그인 시 회원 가입
    public User registKakaoUser(String accessToken) throws JsonProcessingException {

        JsonNode jsonNode = getKakaoUserInfo(accessToken);

        // kakaoId 중복 확인
        String kakaoId = String.valueOf(jsonNode.get("id").asLong());
        User user = userRepo.findByKakaoId(kakaoId).orElse(null);

        // 회원가입
        if (user == null) {
            String profileImg = jsonNode.get("properties").get("profile_image").asText();
            String age = null;
            String gender = null;

            if(jsonNode.get("kakao_account").has("age_range")) {
                age = jsonNode.get("kakao_account").get("age_range").asText();

                if(age.equals("10~19")) age = "10S";
                else if(age.equals("20~29")) age = "20S";
                else if (age.equals("30~39")) age = "30S";
                else if (age.equals("40~49")) age = "40S";
                else if (age.equals("50~59")) age = "50S";
                else if (age.equals("60~69")) age = "60S";
            }

            if(jsonNode.get("kakao_account").has("gender")) {
                gender = jsonNode.get("kakao_account").get("gender").asText();
            }

            String encodedPassword = passwordEncoder.encode(kakaoId);

            Authority authority = Authority.builder()
                    .authorityName(ROLE_USER)
                    .build();

            user = User.builder()
                    .kakaoId(kakaoId)
                    .password(encodedPassword)
                    .profileImg(profileImg)
                    .age(age)
                    .sex(gender)
                    .activated(true)
                    .authorities(Collections.singleton(authority)).build();

            userRepo.save(user);
        }

        return user;
    }

    // kakao 토큰으로 동의 항목 가져오기
    private JsonNode getKakaoUserInfo(String accessToken) throws JsonProcessingException {

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                kakaoUserInfoRequest,
                String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(responseBody);
    }
}
