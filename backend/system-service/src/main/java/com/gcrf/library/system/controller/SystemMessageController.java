package com.gcrf.library.system.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.system.entity.SystemMessage;
import com.gcrf.library.system.service.SystemMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 系统消息控制器
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system/messages")
@RequiredArgsConstructor
@Tag(name = "系统消息", description = "用户消息列表与已读状态管理")
public class SystemMessageController {

    private final SystemMessageService messageService;

    /**
     * 分页查询用户消息列表
     */
    @GetMapping
    @Operation(summary = "查询消息列表", description = "按用户ID分页查询系统消息")
    public Result<PageResult<SystemMessage>> list(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        log.info("查询消息列表, userId: {}, pageNum: {}, pageSize: {}", userId, pageNum, pageSize);
        return Result.success(messageService.listByUser(userId, pageNum, pageSize));
    }

    /**
     * 查询用户未读消息数量
     */
    @GetMapping("/unread-count")
    @Operation(summary = "未读消息数", description = "返回指定用户的未读消息总数")
    public Result<Long> unreadCount(@RequestParam Long userId) {
        return Result.success(messageService.countUnread(userId));
    }

    /**
     * 将指定消息标记为已读
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "标记已读", description = "将指定消息标记为已读")
    public Result<Void> markAsRead(@PathVariable Long id) {
        log.info("标记消息已读, id: {}", id);
        messageService.markAsRead(id);
        return Result.success();
    }
}
