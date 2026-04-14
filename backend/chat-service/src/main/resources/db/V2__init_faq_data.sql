-- Initial FAQ Categories
INSERT INTO faq_category (name, code, description, icon, sort_order, status) VALUES
('借阅规则', 'BORROW_RULE', '图书借阅相关规则和流程', 'BookOpen', 1, 1),
('开馆时间', 'OPENING_HOURS', '图书馆开放时间相关', 'Clock', 2, 1),
('罚款规则', 'FINE_RULE', '逾期罚款和赔偿相关', 'CreditCard', 3, 1),
('预约说明', 'RESERVATION', '图书预约相关说明', 'Calendar', 4, 1),
('读者证办理', 'READER_CARD', '读者证申请和管理', 'IdCard', 5, 1),
('续借说明', 'RENEWAL', '图书续借相关规则', 'Refresh', 6, 1),
('馆藏查询', 'CATALOG', '馆藏资源查询相关', 'Search', 7, 1),
('其他服务', 'OTHER_SERVICE', '其他图书馆服务', 'HelpCircle', 8, 1);

-- Initial Intent Definitions
INSERT INTO chat_intent (name, code, description, patterns, entities, response_template, action_type, status) VALUES
('借阅咨询', 'BORROW_INQUIRY', '借阅规则和流程咨询',
 ARRAY['借书', '借阅', '怎么借', '如何借', '借几本', '可以借多少', '借书流程', '借阅流程'],
 ARRAY['book', 'reader_type'],
 NULL, 'FAQ_LOOKUP', 1),

('还书咨询', 'RETURN_INQUIRY', '还书规则和流程咨询',
 ARRAY['还书', '归还', '怎么还', '如何还', '还书流程', '归还流程', '在哪还'],
 ARRAY['book'],
 NULL, 'FAQ_LOOKUP', 1),

('续借咨询', 'RENEWAL_INQUIRY', '图书续借相关咨询',
 ARRAY['续借', '续期', '延期', '怎么续借', '如何续借', '能续借吗', '续借次数', '续借期限'],
 ARRAY['book'],
 NULL, 'FAQ_LOOKUP', 1),

('逾期咨询', 'OVERDUE_INQUIRY', '逾期和罚款相关咨询',
 ARRAY['逾期', '过期', '超期', '罚款', '罚金', '滞纳金', '逾期了', '过期了'],
 ARRAY['fine_amount', 'days'],
 NULL, 'FAQ_LOOKUP', 1),

('开馆时间', 'OPENING_HOURS', '开放时间查询',
 ARRAY['开馆时间', '开放时间', '几点开门', '几点关门', '营业时间', '什么时候开', '什么时候关', '周末开吗', '节假日'],
 ARRAY['time', 'date'],
 NULL, 'FAQ_LOOKUP', 1),

('读者证办理', 'CARD_APPLICATION', '读者证申请办理',
 ARRAY['读者证', '办证', '借书证', '办理读者证', '怎么办证', '如何办证', '申请读者证', '新办', '补办'],
 ARRAY['document_type'],
 NULL, 'FAQ_LOOKUP', 1),

('预约咨询', 'RESERVATION_INQUIRY', '图书预约相关咨询',
 ARRAY['预约', '预定', '怎么预约', '如何预约', '能预约吗', '预约流程', '取消预约'],
 ARRAY['book'],
 NULL, 'FAQ_LOOKUP', 1),

('馆藏查询', 'CATALOG_SEARCH', '查询馆藏图书',
 ARRAY['有没有', '查书', '找书', '有这本书吗', '馆藏', '在哪个架', '书架位置', '在不在'],
 ARRAY['book_title', 'author'],
 NULL, 'FAQ_LOOKUP', 1),

('密码找回', 'PASSWORD_RESET', '账户密码相关',
 ARRAY['密码', '忘记密码', '重置密码', '修改密码', '找回密码', '登录不了'],
 ARRAY[],
 NULL, 'FAQ_LOOKUP', 1),

