package com.gcrf.library.recommend.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.recommend.dto.request.BatchRecommendRequest;
import com.gcrf.library.recommend.dto.response.RecommendStatsVO;
import com.gcrf.library.recommend.dto.response.RecommendationVO;
import com.gcrf.library.recommend.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书推荐控制器
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recommend")
@RequiredArgsConstructor
@Tag(name = "推荐管理", description = "图书推荐相关接口")
public class RecommendController {

    private final RecommendService recommendService;

    /**
     * 获取读者个性化推荐
     */
    @GetMapping("/books/{readerId}")
    @Operation(summary = "获取读者个性化推荐", description = "根据读者借阅历史生成个性化图书推荐")
    public Result<List<RecommendationVO>> getRecommendationsForReader(
            @Parameter(description = "读者ID") @PathVariable Long readerId,
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "20") Integer limit,
            @Parameter(description = "算法类型") @RequestParam(defaultValue = "HYBRID") String algorithm,
            @Parameter(description = "推荐场景") @RequestParam(defaultValue = "HOMEPAGE") String scene) {

        log.info("Getting recommendations for reader {}, limit={}, algorithm={}, scene={}",
                readerId, limit, algorithm, scene);

        List<RecommendationVO> recommendations = recommendService.recommendForReader(
                readerId, limit, algorithm, scene);

        return Result.success(recommendations);
    }

    /**
     * 获取热门图书
     */
    @GetMapping("/popular")
    @Operation(summary = "获取热门图书", description = "获取近期热门借阅的图书列表")
    public Result<List<RecommendationVO>> getPopularBooks(
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "20") Integer limit) {

        log.info("Getting popular books, limit={}", limit);

        List<RecommendationVO> popularBooks = recommendService.getPopularBooks(limit);

        return Result.success(popularBooks);
    }

    /**
     * 获取相似图书
     */
    @GetMapping("/similar/{bookId}")
    @Operation(summary = "获取相似图书", description = "根据图书相似度推荐类似的图书")
    public Result<List<RecommendationVO>> getSimilarBooks(
            @Parameter(description = "图书ID") @PathVariable Long bookId,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") Integer limit) {

        log.info("Getting similar books for book {}, limit={}", bookId, limit);

        List<RecommendationVO> similarBooks = recommendService.getSimilarBooks(bookId, limit);

        return Result.success(similarBooks);
    }

    /**
     * 批量生成推荐（管理后台）
     */
    @PostMapping("/batch")
    @Operation(summary = "批量生成推荐", description = "为多位读者批量生成推荐，用于管理后台")
    public Result<PageResult<RecommendationVO>> batchRecommend(
            @Valid @RequestBody BatchRecommendRequest request) {

        log.info("Batch recommend with request: {}", request);

        PageResult<RecommendationVO> result = recommendService.batchRecommend(request);

        return Result.success(result);
    }

    /**
     * 获取推荐效果统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取推荐效果统计", description = "获取推荐系统的效果统计数据")
    public Result<RecommendStatsVO> getRecommendStats(
            @Parameter(description = "统计天数") @RequestParam(defaultValue = "30") Integer days) {

        log.info("Getting recommendation stats for last {} days", days);

        RecommendStatsVO stats = recommendService.getRecommendStats(days);

        return Result.success(stats);
    }

    /**
     * 记录推荐点击
     */
    @PostMapping("/click")
    @Operation(summary = "记录推荐点击", description = "记录用户点击推荐图书的行为")
    public Result<Void> recordClick(
            @Parameter(description = "读者ID") @RequestParam Long readerId,
            @Parameter(description = "图书ID") @RequestParam Long bookId) {

        log.info("Recording click: reader={}, book={}", readerId, bookId);

        recommendService.recordClick(readerId, bookId);

        return Result.success();
    }

    /**
     * 记录推荐借阅
     */
    @PostMapping("/borrow")
    @Operation(summary = "记录推荐借阅", description = "记录用户借阅推荐图书的行为（用于计算转化率）")
    public Result<Void> recordBorrow(
            @Parameter(description = "读者ID") @RequestParam Long readerId,
            @Parameter(description = "图书ID") @RequestParam Long bookId) {

        log.info("Recording borrow: reader={}, book={}", readerId, bookId);

        recommendService.recordBorrow(readerId, bookId);

        return Result.success();
    }

    /**
     * 触发相似度矩阵重新计算
     */
    @PostMapping("/recompute")
    @Operation(summary = "重新计算相似度矩阵", description = "手动触发用户和物品相似度矩阵的重新计算")
    public Result<String> recomputeSimilarityMatrix() {

        log.info("Triggering similarity matrix recomputation");

        recommendService.recomputeSimilarityMatrix();

        return Result.success("相似度矩阵重新计算已触发", "相似度矩阵重新计算已触发");
    }
}
