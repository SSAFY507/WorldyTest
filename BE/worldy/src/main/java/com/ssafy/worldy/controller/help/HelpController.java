package com.ssafy.worldy.controller.help;

import com.ssafy.worldy.model.help.dto.HelpCreateDto;
import com.ssafy.worldy.model.help.service.HelpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/help")
public class HelpController {

    @Autowired
    HelpService helpService;

    /***
     * [ 문의 글 작성 ]
     * - 문의 글 저장
     * - 글 작성 시간 자동 저장
     * - ElasticSearch 에 데이터 추가
     ***/
    @PostMapping("/write")
    public ResponseEntity<String> writeHelp(@RequestBody HelpCreateDto helpCreateDto) {

        String kakaoId = SecurityContextHolder.getContext().getAuthentication().getName();
        helpService.createHelp(helpCreateDto, kakaoId);

        return new ResponseEntity<>("success", HttpStatus.OK);
    }
}