package com.microfinance.util;

import com.microfinance.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberIdGenerator {

    private final MemberRepository memberRepository;

    /**
     * Generates the next member code as MEM0001, MEM0002, ... based on the current
     * row count. A small retry window guards against the rare race where two
     * requests generate the same code concurrently before either is committed -
     * the unique constraint on member_code will reject a duplicate and the caller
     * (MemberService) retries with the next sequence value.
     */
    public String generate(int attempt) {
        long nextSeq = memberRepository.count() + 1 + attempt;
        return String.format("MEM%04d", nextSeq);
    }
}
