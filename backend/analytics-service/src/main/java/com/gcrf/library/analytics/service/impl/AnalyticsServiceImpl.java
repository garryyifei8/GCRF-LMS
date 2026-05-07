package com.gcrf.library.analytics.service.impl;

import com.gcrf.library.analytics.client.BookServiceClient;
import com.gcrf.library.analytics.client.CirculationServiceClient;
import com.gcrf.library.analytics.client.ReaderServiceClient;
import com.gcrf.library.analytics.dto.request.RankingQueryRequest;
import com.gcrf.library.analytics.dto.request.TrendQueryRequest;
import com.gcrf.library.analytics.dto.response.*;
import com.gcrf.library.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数据分析服务实现类
 *
 * 注意：当前实现使用模拟数据，生产环境需要通过Feign Client从各服务获取真实数据
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final BookServiceClient bookServiceClient;
    private final CirculationServiceClient circulationServiceClient;
    private final ReaderServiceClient readerServiceClient;

    // 分类颜色映射
    private static final Map<String, String> CATEGORY_COLORS = Map.of(
            "I", "#5470c6",
            "TP", "#91cc75",
            "K", "#fac858",
            "F", "#ee6666",
            "B", "#73c0de",
            "J", "#3ba272",
            "H", "#fc8452",
            "G", "#9a60b4",
            "O", "#ea7ccc",
            "D", "#8b5cf6"
    );

    // 分类名称映射
    private static final Map<String, String> CATEGORY_NAMES = Map.of(
            "I", "文学",
            "TP", "工业技术",
            "K", "历史、地理",
            "F", "经济",
            "B", "哲学、宗教",
            "J", "艺术",
            "H", "语言、文字",
            "G", "文化、教育",
            "O", "数理科学",
            "D", "政治、法律"
    );

    @Override
    public OverviewVO getOverview() {
        log.info("获取总览统计数据");

        // TODO: 生产环境应通过Feign Client从各服务聚合数据
        // 当前返回模拟数据
        return OverviewVO.builder()
                .totalBooks(10000L)
                .totalCopies(25000L)
                .totalReaders(5000L)
                .totalVisits(125000L)
                .booksPerReader(BigDecimal.valueOf(5.0))
                .visitsPerReader(BigDecimal.valueOf(25.0))
                .currentBorrowed(2500L)
                .availableCopies(22500L)
                .overdueCount(350L)
                .reservationCount(180L)
                .todayVisits(randomLong(280, 350))
                .todayBorrowed(randomLong(70, 100))
                .todayReturned(randomLong(75, 110))
                .todayNewReaders(randomLong(3, 10))
                .thisMonthBorrowed(1800L)
                .thisMonthReturned(1650L)
                .thisMonthVisits(8500L)
                .thisMonthNewBooks(120L)
                .circulationRate(BigDecimal.valueOf(0.65))
                .zeroCirculationCount(800L)
                .zeroCirculationRate(BigDecimal.valueOf(0.08))
                .borrowGrowth(BigDecimal.valueOf(0.15))
                .visitsGrowth(BigDecimal.valueOf(0.12))
                .readerGrowth(BigDecimal.valueOf(0.08))
                .build();
    }

    @Override
    public List<BorrowTrendVO> getBorrowTrends(TrendQueryRequest request) {
        log.info("获取借阅趋势数据, timeRange: {}, granularity: {}",
                request.getTimeRange(), request.getGranularity());

        List<BorrowTrendVO> trends = new ArrayList<>();
        int days = getDaysFromTimeRange(request.getTimeRange());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == DayOfWeek.SUNDAY;

            // 周末和工作日有不同的流量模式
            long borrowedMin = isWeekend ? 30 : 60;
            long borrowedMax = isWeekend ? 80 : 150;
            long returnedMin = isWeekend ? 25 : 55;
            long returnedMax = isWeekend ? 75 : 145;
            long visitsMin = isWeekend ? 100 : 200;
            long visitsMax = isWeekend ? 300 : 500;

            trends.add(BorrowTrendVO.builder()
                    .date(date)
                    .dateStr(date.format(formatter))
                    .borrowed(randomLong(borrowedMin, borrowedMax))
                    .returned(randomLong(returnedMin, returnedMax))
                    .visits(randomLong(visitsMin, visitsMax))
                    .newReaders(randomLong(0, isWeekend ? 3 : 8))
                    .reserved(randomLong(5, 25))
                    .renewed(randomLong(3, 15))
                    .build());
        }

        return trends;
    }

    @Override
    public List<PopularBookVO> getPopularBooks(RankingQueryRequest request) {
        log.info("获取热门图书排行, rankBy: {}, limit: {}", request.getRankBy(), request.getLimit());

        List<PopularBookVO> books = new ArrayList<>();

        String[] titles = {
                "三体", "活着", "百年孤独", "Python编程:从入门到实践", "算法导论",
                "深入理解计算机系统", "人类简史", "未来简史", "原则", "自控力",
                "围城", "平凡的世界", "红楼梦", "西游记", "三国演义",
                "水浒传", "追风筝的人", "解忧杂货店", "白夜行", "嫌疑人X的献身"
        };

        String[] authors = {
                "刘慈欣", "余华", "加西亚·马尔克斯", "Eric Matthes", "Thomas H. Cormen",
                "Randal E. Bryant", "尤瓦尔·赫拉利", "尤瓦尔·赫拉利", "瑞·达利欧", "凯利·麦格尼格尔",
                "钱钟书", "路遥", "曹雪芹", "吴承恩", "罗贯中",
                "施耐庵", "卡勒德·胡赛尼", "东野圭吾", "东野圭吾", "东野圭吾"
        };

        String[] categories = {"I", "I", "I", "TP", "TP", "TP", "K", "K", "F", "B",
                "I", "I", "I", "I", "I", "I", "I", "I", "I", "I"};

        int limit = Math.min(request.getLimit(), titles.length);

        for (int i = 0; i < limit; i++) {
            String categoryCode = categories[i];
            int totalCopies = randomInt(5, 15);
            int borrowedCopies = randomInt(2, totalCopies);

            books.add(PopularBookVO.builder()
                    .rank(i + 1)
                    .bookId((long) (i + 1))
                    .isbn("978" + String.format("%010d", randomLong(1000000000L, 9999999999L)))
                    .title(titles[i])
                    .author(authors[i])
                    .categoryCode(categoryCode)
                    .categoryName(CATEGORY_NAMES.getOrDefault(categoryCode, "其他"))
                    .coverUrl("https://picsum.photos/seed/" + i + "/100/150")
                    .borrowCount(randomLong(500 - i * 20, 600 - i * 15))
                    .rating(BigDecimal.valueOf(randomDouble(3.5, 5.0)).setScale(1, RoundingMode.HALF_UP))
                    .totalCopies(totalCopies)
                    .availableCopies(totalCopies - borrowedCopies)
                    .borrowedCopies(borrowedCopies)
                    .reservationCount(randomInt(0, 10))
                    .build());
        }

        return books;
    }

    @Override
    public List<ActiveReaderVO> getActiveReaders(RankingQueryRequest request) {
        log.info("获取活跃读者排行, rankBy: {}, limit: {}", request.getRankBy(), request.getLimit());

        List<ActiveReaderVO> readers = new ArrayList<>();

        String[] names = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十",
                "郑十一", "王小明", "李小红", "张小华", "刘小伟", "陈小芳", "杨小军",
                "黄小燕", "周小敏", "吴小强", "林小丽", "何小刚"};

        String[] types = {"student", "teacher", "staff"};
        String[] typeNames = {"学生", "教师", "职工"};
        String[] categories = {"文学", "工业技术", "历史地理", "经济", "哲学宗教"};

        int limit = Math.min(request.getLimit(), names.length);

        for (int i = 0; i < limit; i++) {
            int typeIndex = i < 15 ? 0 : (i < 18 ? 1 : 2);

            readers.add(ActiveReaderVO.builder()
                    .rank(i + 1)
                    .readerId((long) (i + 1))
                    .cardNo(String.format("RD%08d", i + 1))
                    .realName(names[i])
                    .readerType(types[typeIndex])
                    .readerTypeName(typeNames[typeIndex])
                    .avatar("https://i.pravatar.cc/100?img=" + (i + 1))
                    .borrowCount(randomLong(200 - i * 8, 250 - i * 10))
                    .visitCount(randomLong(500 - i * 20, 600 - i * 25))
                    .favoriteCategory(categories[i % categories.length])
                    .lastBorrowDate(LocalDate.now().minusDays(randomInt(0, 7)).atStartOfDay())
                    .currentBorrowCount(randomInt(0, 10))
                    .overdueCount(randomInt(0, 3))
                    .build());
        }

        return readers;
    }

    @Override
    public List<CategoryDistributionVO> getCategoryDistribution() {
        log.info("获取分类分布数据");

        List<CategoryDistributionVO> distributions = new ArrayList<>();
        long totalBooks = 10000L;

        List<Map.Entry<String, String>> entries = new ArrayList<>(CATEGORY_NAMES.entrySet());

        for (int i = 0; i < entries.size(); i++) {
            String code = entries.get(i).getKey();
            String name = entries.get(i).getValue();
            long bookCount = randomLong(500, 2000);

            distributions.add(CategoryDistributionVO.builder()
                    .code(code)
                    .name(name)
                    .color(CATEGORY_COLORS.getOrDefault(code, "#999999"))
                    .bookCount(bookCount)
                    .borrowCount(randomLong(200, (long) (bookCount * 1.5)))
                    .circulationRate(BigDecimal.valueOf(randomDouble(0.3, 0.9)).setScale(2, RoundingMode.HALF_UP))
                    .readerCount(randomLong(100, 800))
                    .percentage(BigDecimal.valueOf((double) bookCount / totalBooks).setScale(3, RoundingMode.HALF_UP))
                    .zeroCirculationCount(randomLong(10, 100))
                    .zeroCirculationRate(BigDecimal.valueOf(randomDouble(0.02, 0.15)).setScale(3, RoundingMode.HALF_UP))
                    .build());
        }

        return distributions;
    }

    @Override
    public HeatmapDataVO getReaderActivityHeatmap() {
        log.info("获取读者活跃度热力图数据");

        List<String> hours = Arrays.asList(
                "8:00", "9:00", "10:00", "11:00", "12:00", "13:00",
                "14:00", "15:00", "16:00", "17:00", "18:00", "19:00"
        );
        List<String> days = Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日");

        List<int[]> data = new ArrayList<>();
        int minValue = Integer.MAX_VALUE;
        int maxValue = Integer.MIN_VALUE;

        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            for (int hourIndex = 0; hourIndex < hours.size(); hourIndex++) {
                // 模拟不同时段的活跃度
                // 周末和工作日模式不同，午休和下午时段较活跃
                boolean isWeekend = dayIndex >= 5;
                boolean isPeakHour = (hourIndex >= 2 && hourIndex <= 4) || (hourIndex >= 6 && hourIndex <= 8);

                int baseValue = isWeekend ? 40 : 60;
                int peakBonus = isPeakHour ? 30 : 0;
                int value = randomInt(baseValue + peakBonus - 20, baseValue + peakBonus + 20);

                value = Math.max(0, Math.min(100, value));
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);

                data.add(new int[]{hourIndex, dayIndex, value});
            }
        }

        return HeatmapDataVO.builder()
                .hours(hours)
                .days(days)
                .data(data)
                .minValue(minValue)
                .maxValue(maxValue)
                .build();
    }

    @Override
    public List<CategoryDistributionVO> getCategoryStats() {
        log.info("获取分类统计数据");
        // 复用分类分布逻辑，返回相同结构的数据
        return getCategoryDistribution();
    }

    @Override
    public CollectionAnalysisVO getCollectionAnalysis() {
        log.info("获取馆藏分析数据");

        List<CollectionAnalysisVO.StatusItem> statusDistribution = List.of(
                CollectionAnalysisVO.StatusItem.builder()
                        .status("available").statusName("在架")
                        .count(520L).percentage(BigDecimal.valueOf(0.945)).build(),
                CollectionAnalysisVO.StatusItem.builder()
                        .status("borrowed").statusName("借出")
                        .count(15L).percentage(BigDecimal.valueOf(0.027)).build(),
                CollectionAnalysisVO.StatusItem.builder()
                        .status("overdue").statusName("逾期")
                        .count(10L).percentage(BigDecimal.valueOf(0.018)).build(),
                CollectionAnalysisVO.StatusItem.builder()
                        .status("lost").statusName("遗失")
                        .count(5L).percentage(BigDecimal.valueOf(0.009)).build()
        );

        List<CollectionAnalysisVO.AgeItem> ageDistribution = List.of(
                CollectionAnalysisVO.AgeItem.builder()
                        .range("0-1年").count(30L).percentage(BigDecimal.valueOf(0.6)).build(),
                CollectionAnalysisVO.AgeItem.builder()
                        .range("1-3年").count(15L).percentage(BigDecimal.valueOf(0.3)).build(),
                CollectionAnalysisVO.AgeItem.builder()
                        .range("3-5年").count(5L).percentage(BigDecimal.valueOf(0.1)).build()
        );

        CollectionAnalysisVO.CirculationStats circulationStats = CollectionAnalysisVO.CirculationStats.builder()
                .highCirculation(5L)
                .mediumCirculation(15L)
                .lowCirculation(10L)
                .zeroCirculation(20L)
                .build();

        return CollectionAnalysisVO.builder()
                .totalBooks(50L)
                .totalCopies(572L)
                .categoryDistribution(getCategoryStats())
                .statusDistribution(statusDistribution)
                .ageDistribution(ageDistribution)
                .circulationAnalysis(circulationStats)
                .build();
    }

    @Override
    public List<RecentActivityVO> getRecentActivities(int limit) {
        log.info("获取近期活动记录, limit={}", limit);

        // 夹紧 limit：<=0 默认 20，超过 100 截断至 100
        int effective = (limit <= 0) ? 20 : Math.min(limit, 100);

        List<RecentActivityVO> allActivities = List.of(
                RecentActivityVO.builder().id(1L).type("borrow").typeName("借书").icon("DocumentAdd")
                        .readerName("韩雅静").bookTitle("罪与罚")
                        .description("借书《罪与罚》").timestamp("2026-05-06T11:30:00").status("success").build(),
                RecentActivityVO.builder().id(2L).type("return").typeName("还书").icon("DocumentDelete")
                        .readerName("李明").bookTitle("三体")
                        .description("还书《三体》").timestamp("2026-05-06T10:45:00").status("success").build(),
                RecentActivityVO.builder().id(3L).type("register").typeName("新读者").icon("UserFilled")
                        .readerName("张晓雯").bookTitle("")
                        .description("新读者注册：张晓雯").timestamp("2026-05-06T10:15:00").status("success").build(),
                RecentActivityVO.builder().id(4L).type("renew").typeName("续借").icon("Refresh")
                        .readerName("王建国").bookTitle("百年孤独")
                        .description("续借《百年孤独》").timestamp("2026-05-06T09:50:00").status("success").build(),
                RecentActivityVO.builder().id(5L).type("reserve").typeName("预约").icon("Bell")
                        .readerName("陈静").bookTitle("人类简史")
                        .description("预约《人类简史》").timestamp("2026-05-06T09:20:00").status("success").build(),
                RecentActivityVO.builder().id(6L).type("borrow").typeName("借书").icon("DocumentAdd")
                        .readerName("刘洋").bookTitle("活着")
                        .description("借书《活着》").timestamp("2026-05-05T17:30:00").status("success").build(),
                RecentActivityVO.builder().id(7L).type("return").typeName("还书").icon("DocumentDelete")
                        .readerName("孙丽").bookTitle("平凡的世界")
                        .description("还书《平凡的世界》").timestamp("2026-05-05T16:10:00").status("success").build(),
                RecentActivityVO.builder().id(8L).type("borrow").typeName("借书").icon("DocumentAdd")
                        .readerName("赵峰").bookTitle("Python编程:从入门到实践")
                        .description("借书《Python编程:从入门到实践》").timestamp("2026-05-05T15:40:00").status("success").build(),
                RecentActivityVO.builder().id(9L).type("renew").typeName("续借").icon("Refresh")
                        .readerName("周梅").bookTitle("未来简史")
                        .description("续借《未来简史》").timestamp("2026-05-05T14:55:00").status("success").build(),
                RecentActivityVO.builder().id(10L).type("borrow").typeName("借书").icon("DocumentAdd")
                        .readerName("吴浩").bookTitle("深入理解计算机系统")
                        .description("借书《深入理解计算机系统》").timestamp("2026-05-05T14:00:00").status("success").build(),
                RecentActivityVO.builder().id(11L).type("reserve").typeName("预约").icon("Bell")
                        .readerName("郑丽娜").bookTitle("解忧杂货店")
                        .description("预约《解忧杂货店》").timestamp("2026-05-05T11:30:00").status("success").build(),
                RecentActivityVO.builder().id(12L).type("return").typeName("还书").icon("DocumentDelete")
                        .readerName("林志远").bookTitle("算法导论")
                        .description("还书《算法导论》").timestamp("2026-05-05T10:20:00").status("success").build(),
                RecentActivityVO.builder().id(13L).type("borrow").typeName("借书").icon("DocumentAdd")
                        .readerName("何秀云").bookTitle("围城")
                        .description("借书《围城》").timestamp("2026-05-04T16:45:00").status("success").build(),
                RecentActivityVO.builder().id(14L).type("register").typeName("新读者").icon("UserFilled")
                        .readerName("蔡文博").bookTitle("")
                        .description("新读者注册：蔡文博").timestamp("2026-05-04T14:30:00").status("success").build(),
                RecentActivityVO.builder().id(15L).type("return").typeName("还书").icon("DocumentDelete")
                        .readerName("黄晓燕").bookTitle("追风筝的人")
                        .description("还书《追风筝的人》").timestamp("2026-05-04T09:00:00").status("success").build()
        );

        return allActivities.subList(0, Math.min(effective, allActivities.size()));
    }

    // ==================== 辅助方法 ====================

    private int getDaysFromTimeRange(String timeRange) {
        return switch (timeRange) {
            case "LAST_7_DAYS" -> 7;
            case "LAST_30_DAYS" -> 30;
            case "THIS_MONTH" -> LocalDate.now().getDayOfMonth();
            case "THIS_YEAR" -> LocalDate.now().getDayOfYear();
            default -> 30;
        };
    }

    private long randomLong(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    private int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private double randomDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }
}
