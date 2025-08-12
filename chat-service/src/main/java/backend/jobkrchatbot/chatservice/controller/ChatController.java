package backend.jobkrchatbot.chatservice.controller;

import backend.jobkrchatbot.chatservice.application.dto.*;
import backend.jobkrchatbot.chatservice.application.service.ChatApplicationService;
import backend.jobkrchatbot.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatApplicationService chatApplicationService;

    @PostMapping("/start")
    public ApiResponse<ChatRoomResponse> startChat(@RequestBody StartChatRequest request) {
        try {
            ChatRoomResponse response = chatApplicationService.startChat(request);
            return ApiResponse.success(response, "채팅이 시작되었습니다.");
        } catch (Exception e) {
            log.error("Error starting chat", e);
            return ApiResponse.error("채팅 시작 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/{chatRoomId}/send")
    public ApiResponse<ChatResponse> sendMessage(@PathVariable String chatRoomId, @RequestBody SendMessageRequest request) {
        try {
            request.setChatRoomId(chatRoomId);
            ChatResponse response = chatApplicationService.sendMessage(request);
            return ApiResponse.success(response, "메시지가 성공적으로 처리되었습니다.");
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ApiResponse.error("메시지 전송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/{chatRoomId}/history")
    public ApiResponse<ChatHistoryResponse> getChatHistory(@PathVariable String chatRoomId) {
        try {
            ChatHistoryResponse response = chatApplicationService.getChatHistory(chatRoomId);
            return ApiResponse.success(response, "채팅 히스토리를 조회했습니다.");
        } catch (Exception e) {
            log.error("Error getting chat history", e);
            return ApiResponse.error("채팅 히스토리 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @DeleteMapping("/{chatRoomId}")
    public ApiResponse<String> endChat(@PathVariable String chatRoomId) {
        try {
            chatApplicationService.endChat(chatRoomId);
            return ApiResponse.success("채팅방이 종료되었습니다.", "채팅이 종료되었습니다.");
        } catch (Exception e) {
            log.error("Error ending chat", e);
            return ApiResponse.error("채팅 종료 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}/rooms")
    public ApiResponse<List<ChatRoomSummaryResponse>> getUserChatRooms(@PathVariable String userId) {
        try {
            List<ChatRoomSummaryResponse> response = chatApplicationService.getUserChatRooms(userId);
            return ApiResponse.success(response, "사용자의 채팅방 목록을 조회했습니다.");
        } catch (Exception e) {
            log.error("Error getting user chat rooms", e);
            return ApiResponse.error("채팅방 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 