('问候', 'GREETING', '打招呼问候',
 ARRAY['你好', '您好', '嗨', 'hi', 'hello', '在吗', '在不在'],
 ARRAY[],
 '您好！我是图书馆AI助手，很高兴为您服务。请问有什么可以帮您的？', 'NONE', 1),

('感谢', 'THANKS', '表示感谢',
 ARRAY['谢谢', '感谢', '多谢', 'thanks', 'thank you', '太棒了', '好的'],
 ARRAY[],
 '不客气！如果还有其他问题，随时可以问我。祝您阅读愉快！', 'NONE', 1),

('告别', 'GOODBYE', '结束对话',
 ARRAY['再见', '拜拜', 'bye', 'goodbye', '没有了', '没问题了', '就这些'],
 ARRAY[],
 '再见！期待下次为您服务，祝您阅读愉快！', 'NONE', 1);

-- Initial FAQ Knowledge Base
INSERT INTO faq_knowledge (category_id, question, answer, keywords, intent_tags, priority, status) VALUES

-- 借阅规则
(1, '如何借阅图书？',
 '<p>借阅图书的流程如下：</p>
<ol>
<li>携带读者证或使用人脸识别</li>
<li>在书架找到想借的图书</li>
<li>到自助借还机或服务台办理借阅</li>
<li>确认借阅信息后完成借书</li>
</ol>
<p><strong>温馨提示</strong>：您也可以在小程序或网站上预约图书，到馆后直接取书。</p>',
 ARRAY['借书', '借阅', '流程', '怎么借'],
 ARRAY['BORROW_INQUIRY'],
 10, 1),

(1, '每次可以借多少本书？',
 '<p>不同读者类型的借阅数量如下：</p>
<ul>
<li><strong>学生读者</strong>：每次可借 <strong>5本</strong></li>
<li><strong>教师读者</strong>：每次可借 <strong>10本</strong></li>
<li><strong>普通读者</strong>：每次可借 <strong>3本</strong></li>
<li><strong>VIP读者</strong>：每次可借 <strong>15本</strong></li>
</ul>
<p>如需增加借阅额度，请联系图书馆工作人员办理升级。</p>',
 ARRAY['借几本', '借多少', '数量', '限制', '额度'],
 ARRAY['BORROW_INQUIRY'],
 9, 1),

(1, '图书借阅期限是多久？',
 '<p>借阅期限根据读者类型有所不同：</p>
<ul>
<li><strong>学生读者</strong>：借期 <strong>30天</strong></li>
<li><strong>教师读者</strong>：借期 <strong>60天</strong></li>
<li><strong>普通读者</strong>：借期 <strong>21天</strong></li>
<li><strong>VIP读者</strong>：借期 <strong>90天</strong></li>
</ul>
<p>到期前可进行<strong>1次续借</strong>，续借期限与原借期相同。</p>',
 ARRAY['借期', '期限', '多久', '多长时间', '天数'],
 ARRAY['BORROW_INQUIRY'],
 9, 1),

-- 还书相关
(1, '如何归还图书？',
 '<p>归还图书有以下方式：</p>
<ol>
<li><strong>自助还书机</strong>：将图书放入还书口，确认还书成功</li>
<li><strong>服务台还书</strong>：交给工作人员办理</li>
<li><strong>24小时还书箱</strong>：位于图书馆入口处（闭馆时可用）</li>
</ol>
<p><strong>注意</strong>：请确保图书完好无损，否则可能需要赔偿。</p>',
 ARRAY['还书', '归还', '怎么还', '还书流程'],
 ARRAY['RETURN_INQUIRY'],
 10, 1),

-- 续借规则
(2, '如何续借图书？',
 '<p>续借图书有以下几种方式：</p>
<ol>
<li><strong>微信小程序</strong>：打开"我的借阅"，点击"续借"按钮</li>
<li><strong>图书馆网站</strong>：登录个人中心，在借阅记录中操作</li>
<li><strong>到馆续借</strong>：携带图书和读者证到服务台办理</li>
<li><strong>电话续借</strong>：拨打服务热线 0571-12345678</li>
</ol>
<p><strong>注意事项</strong>：</p>
<ul>
<li>每本书只能续借<strong>1次</strong></li>
<li>如有其他读者预约，则不能续借</li>
<li>已逾期的图书不能续借，需先归还</li>
</ul>',
 ARRAY['续借', '续期', '延期', '怎么续借'],
 ARRAY['RENEWAL_INQUIRY'],
 10, 1),

