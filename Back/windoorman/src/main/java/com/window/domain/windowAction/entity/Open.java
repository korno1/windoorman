package com.window.domain.windowAction.entity;

import lombok.Getter;

@Getter
public enum Open {
    // [수정] 각 Enum 값에 한글 표시 이름을 추가하여, Report 서비스 등에서 하드코딩 없이 값을 사용하도록 개선
    open("열림"),
    close("닫힘");

    private final String displayName;

    Open(String displayName) {
        this.displayName = displayName;
    }
}