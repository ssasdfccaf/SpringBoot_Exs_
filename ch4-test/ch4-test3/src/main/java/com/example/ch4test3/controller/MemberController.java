package com.example.ch4test3.controller;


import com.example.ch4test3.dto.MemberFormDto;
import com.example.ch4test3.entity.Member;
import com.example.ch4test3.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@RequestMapping("/members")
@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;

    //회원가입 폼
    @GetMapping(value = "/new")
    public String memberForm(Model model){
        // 서버 -> 뷰 , 데이터만 전달.
        model.addAttribute("memberFormDto", new MemberFormDto());
        // 뷰 리졸버, 타임리프 사용해서, 회원 가입 폼 html로 전달.
        return "member/memberForm";
    }

    // 회원가입 처리
    @PostMapping(value = "/new")
    // 기본적인 유효성 체크, 값의 유무
    // BindingResult -> 유효성 체크에서 , 오류가 발생시, 메세지 결과 확인하는 용도
    public String newMember(@Valid MemberFormDto memberFormDto, BindingResult bindingResult, Model model){

        if(bindingResult.hasErrors()){
            System.out.println("bindingResult toString() : " + bindingResult.toString());
            return "member/memberForm";
        }

        try {
            Member member = Member.createMember(memberFormDto, passwordEncoder);
            memberService.saveMember(member);
        } catch (IllegalStateException e){
            model.addAttribute("errorMessage", e.getMessage());
            return "member/memberForm";
        }

        return "redirect:/";
    }

    // 로그인 폼만 제공, 실제 처리는 시큐리티가 처리.
    @GetMapping(value = "/login")
    public String loginMember(){
        return "/member/memberLoginForm";
    }

    // 로그인 실패시, 이동할 에러페이지 설정.
    @GetMapping(value = "/login/error")
    public String loginError(Model model){
        model.addAttribute("loginErrorMsg", "아이디 또는 비밀번호를 확인해주세요");
        return "/member/memberLoginForm";
    }

}