-- 开馆时间
(2, '图书馆开放时间是什么时候？',
 '<p>图书馆开放时间：</p>
<table border="1" style="border-collapse: collapse; width: 100%;">
<tr><th>时间段</th><th>开放时间</th></tr>
<tr><td>周一至周五</td><td>08:00 - 22:00</td></tr>
<tr><td>周六、周日</td><td>09:00 - 21:00</td></tr>
<tr><td>法定节假日</td><td>10:00 - 18:00</td></tr>
</table>
<p><strong>特别说明</strong>：</p>
<ul>
<li>自习区24小时开放（需刷卡进入）</li>
<li>考试周期间延长开放时间</li>
<li>如遇特殊情况会另行通知</li>
</ul>',
 ARRAY['开馆', '开放', '时间', '几点', '开门', '关门', '营业'],
 ARRAY['OPENING_HOURS'],
 10, 1),

-- 罚款规则
(3, '逾期了怎么办？罚款标准是多少？',
 '<p>逾期罚款规则如下：</p>
<ul>
<li>逾期罚款：<strong>0.1元/天/本</strong></li>
<li>罚款上限：不超过图书原价</li>
<li>缴费方式：微信/支付宝/现金均可</li>
</ul>
<p><strong>处理流程</strong>：</p>
<ol>
<li>先归还逾期图书</li>
<li>系统自动计算罚款金额</li>
<li>在自助机或服务台缴纳罚款</li>
<li>缴清后即可正常借阅</li>
</ol>
<p><strong>温馨提示</strong>：建议开启到期提醒，避免逾期。</p>',
 ARRAY['逾期', '过期', '罚款', '罚金', '滞纳金', '超期'],
 ARRAY['OVERDUE_INQUIRY'],
 10, 1),

(3, '图书丢失或损坏怎么赔偿？',
 '<p>图书丢失或损坏的赔偿规则：</p>
<h4>丢失赔偿</h4>
<ul>
<li>按图书原价的<strong>2-5倍</strong>赔偿</li>
<li>如能购买同版本图书归还，只需支付加工费<strong>10元</strong></li>
</ul>
<h4>损坏赔偿</h4>
<ul>
<li>轻微损坏（不影响阅读）：<strong>5-20元</strong></li>
<li>严重损坏（影响阅读）：按丢失处理</li>
<li>条码/磁条损坏：<strong>5元</strong></li>
</ul>
<p>请到服务台办理赔偿手续。</p>',
 ARRAY['丢失', '损坏', '赔偿', '丢了', '弄坏'],
 ARRAY['OVERDUE_INQUIRY'],
 8, 1),

-- 预约说明
(4, '如何预约图书？',
 '<p>预约图书的流程：</p>
<ol>
<li>登录图书馆网站或小程序</li>
<li>搜索想要预约的图书</li>
<li>点击"预约"按钮</li>
<li>选择取书馆区</li>
<li>确认预约信息</li>
</ol>
<p><strong>预约规则</strong>：</p>
<ul>
<li>每位读者最多预约<strong>3本</strong>图书</li>
<li>预约有效期为<strong>3天</strong>，请及时取书</li>
<li>到书后会发送短信/微信通知</li>
<li>超期未取将自动取消预约</li>
</ul>',
 ARRAY['预约', '预定', '怎么预约', '预约流程'],
 ARRAY['RESERVATION_INQUIRY'],
 10, 1),

(4, '如何取消预约？',
 '<p>取消预约的方式：</p>
<ol>
<li><strong>网站/小程序</strong>：进入"我的预约"，点击"取消预约"</li>
<li><strong>到馆取消</strong>：到服务台告知工作人员</li>
<li><strong>电话取消</strong>：拨打服务热线</li>
</ol>
<p><strong>注意</strong>：频繁取消预约可能影响预约权限。</p>',
 ARRAY['取消预约', '不要了', '取消'],
 ARRAY['RESERVATION_INQUIRY'],
 7, 1),

