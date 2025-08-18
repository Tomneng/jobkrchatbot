package backend.jobkrchatbot.llmservice.controller;

import backend.jobkrchatbot.common.dto.LlmRequest;
import backend.jobkrchatbot.common.dto.LlmResponse;
import backend.jobkrchatbot.common.response.ApiResponse;
import backend.jobkrchatbot.llmservice.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;

    @PostMapping("/generate")
    public ApiResponse<LlmResponse> generateResponse(@RequestBody LlmRequest request) {
        try {
            LlmResponse response = llmService.generateResponse(request);
            return ApiResponse.success(response, "응답이 성공적으로 생성되었습니다.");
        } catch (Exception e) {
            log.error("Error generating LLM response", e);
            return ApiResponse.error("응답 생성 중 오류가 발생했습니다.");
        }
    }
} 