-- 读者证办理
(5, '如何办理读者证？',
 '<p>办理读者证的流程：</p>
<ol>
<li>准备材料：身份证或学生证</li>
<li>前往图书馆服务台</li>
<li>填写《读者证申请表》</li>
<li>工作人员录入信息并拍照</li>
<li>缴纳押金（学生20元，教师50元，普通读者100元）</li>
<li>领取读者证，即可开始借阅</li>
</ol>
<p><strong>温馨提示</strong>：</p>
<ul>
<li>支持人脸识别借书，办证后可直接刷脸借还</li>
<li>押金退证时全额返还</li>
</ul>',
 ARRAY['办证', '读者证', '借书证', '办理', '申请'],
 ARRAY['CARD_APPLICATION'],
 10, 1),

(5, '读者证丢失如何补办？',
 '<p>补办读者证流程：</p>
<ol>
<li>挂失：拨打服务热线或到服务台挂失</li>
<li>准备材料：身份证/学生证</li>
<li>到服务台办理补办</li>
<li>缴纳补办费用<strong>10元</strong></li>
<li>领取新读者证</li>
</ol>
<p><strong>注意</strong>：挂失后原证即刻失效，如找到不能继续使用。</p>',
 ARRAY['补办', '丢失', '挂失', '读者证丢了'],
 ARRAY['CARD_APPLICATION'],
 8, 1),

-- 馆藏查询
(7, '如何查询馆藏图书？',
 '<p>查询馆藏图书的方式：</p>
<ol>
<li><strong>图书馆网站</strong>：访问馆藏检索系统</li>
<li><strong>微信小程序</strong>：首页搜索框输入书名/作者</li>
<li><strong>馆内检索机</strong>：位于各楼层入口处</li>
<li><strong>咨询台</strong>：工作人员协助查询</li>
</ol>
<p><strong>搜索技巧</strong>：</p>
<ul>
<li>可按书名、作者、ISBN、主题词检索</li>
<li>支持模糊搜索</li>
<li>可查看图书位置和在架状态</li>
</ul>',
 ARRAY['查书', '找书', '馆藏', '检索', '搜索'],
 ARRAY['CATALOG_SEARCH'],
 9, 1),

-- 密码找回
(8, '忘记密码怎么办？',
 '<p>密码找回方式：</p>
<ol>
<li><strong>自助找回</strong>：
  <ul>
    <li>点击登录页"忘记密码"</li>
    <li>输入读者证号和绑定手机号</li>
    <li>获取验证码后设置新密码</li>
  </ul>
</li>
<li><strong>人工重置</strong>：
  <ul>
    <li>携带身份证到服务台</li>
    <li>核实身份后重置密码</li>
  </ul>
</li>
</ol>
<p><strong>初始密码</strong>：身份证后6位（字母X用0代替）</p>',
 ARRAY['密码', '忘记密码', '重置密码', '找回密码', '登录不了'],
 ARRAY['PASSWORD_RESET'],
 9, 1),

-- 其他服务
(8, '图书馆提供哪些服务？',
 '<p>图书馆提供以下服务：</p>
<h4>基础服务</h4>
<ul>
<li>图书借阅与归还</li>
<li>期刊阅览</li>
<li>自习座位</li>
<li>WIFI网络</li>
</ul>
<h4>增值服务</h4>
<ul>
<li>文献传递与馆际互借</li>
<li>参考咨询</li>
<li>信息检索培训</li>
<li>研讨室预约</li>
<li>打印复印</li>
</ul>
<h4>数字资源</h4>
<ul>
<li>电子图书</li>
<li>学术数据库</li>
<li>多媒体资源</li>
</ul>',
 ARRAY['服务', '提供什么', '有什么服务', '功能'],
 ARRAY['BORROW_INQUIRY'],
 5, 1);
